package com.camellofacil.backend.vacantes.app;

import com.camellofacil.backend.vacantes.app.ModelosVacante.VacanteExterna;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Un portal de empleo del que se traen vacantes (JSearch, Jooble...). Cada uno vive en un adaptador detrás
 * de esta interfaz, con su propio cupo de peticiones.
 */
public interface FuenteVacantes {

    /** Código corto y estable de la fuente (el mismo de las tablas consulta_vacantes y uso_fuente). */
    String codigo();

    /** Falso si falta la llave: la fuente no se consulta. */
    boolean configurada();

    Cupo cupo();

    /**
     * Verdadero si la fuente trae ofertas de otros países: entonces solo sirven las remotas (las que alguien
     * en Colombia puede aplicar) y las demás se descartan.
     */
    default boolean soloRemotas() {
        return false;
    }

    /** Una petición (una página) a la fuente. Consume una unidad del cupo, salga bien o mal. */
    List<VacanteExterna> buscar(String consulta) throws FuenteException;

    /**
     * Cuántas peticiones se permiten por periodo. Ya incluye el margen de seguridad (p. ej. 90 de las 100
     * diarias de Jooble).
     */
    record Cupo(int limite, Duration periodo, int adelanto) {

        /** @param adelanto peticiones que se pueden hacer apenas empieza el periodo (carga inicial); mínimo 1. */
        public Cupo {
            adelanto = Math.max(1, Math.min(adelanto, limite));
        }

        public Cupo(int limite, Duration periodo) {
            this(limite, periodo, 1);
        }

        /**
         * Cuántas peticiones se pueden haber hecho a esta altura del periodo: el adelanto de una vez y el resto
         * repartido parejo en el tiempo, para no gastarlo todo el primer día. Al final del periodo, el límite.
         */
        public int permitidasHasta(Instant inicioPeriodo, Instant ahora) {
            double transcurrido = Duration.between(inicioPeriodo, ahora).toMillis() / (double) periodo.toMillis();
            int parejo = adelanto + (int) Math.floor((limite - adelanto) * Math.max(0, transcurrido));
            return Math.min(limite, parejo);
        }
    }

    /** La fuente falló. {@code motivo} decide si se pausa la fuente (llave inválida, límite del proveedor). */
    class FuenteException extends Exception {

        public enum Motivo { AUTORIZACION, LIMITE, OTRO }

        private final Motivo motivo;

        public FuenteException(Motivo motivo, String mensaje, Throwable causa) {
            super(mensaje, causa);
            this.motivo = motivo;
        }

        public Motivo motivo() {
            return motivo;
        }
    }
}
