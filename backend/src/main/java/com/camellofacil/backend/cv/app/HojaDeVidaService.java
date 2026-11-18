package com.camellofacil.backend.cv.app;

import com.camellofacil.backend.cv.app.HojaException.Motivo;
import com.camellofacil.backend.cv.app.ModelosHoja.ArchivoInfo;
import com.camellofacil.backend.cv.app.ModelosHoja.Campo;
import com.camellofacil.backend.cv.app.ModelosHoja.Contacto;
import com.camellofacil.backend.cv.app.ModelosHoja.DatosHoja;
import com.camellofacil.backend.cv.app.ModelosHoja.Educacion;
import com.camellofacil.backend.cv.app.ModelosHoja.EstadoHoja;
import com.camellofacil.backend.cv.app.ModelosHoja.Experiencia;
import com.camellofacil.backend.cv.app.ModelosHoja.HojaConDatos;
import com.camellofacil.backend.cv.app.ModelosHoja.ResumenHoja;
import com.camellofacil.backend.cv.app.ModelosHoja.TipoArchivo;
import com.camellofacil.backend.cv.app.ModelosHoja.ValorEducacion;
import com.camellofacil.backend.cv.app.ModelosHoja.ValorExperiencia;
import com.camellofacil.backend.cv.app.ModelosHoja.ValoresHoja;
import com.camellofacil.backend.cv.persistence.ArchivoHoja;
import com.camellofacil.backend.cv.persistence.ArchivoHojaRepository;
import com.camellofacil.backend.cv.persistence.HojaDeVida;
import com.camellofacil.backend.cv.persistence.HojaDeVidaRepository;
import com.camellofacil.backend.legal.app.ConsentimientoService;
import com.camellofacil.backend.legal.app.TipoConsentimiento;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** Hoja de vida base: subirla (o reemplazarla), consultar su estado, revisar y confirmar lo extraído. */
@Service
public class HojaDeVidaService {

    private static final Pattern FECHA = Pattern.compile("^(\\d{4}-(0[1-9]|1[0-2]))?$");

    private final HojaDeVidaRepository hojas;
    private final ArchivoHojaRepository archivos;
    private final LectorDocumentos lector;
    private final ConsentimientoService consentimientos;
    private final JsonMapper json;

    public HojaDeVidaService(
            HojaDeVidaRepository hojas,
            ArchivoHojaRepository archivos,
            LectorDocumentos lector,
            ConsentimientoService consentimientos,
            JsonMapper json) {
        this.hojas = hojas;
        this.archivos = archivos;
        this.lector = lector;
        this.consentimientos = consentimientos;
        this.json = json;
    }

    /**
     * Guarda el archivo y lo deja en la cola de análisis. El análisis con IA necesita la autorización vigente
     * (Ley 1581); sin ella no se recibe el archivo.
     */
    @Transactional
    public void subir(UUID usuarioId, String nombreOriginal, byte[] contenido) {
        if (consentimientos.vigente(usuarioId, TipoConsentimiento.ANALISIS_IA).isEmpty()) {
            throw new HojaException(Motivo.SIN_AUTORIZACION_IA, "Sin autorización de análisis con IA");
        }
        if (contenido.length == 0) {
            throw new HojaException(Motivo.VACIO, "El archivo está vacío");
        }
        TipoArchivo tipo = lector.detectarTipo(contenido)
                .orElseThrow(() -> new HojaException(Motivo.TIPO_NO_PERMITIDO, "Solo PDF o Word"));

        Instant ahora = Instant.now();
        HojaDeVida hoja = hojas.findById(usuarioId).orElseGet(() -> new HojaDeVida(usuarioId));
        hoja.nuevoArchivo(nombreSeguro(nombreOriginal, tipo), tipo.name(), contenido.length, EstadoHoja.ANALIZANDO.name(), ahora);
        hojas.save(hoja);
        archivos.findById(usuarioId).ifPresentOrElse(
                a -> a.reemplazar(contenido),
                () -> archivos.save(new ArchivoHoja(usuarioId, contenido)));
    }

    @Transactional(readOnly = true)
    public Optional<ResumenHoja> resumen(UUID usuarioId) {
        return hojas.findById(usuarioId).map(h -> {
            EstadoHoja estado = EstadoHoja.valueOf(h.getEstado());
            Integer pendientes = estado == EstadoHoja.SIN_CONFIRMAR ? ArmadorDatos.pendientes(leer(h)) : null;
            return new ResumenHoja(estado, info(h), pendientes);
        });
    }

    @Transactional(readOnly = true)
    public Optional<EstadoHoja> estado(UUID usuarioId) {
        return hojas.findById(usuarioId).map(h -> EstadoHoja.valueOf(h.getEstado()));
    }

    /** Lo extraído, para revisarlo. Solo existe cuando el análisis terminó. */
    @Transactional(readOnly = true)
    public HojaConDatos datos(UUID usuarioId) {
        HojaDeVida h = conDatos(usuarioId);
        return new HojaConDatos(info(h), leer(h));
    }

