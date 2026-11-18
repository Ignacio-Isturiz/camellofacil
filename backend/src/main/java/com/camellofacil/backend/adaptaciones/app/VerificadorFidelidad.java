package com.camellofacil.backend.adaptaciones.app;

import com.camellofacil.backend.adaptaciones.app.AdaptadorIa.ExperienciaIa;
import com.camellofacil.backend.adaptaciones.app.AdaptadorIa.PropuestaIa;
import com.camellofacil.backend.adaptaciones.app.ModelosAdaptacion.CambioAdaptacion;
import com.camellofacil.backend.cv.app.ModelosHoja.ValorExperiencia;
import com.camellofacil.backend.cv.app.ModelosHoja.ValoresHoja;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Regla RN-03: la adaptación no inventa datos. Esto se verifica aquí, en código, y no solo con instrucciones
 * al modelo. De la propuesta de la IA solo se acepta:
 * <ul>
 *   <li>reordenar experiencias y habilidades que YA están en la hoja base;
 *   <li>reescribir el perfil y las descripciones, siempre que cada cifra, sigla y nombre propio del texto nuevo
 *       aparezca en la hoja base (si no, ese texto se descarta y queda el original);
 *   <li>avisos de lo que la vacante pide y falta (se muestran, nunca se agregan).
 * </ul>
 * Cargos, organizaciones, fechas, estudios y datos de contacto jamás vienen de la IA.
 */
public final class VerificadorFidelidad {

    private static final int MAXIMO_AVISOS = 5;
    private static final int LARGO_AVISO = 200;
    private static final Pattern PALABRA = Pattern.compile("[\\p{L}\\p{N}][\\p{L}\\p{N}+#./-]*");
    private static final Pattern SEPARADOR_HABILIDADES = Pattern.compile("[,;\\n]");

    private VerificadorFidelidad() {}

    public record Resultado(ValoresHoja valores, List<CambioAdaptacion> cambios, List<String> avisos) {}

    public static Resultado verificar(ValoresHoja base, PropuestaIa propuesta) {
        Set<String> corpus = corpus(base);
        List<CambioAdaptacion> cambios = new ArrayList<>();

        // Perfil: solo se reescribe si ya había uno y el texto nuevo no trae nada que la hoja no respalde.
        String perfil = base.perfil();
        if (!vacio(base.perfil()) && !vacio(propuesta.perfil()) && fiel(propuesta.perfil(), corpus)
                && !propuesta.perfil().strip().equals(base.perfil().strip())) {
            perfil = propuesta.perfil().strip();
            cambios.add(cambio(cambios, "redaccion", "Perfil ajustado a la vacante",
                    "Reescribimos tu perfil para resaltar lo que la vacante pide, sin agregar datos nuevos."));
        }

        // Experiencias: mismas filas de la base; solo cambian el orden y, si es fiel, la descripción.
        List<ValorExperiencia> originales = base.experiencia() == null ? List.of() : base.experiencia();
        Map<String, String> descripciones = new LinkedHashMap<>();
        List<String> ordenIa = new ArrayList<>();
        for (ExperienciaIa e : propuesta.experiencia() == null ? List.<ExperienciaIa>of() : propuesta.experiencia()) {
            if (e == null || e.id() == null) continue;
            ordenIa.add(e.id());
            descripciones.put(e.id(), e.descripcion());
        }
        List<ValorExperiencia> ordenadas = ordenar(originales, ordenIa);
        List<ValorExperiencia> experiencia = new ArrayList<>();
        for (ValorExperiencia x : ordenadas) {
            String nueva = descripciones.get(x.id());
            String descripcion = x.descripcion();
            if (!vacio(x.descripcion()) && !vacio(nueva) && fiel(nueva, corpus) && !nueva.strip().equals(x.descripcion().strip())) {
                descripcion = nueva.strip();
                cambios.add(cambio(cambios, "redaccion", "Descripción de «" + x.cargo() + "» ajustada",
                        "Cambiamos la redacción para acercarla a la vacante; los hechos son los mismos de tu hoja."));
            }
            experiencia.add(new ValorExperiencia(x.id(), x.cargo(), x.organizacion(), x.inicio(), x.fin(), descripcion));
        }
        if (!ids(ordenadas).equals(ids(originales))) {
            cambios.add(cambio(cambios, "movido", "Experiencias reordenadas",
                    "Pusimos primero la experiencia más cercana a lo que pide la vacante."));
        }

        // Habilidades: solo las que ya tenía, con su misma escritura; la IA únicamente decide el orden.
        List<String> habilidadesBase = habilidades(base.habilidades());
        List<String> habilidadesNuevas = ordenarHabilidades(habilidadesBase, propuesta.habilidades());
        String habilidades = base.habilidades();
        if (!habilidadesNuevas.equals(habilidadesBase)) {
            habilidades = String.join(", ", habilidadesNuevas);
            cambios.add(cambio(cambios, "destacado", "Habilidades destacadas",
                    "Pusimos primero: " + String.join(", ", habilidadesNuevas.subList(0, Math.min(3, habilidadesNuevas.size()))) + "."));
        }

        List<String> avisos = new ArrayList<>();
        for (String aviso : propuesta.avisos() == null ? List.<String>of() : propuesta.avisos()) {
            if (vacio(aviso) || avisos.size() >= MAXIMO_AVISOS) continue;
            String limpio = aviso.strip().replaceAll("\\p{Cntrl}", " ");
            avisos.add(limpio.length() > LARGO_AVISO ? limpio.substring(0, LARGO_AVISO) : limpio);
        }

        ValoresHoja valores = new ValoresHoja(
                base.nombre(), base.telefono(), base.ciudad(), perfil, habilidades, experiencia, base.educacion());
        return new Resultado(valores, cambios, avisos);
    }

