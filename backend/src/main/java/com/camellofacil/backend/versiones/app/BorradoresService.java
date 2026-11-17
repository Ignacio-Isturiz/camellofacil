package com.camellofacil.backend.versiones.app;

import com.camellofacil.backend.cv.app.HojaDeVidaService;
import com.camellofacil.backend.cv.app.ModelosHoja.ValoresHoja;
import com.camellofacil.backend.plantillas.app.ModelosPlantilla.DocumentoHoja;
import com.camellofacil.backend.plantillas.app.ModelosPlantilla.PlantillaDetalle;
import com.camellofacil.backend.plantillas.app.PlantillasService;
import com.camellofacil.backend.versiones.app.ModelosVersion.BorradorInfo;
import com.camellofacil.backend.versiones.app.ModelosVersion.SolicitudBorrador;
import com.camellofacil.backend.versiones.app.ModelosVersion.VersionResumen;
import com.camellofacil.backend.versiones.app.VersionException.Motivo;
import com.camellofacil.backend.versiones.app.VersionesService.ContenidoVersion;
import com.camellofacil.backend.versiones.persistence.BorradorHoja;
import com.camellofacil.backend.versiones.persistence.BorradorHojaRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** Editor de la hoja de vida: abre un borrador desde una plantilla o una versión, lo guarda y lo vuelve versión. */
@Service
public class BorradoresService {

    /** Borradores abiertos por persona; al pasar el tope se descartan los más antiguos. */
    static final int MAXIMO_BORRADORES = 10;

    private static final Pattern FECHA = Pattern.compile("^(\\d{4}-(0[1-9]|1[0-2]))?$");

    private final BorradorHojaRepository borradores;
    private final VersionesService versiones;
    private final PlantillasService plantillas;
    private final HojaDeVidaService hojas;
    private final JsonMapper json;

    public BorradoresService(
            BorradorHojaRepository borradores,
            VersionesService versiones,
            PlantillasService plantillas,
            HojaDeVidaService hojas,
            JsonMapper json) {
        this.borradores = borradores;
        this.versiones = versiones;
        this.plantillas = plantillas;
        this.hojas = hojas;
        this.json = json;
    }

    @Transactional
    public BorradorInfo crear(UUID usuarioId, String correo, SolicitudBorrador solicitud) {
        String plantillaId;
        String idioma;
        ValoresHoja valores;
        if (solicitud.versionId() != null && !solicitud.versionId().isBlank()) {
            ContenidoVersion c = versiones.contenido(usuarioId, uuid(solicitud.versionId()));
            plantillaId = c.plantillaId();
            idioma = c.idioma();
            valores = c.valores() != null ? c.valores() : hojas.valores(usuarioId);
        } else if (solicitud.plantillaId() != null && !solicitud.plantillaId().isBlank()) {
            plantillaId = solicitud.plantillaId();
            idioma = solicitud.idioma() == null || solicitud.idioma().isBlank() ? "es" : solicitud.idioma();
            valores = hojas.valores(usuarioId);
        } else {
            throw new VersionException(Motivo.DATOS_INVALIDOS, "Elige una plantilla o una versión para empezar");
        }
        validarPlantilla(plantillaId, idioma);
        descartarSobrantes(usuarioId);
        BorradorHoja b = borradores.save(
                new BorradorHoja(usuarioId, plantillaId, idioma, json.writeValueAsString(valores), Instant.now()));
        return info(b, correo);
    }

    @Transactional
    public BorradorInfo guardar(UUID usuarioId, String correo, UUID id, ValoresHoja valores) {
        BorradorHoja b = buscar(usuarioId, id);
        validarFechas(valores);
        b.guardar(json.writeValueAsString(valores), Instant.now());
        return info(b, correo);
    }

    /** Arma el documento con los valores del borrador y lo guarda como una versión nueva. */
    @Transactional
    public VersionResumen guardarComoVersion(UUID usuarioId, String correo, UUID id) {
        BorradorHoja b = buscar(usuarioId, id);
        ValoresHoja valores = json.readValue(b.getValores(), ValoresHoja.class);
        DocumentoHoja documento = plantillas.armar(b.getPlantillaId(), valores, correo, b.getIdioma());
        PlantillaDetalle plantilla = plantillas.detalle(b.getPlantillaId());
        String nombre = plantilla.nombre() + " · " + LocalDate.now();
        return versiones.crear(usuarioId, nombre, b.getPlantillaId(), b.getIdioma(), valores, documento);
    }

    private void validarPlantilla(String plantillaId, String idioma) {
        PlantillaDetalle p = plantillas.detalle(plantillaId);
        if (!p.idiomas().contains(idioma)) {
            throw new VersionException(Motivo.DATOS_INVALIDOS, "Esa plantilla no está disponible en ese idioma");
        }
    }

    private void descartarSobrantes(UUID usuarioId) {
        List<BorradorHoja> todos = borradores.findByUsuarioIdOrderByCreadoEnAsc(usuarioId);
        int sobran = todos.size() - (MAXIMO_BORRADORES - 1);
        if (sobran > 0) {
            borradores.deleteAll(todos.subList(0, sobran));
        }
    }

    private BorradorHoja buscar(UUID usuarioId, UUID id) {
        return borradores.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new VersionException(Motivo.NO_ENCONTRADA, "No encontramos ese borrador"));
    }

    private BorradorInfo info(BorradorHoja b, String correo) {
        return new BorradorInfo(
                b.getId().toString(), b.getPlantillaId(), b.getIdioma(), correo,
                json.readValue(b.getValores(), ValoresHoja.class), b.getGuardadoEn());
    }

    private static void validarFechas(ValoresHoja v) {
        boolean malas = lista(v.experiencia()).stream().anyMatch(x -> !fecha(x.inicio()) || !fecha(x.fin()))
                || lista(v.educacion()).stream().anyMatch(x -> !fecha(x.inicio()) || !fecha(x.fin()));
        if (malas) {
            throw new VersionException(Motivo.DATOS_INVALIDOS, "Las fechas deben tener año y mes");
        }
    }

    private static boolean fecha(String f) {
        return FECHA.matcher(f == null ? "" : f).matches();
    }

    private static <T> List<T> lista(List<T> l) {
        return l == null ? List.of() : l;
    }

    private static UUID uuid(String texto) {
        try {
            return UUID.fromString(texto);
        } catch (IllegalArgumentException e) {
            throw new VersionException(Motivo.NO_ENCONTRADA, "No encontramos esa versión");
        }
    }
}
