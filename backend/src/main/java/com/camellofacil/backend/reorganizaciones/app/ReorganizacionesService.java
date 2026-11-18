package com.camellofacil.backend.reorganizaciones.app;

import com.camellofacil.backend.adaptaciones.app.AdaptadorIa.ExperienciaIa;
import com.camellofacil.backend.adaptaciones.app.AdaptadorIa.PropuestaIa;
import com.camellofacil.backend.adaptaciones.app.VerificadorFidelidad;
import com.camellofacil.backend.cv.app.EvaluadorHoja;
import com.camellofacil.backend.cv.app.HojaDeVidaService;
import com.camellofacil.backend.cv.app.ModelosHoja.ValorExperiencia;
import com.camellofacil.backend.cv.app.ModelosHoja.ValoresHoja;
import com.camellofacil.backend.legal.app.ConsentimientoService;
import com.camellofacil.backend.legal.app.TipoConsentimiento;
import com.camellofacil.backend.plantillas.app.ModelosPlantilla.DocumentoHoja;
import com.camellofacil.backend.plantillas.app.PlantillasService;
import com.camellofacil.backend.reorganizaciones.app.ModelosReorganizacion.CambioPlan;
import com.camellofacil.backend.reorganizaciones.app.ModelosReorganizacion.CambioPropuesto;
import com.camellofacil.backend.reorganizaciones.app.ModelosReorganizacion.PropuestaInfo;
import com.camellofacil.backend.reorganizaciones.app.ModelosReorganizacion.RedaccionVerificada;
import com.camellofacil.backend.reorganizaciones.app.PlanReorganizacion.Aplicado;
import com.camellofacil.backend.reorganizaciones.app.ReorganizacionException.Motivo;
import com.camellofacil.backend.reorganizaciones.app.ReorganizadorIa.ReorganizadorIaException;
import com.camellofacil.backend.reorganizaciones.persistence.ReorganizacionHoja;
import com.camellofacil.backend.reorganizaciones.persistence.ReorganizacionHojaRepository;
import com.camellofacil.backend.versiones.app.ModelosVersion.VersionResumen;
import com.camellofacil.backend.versiones.app.VersionesService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reorganizar la hoja base a formato Harvard o ATS (RN-03: sin inventar datos). La propuesta es un plan de cambios que
 * la persona aplica o revierte uno por uno. La redacción mejorada viene de la IA, pero solo se acepta lo que
 * {@link VerificadorFidelidad} respalde con la hoja base; si la IA no está disponible, la propuesta se arma igual con
 * los cambios que no la necesitan (orden y viñetas).
 */
@Service
public class ReorganizacionesService {

    /** Llamadas a la IA por persona y día. Cambiar de formato o idioma con la misma hoja reutiliza la redacción. */
    static final int MAXIMO_IA_POR_DIA = 6;
    /** Propuestas guardadas por persona; al pasar el tope se descartan las más antiguas. */
    static final int MAXIMO_GUARDADAS = 10;

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final String PLANTILLA_ORIGINAL = "ats-clasica";
    private static final TypeReference<List<CambioPlan>> PLAN = new TypeReference<>() {};
    private static final TypeReference<List<String>> AVISOS = new TypeReference<>() {};

    private final ReorganizacionHojaRepository reorganizaciones;
    private final HojaDeVidaService hojas;
    private final ConsentimientoService consentimientos;
    private final PlantillasService plantillas;
    private final VersionesService versiones;
    private final ReorganizadorIa ia;
    private final JsonMapper json;
    private final Clock reloj;

    public ReorganizacionesService(
            ReorganizacionHojaRepository reorganizaciones,
            HojaDeVidaService hojas,
            ConsentimientoService consentimientos,
            PlantillasService plantillas,
            VersionesService versiones,
            ReorganizadorIa ia,
            JsonMapper json,
            Clock reloj) {
        this.reorganizaciones = reorganizaciones;
        this.hojas = hojas;
        this.consentimientos = consentimientos;
        this.plantillas = plantillas;
        this.versiones = versiones;
        this.ia = ia;
        this.json = json;
        this.reloj = reloj;
    }

