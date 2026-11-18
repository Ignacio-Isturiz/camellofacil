package com.camellofacil.backend;

import com.camellofacil.backend.cv.app.ModelosAnalisis.RecomendacionIa;
import com.camellofacil.backend.cv.app.ModelosHoja.ValoresHoja;
import com.camellofacil.backend.cv.app.RecomendadorIa;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** IA falsa para las recomendaciones de la hoja: devuelve lo que cada prueba indique, sin llamar a Groq. */
@TestConfiguration
public class RecomendadorIaDePrueba {

    /** Lo que dirá la «IA». Si es null, simula una falla del proveedor. */
    public static final AtomicReference<List<RecomendacionIa>> RESPUESTA = new AtomicReference<>();
    public static final AtomicReference<ValoresHoja> ULTIMA_HOJA = new AtomicReference<>();
    public static final AtomicInteger LLAMADAS = new AtomicInteger();

    @Bean
    @Primary
    RecomendadorIa recomendadorIaDePrueba() {
        return hoja -> {
            LLAMADAS.incrementAndGet();
            ULTIMA_HOJA.set(hoja);
            List<RecomendacionIa> respuesta = RESPUESTA.get();
            if (respuesta == null) throw new RecomendadorIa.RecomendadorIaException("IA de prueba sin respuesta", null);
            return respuesta;
        };
    }
}
