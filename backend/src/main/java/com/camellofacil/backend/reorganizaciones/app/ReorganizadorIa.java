package com.camellofacil.backend.reorganizaciones.app;

import com.camellofacil.backend.adaptaciones.app.AdaptadorIa.PropuestaIa;
import com.camellofacil.backend.cv.app.ModelosHoja.ValoresHoja;

/**
 * Proveedor de IA que redacta mejor el perfil y las descripciones de la hoja para un formato. Hoy es Groq. Su
 * respuesta nunca se usa tal cual: se compara con la hoja base y se descarta lo que traiga de más.
 */
public interface ReorganizadorIa {

    /**
     * @param hoja la hoja SIN datos de contacto
     * @param formato harvard | ats
     * @return solo `perfil` y las descripciones de `experiencia` (por id) se aprovechan; lo demás se ignora
     */
    PropuestaIa redactar(ValoresHoja hoja, String formato) throws ReorganizadorIaException;

    class ReorganizadorIaException extends Exception {
        public ReorganizadorIaException(String mensaje, Throwable causa) {
            super(mensaje, causa);
        }
    }
}
