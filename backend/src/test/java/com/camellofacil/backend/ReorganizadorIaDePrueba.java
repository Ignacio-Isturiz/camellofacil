package com.camellofacil.backend;

import com.camellofacil.backend.adaptaciones.app.AdaptadorIa.PropuestaIa;
import com.camellofacil.backend.cv.app.ModelosHoja.ValoresHoja;
import com.camellofacil.backend.reorganizaciones.app.ReorganizadorIa;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** IA falsa para reorganizar: devuelve lo que cada prueba indique, sin llamar a Groq. */
@TestConfiguration
public class ReorganizadorIaDePrueba {

    /** Lo que dirá la «IA». Si es null, simula una falla del proveedor. */
    public static final AtomicReference<PropuestaIa> RESPUESTA = new AtomicReference<>();
    public static final AtomicReference<ValoresHoja> ULTIMA_HOJA = new AtomicReference<>();
    public static final AtomicInteger LLAMADAS = new AtomicInteger();

    @Bean
    @Primary
    ReorganizadorIa reorganizadorIaDePrueba() {
        return (hoja, formato) -> {
            LLAMADAS.incrementAndGet();
            ULTIMA_HOJA.set(hoja);
            PropuestaIa respuesta = RESPUESTA.get();
            if (respuesta == null) throw new ReorganizadorIa.ReorganizadorIaException("IA de prueba sin respuesta", null);
            return respuesta;
        };
    }
}
