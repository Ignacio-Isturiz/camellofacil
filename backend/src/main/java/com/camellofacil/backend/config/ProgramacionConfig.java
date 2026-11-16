package com.camellofacil.backend.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Activa los procesos periódicos. Solo despiertan trabajo cuyo estado vive en Postgres (la cola de análisis
 * de hojas de vida, la ingesta de vacantes); nunca guardan nada en memoria.
 */
@Configuration
@EnableScheduling
class ProgramacionConfig {

    /** Reloj del sistema en UTC; las pruebas pueden reemplazarlo para controlar el tiempo. */
    @Bean
    Clock reloj() {
        return Clock.systemUTC();
    }
}
