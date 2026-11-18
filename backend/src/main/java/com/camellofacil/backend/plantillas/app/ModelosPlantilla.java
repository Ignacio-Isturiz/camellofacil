package com.camellofacil.backend.plantillas.app;

import java.util.List;

/** Tipos del módulo plantillas que comparten las capas app y web, y que usan los módulos que arman documentos. */
public final class ModelosPlantilla {

    private ModelosPlantilla() {}

    /** `categoria`: harvard | ats. `estilo` define cómo dibuja la interfaz la miniatura. */
    public record Plantilla(
            String id, String nombre, String descripcion, String categoria, String estilo, List<String> idiomas, String etiqueta) {}

    /** `seccion`: contacto | perfil | experiencia | educacion | habilidades. El ejemplo es texto genérico, nunca datos reales. */
    public record GuiaSeccion(String seccion, String que, String ejemplo) {}

    public record PlantillaDetalle(
            String id, String nombre, String descripcion, String categoria, String estilo, List<String> idiomas, String etiqueta,
            List<GuiaSeccion> guias) {}

    /**
     * Un cargo o un estudio con sus partes por separado, para que el PDF y el Word puedan acomodarlas (título y
     * fechas en una línea, lugar debajo, logros en viñetas). `fechas` ya viene escrita («ene 2024 – Actual»).
     */
    public record EntradaDocumento(String titulo, String lugar, String fechas, String detalle) {}

    /**
     * `bloques` es el texto plano de cada elemento (lo que dibuja la interfaz). `entradas` repite las experiencias y
     * estudios con sus partes; está vacía en documentos guardados antes de que existiera y en perfil y habilidades.
     */
    public record SeccionDocumento(String id, String titulo, List<String> bloques, List<EntradaDocumento> entradas) {
        public SeccionDocumento {
            entradas = entradas == null ? List.of() : entradas;
        }

        public SeccionDocumento(String id, String titulo, List<String> bloques) {
            this(id, titulo, bloques, List.of());
        }
    }

    public record DocumentoHoja(String nombre, List<String> contacto, List<SeccionDocumento> secciones) {}
}