    @Transactional
    public PropuestaInfo proponer(UUID usuarioId, String correo, String formato, String idioma) {
        if (!List.of("harvard", "ats").contains(formato) || !List.of("es", "en").contains(idioma)) {
            throw new ReorganizacionException(Motivo.DATOS_INVALIDOS, "Elige un formato (Harvard o ATS) y un idioma (es o en)");
        }
        ValoresHoja base = hojas.valores(usuarioId);
        String huella = huella(base);
        // Lo que falta en la hoja y que NO se inventa: se muestra como aviso.
        List<String> avisos = new ArrayList<>(EvaluadorHoja.observaciones(base).stream()
                .filter(o -> "falta".equals(o.tipo()))
                .limit(5)
                .map(o -> o.titulo() + ": " + o.detalle())
                .toList());

        RedaccionVerificada redaccion;
        boolean generada = false;
        var reutilizable = reorganizaciones.findFirstByUsuarioIdAndBaseHuellaAndIaGeneradaTrueOrderByCreadaEnDesc(usuarioId, huella);
        if (reutilizable.isPresent()) {
            redaccion = json.readValue(reutilizable.get().getIa(), RedaccionVerificada.class);
        } else if (consentimientos.vigente(usuarioId, TipoConsentimiento.ANALISIS_IA).isEmpty()) {
            redaccion = RedaccionVerificada.vacia();
            avisos.add("Activa la autorización de análisis con IA en Mi perfil para que también mejoremos la redacción.");
        } else if (reorganizaciones.countByUsuarioIdAndIaGeneradaTrueAndCreadaEnAfter(usuarioId, inicioDelDia()) >= MAXIMO_IA_POR_DIA) {
            redaccion = RedaccionVerificada.vacia();
            avisos.add("Ya usaste la IA para reorganizar " + MAXIMO_IA_POR_DIA + " veces hoy: te mostramos solo los cambios de orden.");
        } else {
            try {
                redaccion = verificar(base, ia.redactar(sinContacto(base), formato));
                generada = true;
            } catch (ReorganizadorIaException e) {
                redaccion = RedaccionVerificada.vacia();
                avisos.add("No pudimos mejorar la redacción con IA ahora: te mostramos solo los cambios de orden.");
            }
        }
        if (!idioma.equals(hojas.datos(usuarioId).archivo().idioma())) {
            avisos.add("Cambiamos los títulos de las secciones; el contenido queda en el idioma en que lo escribiste.");
        }

        List<CambioPlan> plan = PlanReorganizacion.construir(base, formato, redaccion);
        descartarSobrantes(usuarioId);
        ReorganizacionHoja guardada = reorganizaciones.save(new ReorganizacionHoja(
                usuarioId, formato, idioma, huella, json.writeValueAsString(base), json.writeValueAsString(redaccion), generada,
                json.writeValueAsString(plan), json.writeValueAsString(avisos), Instant.now()));
        return info(guardada, correo);
    }

    /** Aplica o revierte un cambio. No llama a la IA: el documento se vuelve a armar con el plan actualizado. */
    @Transactional
    public PropuestaInfo cambiar(UUID usuarioId, String correo, UUID id, String cambioId, boolean aplicado) {
        ReorganizacionHoja r = buscar(usuarioId, id);
        List<CambioPlan> plan = json.readValue(r.getPlan(), PLAN);
        boolean existe = plan.stream().anyMatch(c -> c.id().equals(cambioId));
        if (!existe) throw new ReorganizacionException(Motivo.NO_ENCONTRADA, "No encontramos ese cambio");
        List<CambioPlan> nuevo = plan.stream()
                .map(c -> c.id().equals(cambioId)
                        ? new CambioPlan(c.id(), c.tipo(), c.titulo(), c.detalle(), c.seccionId(), aplicado, c.clave(), c.valor())
                        : c)
                .toList();
        r.nuevoPlan(json.writeValueAsString(nuevo));
        return info(r, correo);
    }

    /** Guarda la propuesta, con los cambios que quedaron aplicados, como versión «reorganizada». Aceptarla dos veces no duplica. */
    @Transactional
    public VersionResumen aceptar(UUID usuarioId, String correo, UUID id) {
        ReorganizacionHoja r = buscar(usuarioId, id);
        if (r.getVersionId() != null) {
            return versiones.resumen(usuarioId, r.getVersionId());
        }
        ValoresHoja base = json.readValue(r.getBase(), ValoresHoja.class);
        Aplicado aplicado = PlanReorganizacion.aplicar(base, json.readValue(r.getPlan(), PLAN));
        String destino = plantillaDestino(r.getFormato());
        DocumentoHoja documento = plantillas.armar(plantillaDeSecciones(aplicado, destino), aplicado.valores(), correo, r.getIdioma());
        String formato = "harvard".equals(r.getFormato()) ? "Harvard" : "ATS";
        VersionResumen version = versiones.crearReorganizada(
                usuarioId, "Reorganizada · " + formato + " (" + r.getIdioma() + ")", "Reorganizada al formato " + formato,
                destino, r.getIdioma(), aplicado.valores(), documento);
        r.guardadaComoVersion(UUID.fromString(version.id()));
        return version;
    }

