package com.camellofacil.backend.adaptaciones.app;

import com.camellofacil.backend.adaptaciones.app.AdaptacionException.Motivo;
import com.camellofacil.backend.adaptaciones.app.AdaptadorIa.AdaptadorIaException;
import com.camellofacil.backend.adaptaciones.app.AdaptadorIa.PropuestaIa;
import com.camellofacil.backend.adaptaciones.app.AdaptadorIa.VacanteIa;
import com.camellofacil.backend.adaptaciones.app.ModelosAdaptacion.AdaptacionInfo;
import com.camellofacil.backend.adaptaciones.app.ModelosAdaptacion.CambioAdaptacion;
import com.camellofacil.backend.adaptaciones.app.ModelosAdaptacion.ContenidoDescarga;
import com.camellofacil.backend.adaptaciones.app.ModelosAdaptacion.CupoAdaptaciones;
import com.camellofacil.backend.adaptaciones.app.ModelosAdaptacion.EstadoAdaptacion;
import com.camellofacil.backend.adaptaciones.persistence.AdaptacionHoja;
import com.camellofacil.backend.adaptaciones.persistence.AdaptacionHojaRepository;
import com.camellofacil.backend.adaptaciones.persistence.UsoAdaptacionRepository;
import com.camellofacil.backend.cv.app.HojaDeVidaService;
import com.camellofacil.backend.cv.app.ModelosHoja.ValoresHoja;
import com.camellofacil.backend.legal.app.ConsentimientoService;
import com.camellofacil.backend.legal.app.TipoConsentimiento;
import com.camellofacil.backend.plantillas.app.ModelosPlantilla.DocumentoHoja;
import com.camellofacil.backend.plantillas.app.PlantillasService;
import com.camellofacil.backend.vacantes.app.ModelosVacante.VacanteDetalle;
import com.camellofacil.backend.vacantes.app.VacantesService;
import com.camellofacil.backend.versiones.app.ModelosVersion.VersionResumen;
import com.camellofacil.backend.versiones.app.OrigenAdaptacion;
import com.camellofacil.backend.versiones.app.VersionesService;
import com.camellofacil.backend.versiones.app.VersionesService.ContenidoVersion;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Adaptar la hoja de vida a una vacante (RN-03: sin inventar datos). El cupo diario se gasta antes de llamar a
 * la IA y se deshace si ella falla, porque todo ocurre en una sola transacción.
 */
@Service
public class AdaptacionesService implements OrigenAdaptacion {

    /** La adaptación usa una plantilla ATS: es la que mejor leen los sistemas de selección. */
    static final String PLANTILLA = "ats-clasica";
    /** El «día» del cupo es el de Colombia, no el del servidor. */
    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final int MAXIMO_DESCRIPCION = 8000;

    private final AdaptacionHojaRepository adaptaciones;
    private final UsoAdaptacionRepository usos;
    private final VacantesService vacantes;
    private final HojaDeVidaService hojas;
    private final ConsentimientoService consentimientos;
    private final PlantillasService plantillas;
    private final VersionesService versiones;
    private final AdaptadorIa ia;
    private final JsonMapper json;
    private final Clock reloj;
    private final int limiteDiario;

    public AdaptacionesService(
            AdaptacionHojaRepository adaptaciones,
            UsoAdaptacionRepository usos,
            VacantesService vacantes,
            HojaDeVidaService hojas,
            ConsentimientoService consentimientos,
            PlantillasService plantillas,
            VersionesService versiones,
            AdaptadorIa ia,
            JsonMapper json,
            Clock reloj,
            @Value("${camellofacil.adaptaciones.limite-diario:3}") int limiteDiario) {
        this.adaptaciones = adaptaciones;
        this.usos = usos;
        this.vacantes = vacantes;
        this.hojas = hojas;
        this.consentimientos = consentimientos;
        this.plantillas = plantillas;
        this.versiones = versiones;
        this.ia = ia;
        this.json = json;
        this.reloj = reloj;
        this.limiteDiario = limiteDiario;
    }

    @Transactional(readOnly = true)
    public EstadoAdaptacion estado(UUID usuarioId, UUID vacanteId) {
        int restantes = restantes(usuarioId);
        return new EstadoAdaptacion(
                adaptaciones.findByUsuarioIdAndVacanteId(usuarioId, vacanteId).map(a -> info(a, restantes)).orElse(null),
                restantes);
    }

    @Transactional(readOnly = true)
    public CupoAdaptaciones cupo(UUID usuarioId) {
        return new CupoAdaptaciones(limiteDiario - restantes(usuarioId), limiteDiario);
    }

