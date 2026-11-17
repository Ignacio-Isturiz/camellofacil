package com.camellofacil.backend.plantillas.app;

import com.camellofacil.backend.plantillas.app.ModelosPlantilla.GuiaSeccion;
import com.camellofacil.backend.plantillas.app.ModelosPlantilla.Plantilla;
import com.camellofacil.backend.plantillas.app.ModelosPlantilla.PlantillaDetalle;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Las plantillas propias de la plataforma (Harvard y ATS). Son parte del código: no dependen de proveedores
 * externos ni de tablas, así que el contenedor sigue sin estado.
 */
@Component
public class CatalogoPlantillas {

    private static final List<String> AMBOS = List.of("es", "en");

    private static final List<GuiaSeccion> GUIAS = List.of(
            new GuiaSeccion("contacto", "Tu nombre, teléfono, ciudad y el correo de tu cuenta. Nada más: sin foto ni documento.",
                    "Nombre Apellido · 300 000 0000 · Ciudad"),
            new GuiaSeccion("perfil", "Dos o tres frases: quién eres, qué estudias y qué buscas.",
                    "Estudiante de séptimo semestre que busca una práctica donde aplicar análisis de datos."),
            new GuiaSeccion("experiencia", "Cargo, organización y fechas; después, qué hiciste y qué lograste, con verbos de acción.",
                    "Monitor de laboratorio — Universidad de Ejemplo (2024-02 – 2024-11)"),
            new GuiaSeccion("educacion", "Programa, institución y fechas. Lo más reciente primero.",
                    "Ingeniería de Sistemas — Universidad de Ejemplo (2021-01 – Actual)"),
            new GuiaSeccion("habilidades", "Herramientas, tecnologías e idiomas que de verdad dominas, separadas por comas.",
                    "Python, SQL, trabajo en equipo, inglés B2"));

    private static final List<PlantillaDetalle> TODAS = List.of(
            new PlantillaDetalle("harvard-clasica", "Harvard clásica",
                    "Formato sobrio de una página, con la formación académica primero. Ideal para tu primera práctica.",
                    "harvard", "harvard", AMBOS, "Recomendada para prácticas", GUIAS),
            new PlantillaDetalle("ats-clasica", "ATS clásica",
                    "Una sola columna y títulos estándar, para que los sistemas de selección la lean sin errores.",
                    "ats", "ats-clasico", AMBOS, null, GUIAS),
            new PlantillaDetalle("ats-moderna", "ATS moderna",
                    "Misma estructura que la ATS clásica, con un diseño más actual.",
                    "ats", "ats-moderno", AMBOS, null, GUIAS),
            new PlantillaDetalle("ats-compacta", "ATS compacta",
                    "Pone las habilidades arriba y cabe en una página aunque tengas varias experiencias.",
                    "ats", "ats-compacto", AMBOS, null, GUIAS));

    public List<Plantilla> listar() {
        return TODAS.stream().map(CatalogoPlantillas::resumen).toList();
    }

    public Optional<PlantillaDetalle> buscar(String id) {
        return TODAS.stream().filter(p -> p.id().equals(id)).findFirst();
    }

    private static Plantilla resumen(PlantillaDetalle d) {
        return new Plantilla(d.id(), d.nombre(), d.descripcion(), d.categoria(), d.estilo(), d.idiomas(), d.etiqueta());
    }
}
