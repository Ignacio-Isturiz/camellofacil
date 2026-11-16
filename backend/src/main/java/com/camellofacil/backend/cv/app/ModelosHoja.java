package com.camellofacil.backend.cv.app;

import java.time.Instant;
import java.util.List;

/** Tipos del módulo cv que comparten las capas app y web. */
public final class ModelosHoja {

    private ModelosHoja() {}

    /** Peso máximo del archivo (el mismo límite está en el frontend, nginx y la configuración multipart). */
    public static final int PESO_MAXIMO_BYTES = 20 * 1024 * 1024;

    public enum TipoArchivo {
        PDF,
        DOCX,
        DOC
    }

    public enum EstadoHoja {
        /** En la cola del análisis en segundo plano. */
        ANALIZANDO,
        /** Datos extraídos; falta que la persona los revise y confirme. */
        SIN_CONFIRMAR,
        CONFIRMADA,
        /** El archivo no tiene texto legible (p. ej. un escaneo). */
        ILEGIBLE,
        /** El análisis falló varias veces por un problema nuestro o del proveedor de IA. */
        ERROR
    }

    /** Estado de cada dato extraído: tal como lo espera la interfaz. */
    public static final String LEIDO = "leido";
    public static final String REVISA = "revisa";
    public static final String NO_ENCONTRADO = "no_encontrado";

    public record Campo(String valor, String estado, String ayuda) {
        public static Campo leido(String valor) {
            return new Campo(valor == null ? "" : valor, LEIDO, null);
        }
    }

    public record Contacto(Campo nombre, Campo telefono, Campo ciudad) {}

    public record Experiencia(String id, Campo cargo, Campo organizacion, Campo inicio, Campo fin, Campo descripcion) {}

    public record Educacion(String id, Campo titulo, Campo institucion, Campo inicio, Campo fin) {}

    /** Lo que se leyó de la hoja (y luego corrigió la persona). Se guarda en JSON. */
    public record DatosHoja(
            Contacto contacto, Campo perfil, List<Experiencia> experiencia, List<Educacion> educacion, Campo habilidades) {}

    public record ArchivoInfo(String nombre, Instant actualizadaEn, String idioma, int paginas) {}

    /** Resumen del estado de la hoja base de la persona. */
    public record ResumenHoja(EstadoHoja estado, ArchivoInfo archivo, Integer pendientesRevision) {}

    public record HojaConDatos(ArchivoInfo archivo, DatosHoja datos) {}

    /** Valores corregidos por la persona (solo texto). */
    public record ValoresHoja(
            String nombre,
            String telefono,
            String ciudad,
            String perfil,
            String habilidades,
            List<ValorExperiencia> experiencia,
            List<ValorEducacion> educacion) {}

    public record ValorExperiencia(String id, String cargo, String organizacion, String inicio, String fin, String descripcion) {}

    public record ValorEducacion(String id, String titulo, String institucion, String inicio, String fin) {}
}
