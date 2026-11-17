package com.camellofacil.backend.postulaciones.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Cada hora borra del todo las postulaciones que la persona quitó de su historial hace más de un día. */
@Component
class LimpiezaPostulaciones {

    private static final Logger LOG = LoggerFactory.getLogger(LimpiezaPostulaciones.class);

    private final PostulacionesService postulaciones;

    LimpiezaPostulaciones(PostulacionesService postulaciones) {
        this.postulaciones = postulaciones;
    }

    @Scheduled(initialDelayString = "PT2M", fixedDelayString = "PT1H")
    void limpiar() {
        int borradas = postulaciones.borrarQuitadas();
        if (borradas > 0) LOG.info("Postulaciones: {} quitadas borradas del todo", borradas);
    }
}
