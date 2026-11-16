package com.camellofacil.backend;

import com.camellofacil.backend.vacantes.app.FuenteVacantes;
import com.camellofacil.backend.vacantes.app.ModelosVacante.VacanteExterna;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.boot.test.context.TestComponent;

/**
 * Fuente de vacantes falsa para las pruebas: responde lo que haya en {@link #RESPUESTA} (null = falla) y
 * anota cada búsqueda que recibe. Cupo: 10 por día.
 */
@TestComponent
public class FuenteDePrueba implements FuenteVacantes {

    public static final String CODIGO = "prueba";
    public static final AtomicReference<List<VacanteExterna>> RESPUESTA = new AtomicReference<>(List.of());
    public static final List<String> BUSQUEDAS = new CopyOnWriteArrayList<>();
    /** Simula una fuente internacional (como Jooble con jooble.org): solo sirven las remotas. */
    public static final AtomicBoolean SOLO_REMOTAS = new AtomicBoolean(false);
    /** Peticiones permitidas de una vez al empezar el periodo. */
    public static final java.util.concurrent.atomic.AtomicInteger ADELANTO = new java.util.concurrent.atomic.AtomicInteger(1);

    @Override
    public boolean soloRemotas() {
        return SOLO_REMOTAS.get();
    }

    @Override
    public String codigo() {
        return CODIGO;
    }

    @Override
    public boolean configurada() {
        return true;
    }

    @Override
    public Cupo cupo() {
        return new Cupo(10, Duration.ofDays(1), ADELANTO.get());
    }

    @Override
    public List<VacanteExterna> buscar(String consulta) throws FuenteException {
        BUSQUEDAS.add(consulta);
        List<VacanteExterna> respuesta = RESPUESTA.get();
        if (respuesta == null) throw new FuenteException(FuenteException.Motivo.AUTORIZACION, "llave falsa rechazada", null);
        return respuesta;
    }
}
