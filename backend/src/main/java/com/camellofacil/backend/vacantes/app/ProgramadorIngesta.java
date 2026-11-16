package com.camellofacil.backend.vacantes.app;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Despierta la ingesta cada pocos minutos. El cupo y el turno de cada búsqueda viven en Postgres, así que
 * despertar seguido no gasta de más: si no hay cupo para este momento, la ronda no consulta nada.
 * Se apaga en las pruebas.
 */
@Component
@ConditionalOnProperty(name = "camellofacil.vacantes.ingesta-automatica", havingValue = "true", matchIfMissing = true)
class ProgramadorIngesta {

    private final IngestaVacantesService ingesta;

    ProgramadorIngesta(IngestaVacantesService ingesta) {
        this.ingesta = ingesta;
    }

    @Scheduled(initialDelayString = "${camellofacil.vacantes.retraso-inicial:60000}",
            fixedDelayString = "${camellofacil.vacantes.intervalo-ingesta:300000}")
    void ronda() {
        ingesta.ronda();
    }
}
