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

    public record SeccionDocumento(String id, String titulo, List<String> bloques) {}

    public record DocumentoHoja(String nombre, List<String> contacto, List<SeccionDocumento> secciones) {}
}
