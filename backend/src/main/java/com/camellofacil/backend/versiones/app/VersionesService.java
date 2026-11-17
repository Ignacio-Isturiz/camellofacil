package com.camellofacil.backend.versiones.app;

import com.camellofacil.backend.cv.app.HojaDeVidaService;
import com.camellofacil.backend.cv.app.ModelosHoja.EstadoHoja;
import com.camellofacil.backend.cv.app.ModelosHoja.ValoresHoja;
import com.camellofacil.backend.plantillas.app.ModelosPlantilla.DocumentoHoja;
import com.camellofacil.backend.plantillas.app.PlantillasService;
import com.camellofacil.backend.versiones.app.ModelosVersion.VersionResumen;
import com.camellofacil.backend.versiones.app.VersionException.Motivo;
import com.camellofacil.backend.versiones.persistence.VersionHoja;
import com.camellofacil.backend.versiones.persistence.VersionHojaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** Versiones de la hoja de vida: listarlas, verlas, renombrarlas, duplicarlas, elegir la base y eliminarlas. */
@Service
public class VersionesService {

    /** Tope por persona: evita que alguien llene la base de datos con versiones. */
    static final int MAXIMO_VERSIONES = 30;

    private static final String PLANTILLA_BASE = "ats-clasica";

    private final VersionHojaRepository versiones;
    private final HojaDeVidaService hojas;
    private final PlantillasService plantillas;
    private final JsonMapper json;

    public VersionesService(
            VersionHojaRepository versiones, HojaDeVidaService hojas, PlantillasService plantillas, JsonMapper json) {
        this.versiones = versiones;
        this.hojas = hojas;
        this.plantillas = plantillas;
        this.json = json;
    }

    /** Lo necesario para volver a editar una versión. `valores` es null si la versión no los guardó. */
    public record ContenidoVersion(String plantillaId, String idioma, ValoresHoja valores) {}

    /**
     * Lista las versiones de la persona. Con la hoja confirmada y sin versión base, la crea en ese momento con
     * los datos confirmados (así no hay que tocar el módulo cv para que exista).
     */
    @Transactional
    public List<VersionResumen> listar(UUID usuarioId, String correo) {
        asegurarBase(usuarioId, correo);
        return versiones.findByUsuarioIdOrderByCreadaEnDesc(usuarioId).stream().map(VersionesService::resumen).toList();
    }

    @Transactional(readOnly = true)
    public DocumentoHoja documento(UUID usuarioId, UUID id) {
        return json.readValue(buscar(usuarioId, id).getDocumento(), DocumentoHoja.class);
    }

    @Transactional(readOnly = true)
    public ContenidoVersion contenido(UUID usuarioId, UUID id) {
        VersionHoja v = buscar(usuarioId, id);
        ValoresHoja valores = v.getValores() == null ? null : json.readValue(v.getValores(), ValoresHoja.class);
        return new ContenidoVersion(v.getPlantillaId(), v.getIdioma(), valores);
    }

    @Transactional
    public VersionResumen renombrar(UUID usuarioId, UUID id, String nombre) {
        String limpio = nombre == null ? "" : nombre.strip();
        if (limpio.isEmpty() || limpio.length() > 120) {
            throw new VersionException(Motivo.DATOS_INVALIDOS, "El nombre debe tener entre 1 y 120 caracteres");
        }
        VersionHoja v = buscar(usuarioId, id);
        v.renombrar(limpio);
        return resumen(v);
    }

    @Transactional
    public VersionResumen duplicar(UUID usuarioId, UUID id) {
        VersionHoja origen = buscar(usuarioId, id);
        verificarCupo(usuarioId);
        String nombre = "Copia de " + origen.getNombre();
        VersionHoja copia = new VersionHoja(
                usuarioId, nombre.length() > 120 ? nombre.substring(0, 120) : nombre, origen.getTipo(), origen.getIdioma(),
                origen.getPlantillaId(), false, origen.getNota(), origen.getDocumento(), origen.getValores(), Instant.now());
        return resumen(versiones.save(copia));
    }

    @Transactional
    public void usarComoBase(UUID usuarioId, UUID id) {
        VersionHoja v = buscar(usuarioId, id);
        if (v.isEsBase()) return;
        versiones.quitarBase(usuarioId);
        // quitarBase limpia el contexto: se vuelve a leer la versión antes de marcarla.
        buscar(usuarioId, id).marcarBase(true);
    }

    @Transactional
    public void eliminar(UUID usuarioId, UUID id) {
        VersionHoja v = buscar(usuarioId, id);
        if (v.isEsBase()) {
            throw new VersionException(Motivo.NO_ELIMINABLE, "Es tu hoja base: elige otra como base para poder eliminarla");
        }
        versiones.delete(v);
    }

    /** Guarda una versión nueva (la usa el editor al guardar un borrador). */
    @Transactional
    public VersionResumen crear(
            UUID usuarioId, String nombre, String plantillaId, String idioma, ValoresHoja valores, DocumentoHoja documento) {
        verificarCupo(usuarioId);
        VersionHoja v = new VersionHoja(
                usuarioId, nombre, "BASE", idioma, plantillaId, false, null,
                json.writeValueAsString(documento), json.writeValueAsString(valores), Instant.now());
        return resumen(versiones.save(v));
    }

    private void asegurarBase(UUID usuarioId, String correo) {
        if (versiones.existsByUsuarioIdAndEsBaseTrue(usuarioId)) return;
        if (hojas.estado(usuarioId).orElse(null) != EstadoHoja.CONFIRMADA) return;
        String idioma = hojas.datos(usuarioId).archivo().idioma();
        ValoresHoja valores = hojas.valores(usuarioId);
        DocumentoHoja documento = plantillas.armar(PLANTILLA_BASE, valores, correo, idioma);
        versiones.save(new VersionHoja(
                usuarioId, "Mi hoja base", "BASE", idioma, PLANTILLA_BASE, true,
                "Armada con los datos que confirmaste", json.writeValueAsString(documento), json.writeValueAsString(valores),
                Instant.now()));
    }

    private void verificarCupo(UUID usuarioId) {
        if (versiones.countByUsuarioId(usuarioId) >= MAXIMO_VERSIONES) {
            throw new VersionException(Motivo.LIMITE, "Llegaste al máximo de " + MAXIMO_VERSIONES + " versiones: elimina alguna para crear otra");
        }
    }

    private VersionHoja buscar(UUID usuarioId, UUID id) {
        return versiones.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new VersionException(Motivo.NO_ENCONTRADA, "No encontramos esa versión"));
    }

    private static VersionResumen resumen(VersionHoja v) {
        return new VersionResumen(
                v.getId().toString(), v.getNombre(), v.getTipo().toLowerCase(Locale.ROOT), v.getCreadaEn(), v.getIdioma(), null, null,
                v.getNota(), !v.isEsBase(),
                v.isEsBase() ? "Es tu hoja base. Elige otra como base para poder eliminarla." : null);
    }
}
