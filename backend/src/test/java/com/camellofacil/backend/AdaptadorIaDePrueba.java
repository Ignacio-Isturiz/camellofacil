package com.camellofacil.backend;

import com.camellofacil.backend.adaptaciones.app.AdaptadorIa;
import com.camellofacil.backend.adaptaciones.app.AdaptadorIa.PropuestaIa;
import com.camellofacil.backend.cv.app.ModelosHoja.ValoresHoja;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** IA falsa para la adaptación: devuelve lo que cada prueba indique, sin llamar a Groq. */
@TestConfiguration
public class AdaptadorIaDePrueba {

    /** Propuesta que dará la «IA». Si es null, simula una falla del proveedor. */
    public static final AtomicReference<PropuestaIa> RESPUESTA = new AtomicReference<>();
    /** La última hoja que recibió: sirve para comprobar que no se le envían datos de contacto. */
    public static final AtomicReference<ValoresHoja> ULTIMA_HOJA = new AtomicReference<>();
    public static final AtomicInteger LLAMADAS = new AtomicInteger();

    @Bean
    @Primary
    AdaptadorIa adaptadorIaDePrueba() {
        return (hoja, vacante) -> {
            LLAMADAS.incrementAndGet();
            ULTIMA_HOJA.set(hoja);
            PropuestaIa respuesta = RESPUESTA.get();
            if (respuesta == null) throw new AdaptadorIa.AdaptadorIaException("IA de prueba sin respuesta", null);
            return respuesta;
        };
    }
}
