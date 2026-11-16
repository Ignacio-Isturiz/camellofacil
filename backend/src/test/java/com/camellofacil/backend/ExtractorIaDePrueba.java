package com.camellofacil.backend;

import com.camellofacil.backend.cv.app.ExtractorIa;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** IA falsa para las pruebas: devuelve lo que cada prueba le indique, sin llamar a Groq. */
@TestConfiguration
public class ExtractorIaDePrueba {

    /** Respuesta que dará la «IA». Si es null, simula una falla del proveedor. */
    public static final AtomicReference<ExtractorIa.Extraccion> RESPUESTA = new AtomicReference<>();

    @Bean
    @Primary
    ExtractorIa extractorIaDePrueba() {
        return texto -> {
            ExtractorIa.Extraccion respuesta = RESPUESTA.get();
            if (respuesta == null) throw new ExtractorIa.ExtractorIaException("IA de prueba sin respuesta", null);
            return respuesta;
        };
    }
}
