package com.camellofacil.backend.cv.app;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Revisa la cola de análisis cada pocos segundos. La cola vive en Postgres, así que esto solo despierta el
 * trabajo: si el servidor se apaga, las hojas pendientes se procesan al volver. Se apaga en las pruebas.
 */
@Component
@ConditionalOnProperty(name = "camellofacil.cv.analisis-automatico", havingValue = "true", matchIfMissing = true)
class ProgramadorAnalisis {

    private final AnalisisHojaService analisis;

    ProgramadorAnalisis(AnalisisHojaService analisis) {
        this.analisis = analisis;
    }

    @Scheduled(fixedDelayString = "${camellofacil.cv.intervalo-analisis:3000}")
    void revisarCola() {
        // Procesa todas las pendientes que haya ahora (una por una) y vuelve a dormir.
        int maximoPorRonda = 10;
        while (maximoPorRonda-- > 0 && analisis.procesarSiguiente()) {
            // sigue con la siguiente
        }
    }
}
