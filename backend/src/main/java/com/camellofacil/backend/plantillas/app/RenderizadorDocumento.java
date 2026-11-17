package com.camellofacil.backend.plantillas.app;

import com.camellofacil.backend.cv.app.ModelosHoja.ValoresHoja;
import com.camellofacil.backend.plantillas.app.ModelosPlantilla.DocumentoHoja;
import com.camellofacil.backend.plantillas.app.ModelosPlantilla.PlantillaDetalle;
import com.camellofacil.backend.plantillas.app.ModelosPlantilla.SeccionDocumento;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Arma el documento estructurado de una hoja de vida con los valores de la persona, en el orden y con los
 * títulos de la plantilla. Solo coloca lo que la persona escribió: no completa ni redacta nada.
 */
@Component
public class RenderizadorDocumento {

    private static final Map<String, List<String>> TITULOS = Map.of(
            "perfil", List.of("Perfil", "Profile"),
            "experiencia", List.of("Experiencia", "Experience"),
            "educacion", List.of("Educación", "Education"),
            "habilidades", List.of("Habilidades", "Skills"));

    private static final Map<String, List<String>> ORDEN = Map.of(
            "harvard", List.of("perfil", "educacion", "experiencia", "habilidades"),
            "ats-clasico", List.of("perfil", "experiencia", "educacion", "habilidades"),
            "ats-moderno", List.of("perfil", "experiencia", "educacion", "habilidades"),
            "ats-compacto", List.of("perfil", "habilidades", "experiencia", "educacion"));

    public DocumentoHoja renderizar(PlantillaDetalle plantilla, ValoresHoja v, String correo, String idioma) {
        boolean en = "en".equals(idioma);
        List<String> contacto = new ArrayList<>();
        for (String dato : new String[] {correo, v.telefono(), v.ciudad()}) {
            if (!vacio(dato)) contacto.add(dato.strip());
        }
        List<SeccionDocumento> secciones = new ArrayList<>();
        for (String id : ORDEN.getOrDefault(plantilla.estilo(), ORDEN.get("ats-clasico"))) {
            List<String> bloques = bloques(id, v, en);
            if (!bloques.isEmpty()) {
                secciones.add(new SeccionDocumento(id, TITULOS.get(id).get(en ? 1 : 0), bloques));
            }
        }
        return new DocumentoHoja(vacio(v.nombre()) ? "" : v.nombre().strip(), contacto, secciones);
    }

    private static List<String> bloques(String seccion, ValoresHoja v, boolean en) {
        return switch (seccion) {
            case "perfil" -> vacio(v.perfil()) ? List.of() : List.of(v.perfil().strip());
            case "habilidades" -> vacio(v.habilidades()) ? List.of() : List.of(v.habilidades().strip());
            case "experiencia" -> v.experiencia() == null ? List.of() : v.experiencia().stream()
                    .map(x -> linea(x.cargo(), x.organizacion(), x.inicio(), x.fin(), x.descripcion(), en))
                    .filter(s -> !s.isEmpty()).toList();
            case "educacion" -> v.educacion() == null ? List.of() : v.educacion().stream()
                    .map(x -> linea(x.titulo(), x.institucion(), x.inicio(), x.fin(), null, en))
                    .filter(s -> !s.isEmpty()).toList();
            default -> List.of();
        };
    }

    /** «Cargo — Organización (2024-02 – 2024-11). Descripción». */
    private static String linea(String titulo, String lugar, String inicio, String fin, String detalle, boolean en) {
        StringBuilder s = new StringBuilder();
        if (!vacio(titulo)) s.append(titulo.strip());
        if (!vacio(lugar)) s.append(s.length() > 0 ? " — " : "").append(lugar.strip());
        if (!vacio(inicio) || !vacio(fin)) {
            String desde = vacio(inicio) ? "" : inicio.strip();
            String hasta = vacio(fin) ? (en ? "Present" : "Actual") : fin.strip();
            s.append(s.length() > 0 ? " " : "").append('(').append(desde).append(" – ").append(hasta).append(')');
        }
        if (!vacio(detalle)) s.append(s.length() > 0 ? ". " : "").append(detalle.strip());
        return s.toString();
    }

    private static boolean vacio(String s) {
        return s == null || s.isBlank();
    }
}