    @Transactional
    public void descartar(UUID usuarioId, UUID id) {
        reorganizaciones.delete(buscar(usuarioId, id));
    }

    // ---------- Armado de la propuesta ----------

    private PropuestaInfo info(ReorganizacionHoja r, String correo) {
        ValoresHoja base = json.readValue(r.getBase(), ValoresHoja.class);
        List<CambioPlan> plan = json.readValue(r.getPlan(), PLAN);
        Aplicado aplicado = PlanReorganizacion.aplicar(base, plan);
        String destino = plantillaDestino(r.getFormato());

        DocumentoHoja original = plantillas.armar(PLANTILLA_ORIGINAL, base, correo, r.getIdioma());
        DocumentoHoja propuesta = plantillas.armar(plantillaDeSecciones(aplicado, destino), aplicado.valores(), correo, r.getIdioma());
        List<CambioPropuesto> cambios = plan.stream()
                .map(c -> new CambioPropuesto(c.id(), c.tipo(), c.titulo(), c.detalle(), c.aplicado(), c.seccionId()))
                .toList();
        return new PropuestaInfo(
                r.getId().toString(), r.getFormato(), r.getIdioma(), original, propuesta, cambios, json.readValue(r.getAvisos(), AVISOS));
    }

    /** Sin el cambio «secciones en el orden Harvard» las secciones se quedan como estaban (orden de la hoja base). */
    private static String plantillaDeSecciones(Aplicado aplicado, String destino) {
        return aplicado.ordenSecciones() ? destino : PLANTILLA_ORIGINAL;
    }

    private static String plantillaDestino(String formato) {
        return "harvard".equals(formato) ? "harvard-clasica" : "ats-clasica";
    }

    /**
     * RN-03: de la IA solo se acepta el perfil y las descripciones que {@link VerificadorFidelidad} respalde con la
     * hoja base. El orden y todo lo demás que ella diga se ignora.
     */
    private static RedaccionVerificada verificar(ValoresHoja base, PropuestaIa propuesta) {
        Map<String, String> propuestas = new LinkedHashMap<>();
        for (ExperienciaIa e : propuesta.experiencia() == null ? List.<ExperienciaIa>of() : propuesta.experiencia()) {
            if (e != null && e.id() != null) propuestas.put(e.id(), e.descripcion());
        }
        List<ExperienciaIa> enOrdenDeLaBase = new ArrayList<>();
        for (ValorExperiencia e : base.experiencia() == null ? List.<ValorExperiencia>of() : base.experiencia()) {
            enOrdenDeLaBase.add(new ExperienciaIa(e.id(), propuestas.get(e.id())));
        }
        var resultado = VerificadorFidelidad.verificar(base, new PropuestaIa(propuesta.perfil(), List.of(), enOrdenDeLaBase, List.of()));
        Map<String, String> descripciones = new LinkedHashMap<>();
        resultado.valores().experiencia().forEach(e -> descripciones.put(e.id(), e.descripcion()));
        return new RedaccionVerificada(resultado.valores().perfil(), descripciones);
    }

    /** Minimiza datos: la IA no recibe nombre, teléfono ni ciudad. */
    private static ValoresHoja sinContacto(ValoresHoja v) {
        return new ValoresHoja("", "", "", v.perfil(), v.habilidades(), v.experiencia(), v.educacion());
    }

    private void descartarSobrantes(UUID usuarioId) {
        List<ReorganizacionHoja> todas = reorganizaciones.findByUsuarioIdOrderByCreadaEnAsc(usuarioId);
        int sobran = todas.size() - (MAXIMO_GUARDADAS - 1);
        if (sobran > 0) reorganizaciones.deleteAll(todas.subList(0, sobran));
    }

    private ReorganizacionHoja buscar(UUID usuarioId, UUID id) {
        return reorganizaciones.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new ReorganizacionException(Motivo.NO_ENCONTRADA, "No encontramos esa propuesta"));
    }

    private Instant inicioDelDia() {
        return LocalDate.now(reloj.withZone(ZONA)).atStartOfDay(ZONA).toInstant();
    }

    private String huella(ValoresHoja base) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(json.writeValueAsString(base).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
