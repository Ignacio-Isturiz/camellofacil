package com.camellofacil.backend.cv.app;

import java.util.List;

/**
 * Proveedor de IA que convierte el texto de una hoja de vida en datos estructurados. Hoy es Groq; cambiar de
 * proveedor es escribir otra implementación de esta interfaz.
 *
 * El texto de la hoja es contenido no confiable (puede traer instrucciones dirigidas a la IA): la respuesta
 * solo se acepta como datos y {@link ArmadorDatos} la verifica contra el texto original.
 */
public interface ExtractorIa {

    Extraccion extraer(String textoHoja) throws ExtractorIaException;

    /** Lo que devuelve la IA, sin verificar. Cualquier campo puede venir null. */
    record Extraccion(
            String idioma,
            String nombre,
            String telefono,
            String ciudad,
            String perfil,
            String habilidades,
            List<ExperienciaIa> experiencia,
            List<EducacionIa> educacion) {}

    record ExperienciaIa(String cargo, String organizacion, String inicio, String fin, String descripcion) {}

    record EducacionIa(String titulo, String institucion, String inicio, String fin) {}

    /** El proveedor falló (red, límite de uso, respuesta inválida o sin configurar). Se reintenta. */
    class ExtractorIaException extends Exception {
        public ExtractorIaException(String mensaje, Throwable causa) {
            super(mensaje, causa);
        }
    }
}