    /**
     * Un texto es fiel si cada cifra, sigla y nombre propio que contiene (salvo el que abre una oración) aparece
     * en la hoja base. Las demás palabras pueden cambiar: es la redacción, que sí se puede ajustar.
     */
    static boolean fiel(String texto, Set<String> corpus) {
        Matcher m = PALABRA.matcher(texto);
        while (m.find()) {
            String token = recortar(m.group());
            if (token.isEmpty()) continue;
            boolean exigente = tieneDigito(token) || esSigla(token) || (Character.isUpperCase(token.codePointAt(0)) && !abreOracion(texto, m.start()));
            if (exigente && !enCorpus(token, corpus)) return false;
        }
        return true;
    }

    /** Todas las palabras de la hoja base, en minúscula (con sus partes si traen «/», «.» o «-»). */
    static Set<String> corpus(ValoresHoja v) {
        StringBuilder todo = new StringBuilder();
        // Sin nombre, teléfono ni ciudad: la IA no los recibe y no tienen por qué aparecer en un perfil o una descripción.
        for (String s : new String[] {v.perfil(), v.habilidades()}) {
            if (s != null) todo.append(s).append('\n');
        }
        if (v.experiencia() != null) {
            v.experiencia().forEach(x -> todo.append(x.cargo()).append('\n').append(x.organizacion()).append('\n')
                    .append(x.inicio()).append('\n').append(x.fin()).append('\n').append(x.descripcion()).append('\n'));
        }
        if (v.educacion() != null) {
            v.educacion().forEach(x -> todo.append(x.titulo()).append('\n').append(x.institucion()).append('\n')
                    .append(x.inicio()).append('\n').append(x.fin()).append('\n'));
        }
        Set<String> palabras = new HashSet<>();
        Matcher m = PALABRA.matcher(todo);
        while (m.find()) {
            String token = recortar(m.group()).toLowerCase(Locale.ROOT);
            palabras.add(token);
            for (String parte : token.split("[./-]+")) {
                if (!parte.isEmpty()) palabras.add(parte);
            }
        }
        return palabras;
    }

    private static boolean enCorpus(String token, Set<String> corpus) {
        String t = token.toLowerCase(Locale.ROOT);
        if (corpus.contains(t)) return true;
        String[] partes = t.split("[./-]+");
        if (partes.length < 2) return false;
        for (String p : partes) {
            if (!p.isEmpty() && !corpus.contains(p)) return false;
        }
        return true;
    }

    private static List<ValorExperiencia> ordenar(List<ValorExperiencia> base, List<String> ordenIa) {
        Map<String, ValorExperiencia> porId = new LinkedHashMap<>();
        for (ValorExperiencia x : base) porId.put(x.id(), x);
        List<ValorExperiencia> resultado = new ArrayList<>();
        Set<String> usados = new HashSet<>();
        for (String id : ordenIa) {
            ValorExperiencia x = porId.get(id);
            if (x != null && usados.add(id)) resultado.add(x);
        }
        for (ValorExperiencia x : base) {
            if (usados.add(x.id())) resultado.add(x);
        }
        return resultado;
    }

    private static List<String> ordenarHabilidades(List<String> base, List<String> propuestas) {
        Map<String, String> porClave = new LinkedHashMap<>();
        for (String h : base) porClave.putIfAbsent(h.toLowerCase(Locale.ROOT), h);
        Set<String> resultado = new LinkedHashSet<>();
        for (String h : propuestas == null ? List.<String>of() : propuestas) {
            if (h == null) continue;
            String original = porClave.get(h.strip().toLowerCase(Locale.ROOT));
            if (original != null) resultado.add(original);
        }
        resultado.addAll(base);
        return new ArrayList<>(resultado);
    }

    private static List<String> habilidades(String texto) {
        List<String> lista = new ArrayList<>();
        if (texto == null) return lista;
        for (String h : SEPARADOR_HABILIDADES.split(texto)) {
            if (!h.isBlank()) lista.add(h.strip());
        }
        return lista;
    }

    private static List<String> ids(List<ValorExperiencia> lista) {
        return lista.stream().map(ValorExperiencia::id).toList();
    }

    private static CambioAdaptacion cambio(List<CambioAdaptacion> previos, String tipo, String titulo, String detalle) {
        return new CambioAdaptacion("c" + (previos.size() + 1), tipo, titulo, detalle);
    }

    private static String recortar(String token) {
        int fin = token.length();
        while (fin > 0 && ".-/".indexOf(token.charAt(fin - 1)) >= 0) fin--;
        return token.substring(0, fin);
    }

    private static boolean tieneDigito(String s) {
        return s.chars().anyMatch(Character::isDigit);
    }

    /** Sigla: dos o más letras, todas en mayúscula (SQL, ATS, UNAULA). */
    private static boolean esSigla(String s) {
        long letras = s.chars().filter(Character::isLetter).count();
        return letras >= 2 && s.chars().filter(Character::isLetter).allMatch(Character::isUpperCase);
    }

    /** La palabra empieza una oración (o el texto): ahí la mayúscula no indica un nombre propio. */
    private static boolean abreOracion(String texto, int inicio) {
        int i = inicio - 1;
        while (i >= 0) {
            char c = texto.charAt(i);
            if (c == '\n') return true;
            if (!Character.isWhitespace(c) && "\"'«•*-–—(".indexOf(c) < 0) break;
            i--;
        }
        return i < 0 || ".!?:;".indexOf(texto.charAt(i)) >= 0;
    }

    private static boolean vacio(String s) {
        return s == null || s.isBlank();
    }
}