    /** Los datos de la hoja confirmada como valores de texto, listos para armar documentos (plantillas, versiones). */
    @Transactional(readOnly = true)
    public ValoresHoja valores(UUID usuarioId) {
        DatosHoja d = leer(conDatos(usuarioId));
        return new ValoresHoja(
                d.contacto().nombre().valor(), d.contacto().telefono().valor(), d.contacto().ciudad().valor(),
                d.perfil().valor(), d.habilidades().valor(),
                lista(d.experiencia()).stream()
                        .map(x -> new ValorExperiencia(
                                x.id(), x.cargo().valor(), x.organizacion().valor(), x.inicio().valor(), x.fin().valor(), x.descripcion().valor()))
                        .toList(),
                lista(d.educacion()).stream()
                        .map(x -> new ValorEducacion(x.id(), x.titulo().valor(), x.institucion().valor(), x.inicio().valor(), x.fin().valor()))
                        .toList());
    }

    /** Guarda lo que la persona corrigió. Desde aquí, todo queda como «leído»: lo confirmó ella. */
    @Transactional
    public void corregir(UUID usuarioId, ValoresHoja v) {
        HojaDeVida h = conDatos(usuarioId);
        validarFechas(v);
        DatosHoja datos = new DatosHoja(
                new Contacto(Campo.leido(v.nombre()), Campo.leido(v.telefono()), Campo.leido(v.ciudad())),
                Campo.leido(v.perfil()),
                lista(v.experiencia()).stream().map(HojaDeVidaService::experiencia).toList(),
                lista(v.educacion()).stream().map(HojaDeVidaService::educacion).toList(),
                Campo.leido(v.habilidades()));
        h.corregirDatos(json.writeValueAsString(datos), Instant.now());
    }

    @Transactional
    public void confirmar(UUID usuarioId) {
        HojaDeVida h = conDatos(usuarioId);
        h.cambiarEstado(EstadoHoja.CONFIRMADA.name(), Instant.now());
    }

    private HojaDeVida conDatos(UUID usuarioId) {
        HojaDeVida h = hojas.findById(usuarioId)
                .orElseThrow(() -> new HojaException(Motivo.SIN_HOJA, "Aún no has subido tu hoja de vida"));
        EstadoHoja estado = EstadoHoja.valueOf(h.getEstado());
        if (estado != EstadoHoja.SIN_CONFIRMAR && estado != EstadoHoja.CONFIRMADA) {
            throw new HojaException(Motivo.ESTADO_INVALIDO, "Tu hoja de vida aún no tiene datos para revisar");
        }
        return h;
    }

    private DatosHoja leer(HojaDeVida h) {
        return json.readValue(h.getDatos(), DatosHoja.class);
    }

    private static ArchivoInfo info(HojaDeVida h) {
        return new ArchivoInfo(
                h.getNombreArchivo(),
                h.getActualizadaEn(),
                h.getIdioma() == null ? "es" : h.getIdioma(),
                h.getPaginas() == null ? 1 : h.getPaginas());
    }

    private static void validarFechas(ValoresHoja v) {
        boolean malas = lista(v.experiencia()).stream().anyMatch(x -> !fechaValida(x.inicio()) || !fechaValida(x.fin()))
                || lista(v.educacion()).stream().anyMatch(x -> !fechaValida(x.inicio()) || !fechaValida(x.fin()));
        if (malas) {
            throw new HojaException(Motivo.DATOS_INVALIDOS, "Las fechas deben tener año y mes");
        }
    }

    private static boolean fechaValida(String f) {
        return FECHA.matcher(f == null ? "" : f).matches();
    }

    private static Experiencia experiencia(ValorExperiencia x) {
        return new Experiencia(
                idSeguro(x.id()), Campo.leido(x.cargo()), Campo.leido(x.organizacion()),
                Campo.leido(x.inicio()), Campo.leido(x.fin()), Campo.leido(x.descripcion()));
    }

    private static Educacion educacion(ValorEducacion x) {
        return new Educacion(
                idSeguro(x.id()), Campo.leido(x.titulo()), Campo.leido(x.institucion()),
                Campo.leido(x.inicio()), Campo.leido(x.fin()));
    }

    private static String idSeguro(String id) {
        return id != null && id.matches("[A-Za-z0-9-]{1,40}") ? id : UUID.randomUUID().toString();
    }

    /** Solo el nombre visible (sin rutas ni caracteres de control); el archivo se guarda por id de usuario. */
    private static String nombreSeguro(String original, TipoArchivo tipo) {
        String nombre = original == null ? "" : original.replaceAll("^.*[\\\\/]", "").replaceAll("\\p{Cntrl}", "").strip();
        if (nombre.isEmpty()) nombre = "hoja-de-vida." + tipo.name().toLowerCase();
        return nombre.length() > 255 ? nombre.substring(nombre.length() - 255) : nombre;
    }

    private static <T> List<T> lista(List<T> l) {
        return l == null ? List.of() : l;
    }
}
