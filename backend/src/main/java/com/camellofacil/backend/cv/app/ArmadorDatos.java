package com.camellofacil.backend.cv.app;

import com.camellofacil.backend.cv.app.ExtractorIa.EducacionIa;
import com.camellofacil.backend.cv.app.ExtractorIa.ExperienciaIa;
import com.camellofacil.backend.cv.app.ExtractorIa.Extraccion;
import com.camellofacil.backend.cv.app.ModelosHoja.Campo;
import com.camellofacil.backend.cv.app.ModelosHoja.Contacto;
import com.camellofacil.backend.cv.app.ModelosHoja.DatosHoja;
import com.camellofacil.backend.cv.app.ModelosHoja.Educacion;
import com.camellofacil.backend.cv.app.ModelosHoja.Experiencia;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Convierte la respuesta de la IA en los datos que ve la persona, sin confiar en ella (RN-03):
 * cada texto debe aparecer en el archivo original; si no, se marca «Revisa». También recorta longitudes y
 * cantidades para que una respuesta anómala no llene la base de datos.
 */
final class ArmadorDatos {

    static final int MAXIMO_ITEMS = 15;
    private static final Pattern FECHA = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");
    private static final Pattern NO_ALFANUMERICO = Pattern.compile("[^\\p{L}\\p{N}]+");

    private ArmadorDatos() {}

    static DatosHoja armar(Extraccion e, String textoOriginal) {
        String fuente = normalizar(textoOriginal);
        Contacto contacto = new Contacto(
                texto(e.nombre(), 150, fuente),
                telefono(e.telefono(), fuente),
                texto(e.ciudad(), 100, fuente));
        List<Experiencia> experiencia = listar(e.experiencia()).stream()
                .limit(MAXIMO_ITEMS)
                .map(x -> experiencia(x, fuente))
                .toList();
        List<Educacion> educacion = listar(e.educacion()).stream()
                .limit(MAXIMO_ITEMS)
                .map(x -> educacion(x, fuente))
                .toList();
        // Perfil y habilidades suelen venir resumidos o reordenados: no se exige coincidencia exacta.
        return new DatosHoja(
                contacto,
                libre(e.perfil(), 2000),
                experiencia,
                educacion,
                libre(e.habilidades(), 1000));
    }

    static String idioma(Extraccion e) {
        return "en".equals(e.idioma()) ? "en" : "es";
    }

    /** Cuántos datos debe revisar la persona antes de confirmar. */
    static int pendientes(DatosHoja d) {
        int n = cuenta(d.contacto().nombre()) + cuenta(d.contacto().telefono()) + cuenta(d.contacto().ciudad())
                + cuenta(d.perfil()) + cuenta(d.habilidades());
        for (Experiencia x : d.experiencia()) {
            n += cuenta(x.cargo()) + cuenta(x.organizacion()) + cuenta(x.inicio()) + cuenta(x.fin()) + cuenta(x.descripcion());
        }
        for (Educacion x : d.educacion()) {
            n += cuenta(x.titulo()) + cuenta(x.institucion()) + cuenta(x.inicio()) + cuenta(x.fin());
        }
        return n;
    }

    private static int cuenta(Campo c) {
        return ModelosHoja.REVISA.equals(c.estado()) ? 1 : 0;
    }

    private static Experiencia experiencia(ExperienciaIa x, String fuente) {
        return new Experiencia(
                UUID.randomUUID().toString(),
                texto(x.cargo(), 150, fuente),
                texto(x.organizacion(), 150, fuente),
                fecha(x.inicio()),
                fecha(x.fin()),
                libre(x.descripcion(), 1500));
    }

    private static Educacion educacion(EducacionIa x, String fuente) {
        return new Educacion(
                UUID.randomUUID().toString(),
                texto(x.titulo(), 200, fuente),
                texto(x.institucion(), 200, fuente),
                fecha(x.inicio()),
                fecha(x.fin()));
    }

    /** Un dato corto (nombre, cargo, institución) debe aparecer en el archivo. */
    private static Campo texto(String valor, int maximo, String fuente) {
        String v = limpiar(valor, maximo);
        if (v.isEmpty()) return noEncontrado();
        if (!fuente.contains(normalizar(v))) {
            return new Campo(v, ModelosHoja.REVISA, "No lo encontramos tal cual en tu archivo. Revisa que esté bien.");
        }
        return Campo.leido(v);
    }

    private static Campo telefono(String valor, String fuente) {
        String v = limpiar(valor, 40);
        if (v.isEmpty()) return noEncontrado();
        String digitos = v.replaceAll("\\D", "");
        String digitosFuente = fuente.replaceAll("\\D", "");
        if (digitos.length() < 7 || !digitosFuente.contains(digitos)) {
            return new Campo(v, ModelosHoja.REVISA, "Revisa que el número esté completo.");
        }
        return Campo.leido(v);
    }

    private static Campo fecha(String valor) {
        String v = limpiar(valor, 7);
        if (v.isEmpty()) return noEncontrado();
        if (!FECHA.matcher(v).matches()) {
            return new Campo("", ModelosHoja.REVISA, "Escribe la fecha como año y mes.");
        }
        return Campo.leido(v);
    }

    private static Campo libre(String valor, int maximo) {
        String v = limpiar(valor, maximo);
        return v.isEmpty() ? noEncontrado() : Campo.leido(v);
    }

    private static Campo noEncontrado() {
        return new Campo("", ModelosHoja.NO_ENCONTRADO, null);
    }

    private static String limpiar(String valor, int maximo) {
        if (valor == null) return "";
        String v = valor.strip();
        return v.length() > maximo ? v.substring(0, maximo).strip() : v;
    }

    /** Minúsculas, sin tildes y sin signos: «José  Pérez-Gómez» y «jose perez gomez» coinciden. */
    static String normalizar(String texto) {
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return NO_ALFANUMERICO.matcher(sinTildes.toLowerCase(Locale.ROOT)).replaceAll(" ").strip();
    }

    private static <T> List<T> listar(List<T> lista) {
        return lista == null ? List.of() : lista.stream().filter(java.util.Objects::nonNull).toList();
    }
}
