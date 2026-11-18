package com.camellofacil.backend.versiones.app;

import com.camellofacil.backend.versiones.app.VersionesService.ContenidoVersion;
import java.util.Optional;
import java.util.UUID;

/**
 * De dónde sale el contenido para abrir el editor sobre una hoja adaptada. Lo implementa el módulo adaptaciones;
 * la interfaz vive aquí para que versiones no dependa de adaptaciones (las dependencias van en un solo sentido).
 */
public interface OrigenAdaptacion {

    /** El contenido de la adaptación, solo si es de esa persona. */
    Optional<ContenidoVersion> contenido(UUID usuarioId, UUID adaptacionId);
}