    /** Crea la adaptación (o regenera la que ya había para esa vacante). Gasta una del cupo de hoy. */
    @Transactional
    public AdaptacionInfo crear(UUID usuarioId, String correo, UUID vacanteId) {
        VacanteDetalle vacante = vacantes.detalle(vacanteId)
                .orElseThrow(() -> new AdaptacionException(Motivo.NO_ENCONTRADA, "No encontramos esa vacante"));
        ValoresHoja base = hojas.valores(usuarioId);
        // Los datos de la hoja viajan al proveedor de IA: hace falta la autorización vigente (Ley 1581).
        if (consentimientos.vigente(usuarioId, TipoConsentimiento.ANALISIS_IA).isEmpty()) {
            throw new AdaptacionException(Motivo.SIN_AUTORIZACION_IA, "Necesitamos tu autorización de análisis con IA para adaptar tu hoja");
        }
        if (usos.consumir(usuarioId, hoy(), limiteDiario) == 0) {
            throw new AdaptacionException(Motivo.LIMITE, "Ya usaste tus " + limiteDiario + " adaptaciones de hoy");
        }

        PropuestaIa propuesta;
        try {
            propuesta = ia.adaptar(sinContacto(base), vacanteIa(vacante));
        } catch (AdaptadorIaException e) {
            // La excepción deshace la transacción, y con ella el gasto del cupo.
            throw new AdaptacionException(Motivo.IA_NO_DISPONIBLE, "No pudimos adaptar tu hoja ahora. No gastaste ninguna adaptación: intenta de nuevo en un momento");
        }

        var resultado = VerificadorFidelidad.verificar(base, propuesta);
        String idioma = hojas.datos(usuarioId).archivo().idioma();
        DocumentoHoja original = plantillas.armar(PLANTILLA, base, correo, idioma);
        DocumentoHoja adaptada = plantillas.armar(PLANTILLA, resultado.valores(), correo, idioma);

        adaptaciones.findByUsuarioIdAndVacanteId(usuarioId, vacanteId).ifPresent(anterior -> {
            adaptaciones.delete(anterior);
            adaptaciones.flush();
        });
        AdaptacionHoja guardada = adaptaciones.save(new AdaptacionHoja(
                usuarioId, vacanteId, PLANTILLA, idioma, json.writeValueAsString(resultado.valores()),
                json.writeValueAsString(original), json.writeValueAsString(adaptada),
                json.writeValueAsString(resultado.cambios()), json.writeValueAsString(resultado.avisos()), Instant.now()));
        return info(guardada, restantes(usuarioId));
    }

    @Transactional(readOnly = true)
    public ContenidoDescarga paraDescargar(UUID usuarioId, UUID id) {
        AdaptacionHoja a = buscar(usuarioId, id);
        return new ContenidoDescarga(a.getPlantillaId(), json.readValue(a.getAdaptada(), DocumentoHoja.class));
    }

    @Transactional
    public void descartar(UUID usuarioId, UUID id) {
        adaptaciones.delete(buscar(usuarioId, id));
    }

    /** Guarda la adaptación como versión de la hoja (tipo «adaptada»). Aceptarla dos veces no duplica la versión. */
    @Transactional
    public VersionResumen aceptar(UUID usuarioId, UUID id) {
        AdaptacionHoja a = buscar(usuarioId, id);
        if (a.getVersionId() != null) {
            return versiones.resumen(usuarioId, a.getVersionId());
        }
        VacanteDetalle vacante = vacantes.detalle(a.getVacanteId())
                .orElseThrow(() -> new AdaptacionException(Motivo.NO_ENCONTRADA, "La vacante ya no existe"));
        String empresa = vacante.resumen().empresa();
        String nombre = recortar("Adaptada · " + vacante.resumen().cargo() + " (" + empresa + ")", 120);
        VersionResumen version = versiones.crearAdaptada(
                usuarioId, nombre, "Adaptada a la vacante de " + recortar(empresa, 150), a.getPlantillaId(), a.getIdioma(),
                json.readValue(a.getValores(), ValoresHoja.class), json.readValue(a.getAdaptada(), DocumentoHoja.class));
        a.guardadaComoVersion(UUID.fromString(version.id()));
        return version;
    }

    /** Para abrir el editor sobre la hoja adaptada (la pide el módulo versiones). */
    @Override
    @Transactional(readOnly = true)
    public Optional<ContenidoVersion> contenido(UUID usuarioId, UUID adaptacionId) {
        return adaptaciones.findByIdAndUsuarioId(adaptacionId, usuarioId).map(a -> new ContenidoVersion(
                a.getPlantillaId(), a.getIdioma(), json.readValue(a.getValores(), ValoresHoja.class)));
    }

    private AdaptacionInfo info(AdaptacionHoja a, int restantes) {
        return new AdaptacionInfo(
                a.getId().toString(), a.getVacanteId().toString(),
                json.readValue(a.getOriginal(), DocumentoHoja.class), json.readValue(a.getAdaptada(), DocumentoHoja.class),
                json.readValue(a.getCambios(), new TypeReference<List<CambioAdaptacion>>() {}),
                json.readValue(a.getAvisos(), new TypeReference<List<String>>() {}), restantes);
    }

    private AdaptacionHoja buscar(UUID usuarioId, UUID id) {
        return adaptaciones.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new AdaptacionException(Motivo.NO_ENCONTRADA, "No encontramos esa adaptación"));
    }

    private int restantes(UUID usuarioId) {
        return Math.max(0, limiteDiario - usos.usadas(usuarioId, hoy()).orElse(0));
    }

    private LocalDate hoy() {
        return LocalDate.now(reloj.withZone(ZONA));
    }

    /** Minimiza datos: el proveedor de IA no recibe nombre, teléfono ni ciudad, que la adaptación no usa. */
    private static ValoresHoja sinContacto(ValoresHoja v) {
        return new ValoresHoja("", "", "", v.perfil(), v.habilidades(), v.experiencia(), v.educacion());
    }

    private static VacanteIa vacanteIa(VacanteDetalle v) {
        return new VacanteIa(
                v.resumen().cargo(), v.resumen().empresa(), recortar(v.descripcion() == null ? "" : v.descripcion(), MAXIMO_DESCRIPCION),
                v.responsabilidades(), v.requisitos());
    }

    private static String recortar(String s, int maximo) {
        return s.length() > maximo ? s.substring(0, maximo) : s;
    }
}
