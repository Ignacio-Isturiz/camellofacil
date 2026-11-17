package com.camellofacil.backend.versiones.app;

import com.camellofacil.backend.cv.app.ModelosHoja.ValoresHoja;
import java.time.Instant;

/** Tipos del módulo versiones que comparten las capas app y web. */
public final class ModelosVersion {

    private ModelosVersion() {}

    /** `tipo`: base | reorganizada | adaptada. Los puntajes llegan con el análisis Harvard / ATS. */
    public record VersionResumen(
            String id, String nombre, String tipo, Instant creadaEn, String idioma, Integer puntajeHarvard, Integer puntajeAts,
            String nota, boolean eliminable, String motivoNoEliminable) {}

    public record SolicitudBorrador(String plantillaId, String versionId, String idioma) {}

    public record BorradorInfo(String id, String plantillaId, String idioma, String correo, ValoresHoja valores, Instant guardadoEn) {}
}
