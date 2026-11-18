package com.camellofacil.backend.cv.app;

import com.camellofacil.backend.cv.app.ModelosAnalisis.Observacion;
import com.camellofacil.backend.cv.app.ModelosAnalisis.OpcionAts;
import com.camellofacil.backend.cv.app.ModelosAnalisis.Puntaje;
import com.camellofacil.backend.cv.app.ModelosAnalisis.PuntajeAts;
import com.camellofacil.backend.cv.app.ModelosHoja.ValorEducacion;
import com.camellofacil.backend.cv.app.ModelosHoja.ValorExperiencia;
import com.camellofacil.backend.cv.app.ModelosHoja.ValoresHoja;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Evalúa una hoja de vida con reglas que se pueden explicar, sin IA: el mismo contenido da siempre el mismo
 * resultado y cada punto sale de algo que se puede señalar en la hoja. Solo mira lo que la persona confirmó
 * (los datos extraídos), no el diseño del archivo que subió.
 *
 * <p>Cada criterio devuelve un cumplimiento de 0 a 1 y una observación (bien, mejorar o falta). El puntaje es el
 * promedio de los cumplimientos ponderado por el peso del criterio.
 */
public final class EvaluadorHoja {

    private EvaluadorHoja() {}

    /** Resultado de un criterio: cuánto cumple (0 a 1) y qué decirle a la persona. */
    record Criterio(String id, String titulo, double cumple, String bien, String mejorar, String falta) {

        Observacion observacion() {
            if (cumple >= 0.999) return new Observacion(id, "bien", titulo, bien, "reglas");
            if (cumple > 0) return new Observacion(id, "mejorar", titulo, mejorar, "reglas");
            return new Observacion(id, "falta", titulo, falta, "reglas");
        }
    }

    private static final Pattern FECHA = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");
    private static final Pattern CIFRA = Pattern.compile("\\d");
    private static final Pattern ORACION = Pattern.compile("(?<=[.!?])\\s+(?=\\p{Lu})");

    public static final List<OpcionAts> OPCIONES_ATS = List.of(
            new OpcionAts("general", "Criterios generales"),
            new OpcionAts("palabras-clave", "Énfasis en palabras clave"),
            new OpcionAts("estructura", "Énfasis en estructura y fechas"));

    // ---------- Harvard ----------

    private static final Map<String, Integer> PESOS_HARVARD = pesos(
            "h-contacto", 10, "h-perfil", 10, "h-educacion", 15, "h-experiencia", 20,
            "h-orden", 10, "h-logros", 15, "h-extension", 10, "h-habilidades", 10);

    public static Puntaje harvard(ValoresHoja v) {
        return puntaje(criteriosHarvard(v), PESOS_HARVARD, "Harvard");
    }

    static List<Criterio> criteriosHarvard(ValoresHoja v) {
        return List.of(contacto(v), perfil(v), educacion(v), experiencia(v), orden(v), logros(v), extension(v, 3200, 4200), habilidades(v, 5));
    }

    // ---------- ATS ----------

    private static final Map<String, Map<String, Integer>> PESOS_ATS = Map.of(
            "general", pesos("a-contacto", 15, "a-secciones", 15, "a-fechas", 15, "a-palabras", 15, "a-descripciones", 20, "a-texto", 10, "a-longitud", 10),
            "palabras-clave", pesos("a-contacto", 10, "a-secciones", 10, "a-fechas", 10, "a-palabras", 35, "a-descripciones", 25, "a-texto", 5, "a-longitud", 5),
            "estructura", pesos("a-contacto", 20, "a-secciones", 25, "a-fechas", 25, "a-palabras", 10, "a-descripciones", 10, "a-texto", 5, "a-longitud", 5));

    /** Vacío si la opción no existe. */
    public static Optional<PuntajeAts> ats(ValoresHoja v, String opcionId) {
        Map<String, Integer> pesos = PESOS_ATS.get(opcionId);
        if (pesos == null) return Optional.empty();
        Puntaje p = puntaje(criteriosAts(v), pesos, "ATS");
        return Optional.of(new PuntajeAts(p.valor(), p.nivel(), p.detalle(), opcionId));
    }

    static List<Criterio> criteriosAts(ValoresHoja v) {
        return List.of(atsContacto(v), atsSecciones(v), atsFechas(v), habilidadesAts(v), atsDescripciones(v), atsTextoSimple(v), extensionAts(v));
    }

    /** Las observaciones de Harvard y ATS juntas, sin repetir criterios con el mismo tema. */
    public static List<Observacion> observaciones(ValoresHoja v) {
        List<Observacion> lista = new ArrayList<>();
        criteriosHarvard(v).forEach(c -> lista.add(c.observacion()));
        // Contacto, perfil, educación, experiencia, habilidades y longitud ya salen en Harvard: de ATS solo lo propio.
        for (Criterio c : criteriosAts(v)) {
            if (List.of("a-secciones", "a-fechas", "a-descripciones", "a-texto").contains(c.id())) lista.add(c.observacion());
        }
        return lista;
    }

    // ---------- Puntaje ----------

    private static Puntaje puntaje(List<Criterio> criterios, Map<String, Integer> pesos, String formato) {
        double suma = 0;
        int total = 0;
        long cumplidos = 0;
        for (Criterio c : criterios) {
            int peso = pesos.getOrDefault(c.id(), 0);
            suma += peso * c.cumple();
            total += peso;
            if (c.cumple() >= 0.999 && peso > 0) cumplidos++;
        }
        int valor = total == 0 ? 0 : (int) Math.round(100 * suma / total);
        long evaluados = criterios.stream().filter(c -> pesos.getOrDefault(c.id(), 0) > 0).count();
        String detalle = "Cumples " + cumplidos + " de " + evaluados + " criterios de " + formato + "."
                + (cumplidos < evaluados ? " Revisa las observaciones para subir el puntaje." : " ¡Muy buen trabajo!");
        return new Puntaje(valor, nivel(valor), detalle);
    }

    static String nivel(int valor) {
        return valor >= 80 ? "alto" : valor >= 50 ? "medio" : "bajo";
    }

    // ---------- Criterios compartidos ----------

    private static Criterio contacto(ValoresHoja v) {
        int presentes = (hay(v.nombre()) ? 1 : 0) + (hay(v.telefono()) ? 1 : 0) + (hay(v.ciudad()) ? 1 : 0);
        return new Criterio("h-contacto", "Datos de contacto", presentes / 3.0,
                "Tienes nombre, teléfono y ciudad, además del correo de tu cuenta.",
                "Te falta alguno de estos datos: nombre, teléfono o ciudad. Quien te escribe necesita poder localizarte.",
                "No tiene nombre, teléfono ni ciudad. Agrégalos para que puedan contactarte.");
    }

    private static Criterio perfil(ValoresHoja v) {
        String p = v.perfil() == null ? "" : v.perfil().strip();
        if (p.isEmpty()) {
            return new Criterio("h-perfil", "Perfil profesional", 0, "", "",
                    "No tiene perfil. Escribe dos o tres frases: quién eres, qué estudias y qué buscas.");
        }
        int oraciones = ORACION.split(p).length;
        boolean breve = p.length() <= 600 && oraciones <= 4;
        return new Criterio("h-perfil", "Perfil profesional", breve ? 1 : 0.5,
                "Tu perfil es breve y directo.",
                "Tu perfil es largo (" + oraciones + " frases). Reduce a dos o tres frases con lo esencial.", "");
    }

    private static Criterio educacion(ValoresHoja v) {
        List<ValorEducacion> e = lista(v.educacion());
        if (e.isEmpty()) {
            return new Criterio("h-educacion", "Formación académica", 0, "", "",
                    "No hay estudios. En Harvard la formación va primero: agrega tu programa y tu institución.");
        }
        long completas = e.stream().filter(x -> hay(x.titulo()) && hay(x.institucion()) && hay(x.inicio())).count();
        return new Criterio("h-educacion", "Formación académica", (double) completas / e.size(),
                "Tus estudios tienen programa, institución y fecha de inicio.",
                "A algún estudio le falta el programa, la institución o la fecha de inicio.", "");
    }

    private static Criterio experiencia(ValoresHoja v) {
        List<ValorExperiencia> x = lista(v.experiencia());
        if (x.isEmpty()) {
            return new Criterio("h-experiencia", "Experiencia", 0, "", "",
                    "Aún no tiene experiencia. Si estás empezando, incluye monitorías, proyectos o voluntariados.");
        }
        long completas = x.stream().filter(e -> hay(e.cargo()) && hay(e.organizacion()) && hay(e.inicio())).count();
        return new Criterio("h-experiencia", "Experiencia", (double) completas / x.size(),
                "Cada experiencia tiene cargo, organización y fecha de inicio.",
                "A alguna experiencia le falta el cargo, la organización o la fecha de inicio.", "");
    }

    /** Lo más reciente primero, tanto en experiencia como en estudios. */
    private static Criterio orden(ValoresHoja v) {
        boolean ok = recienteAntes(lista(v.experiencia()).stream().map(ValorExperiencia::inicio).toList())
                && recienteAntes(lista(v.educacion()).stream().map(ValorEducacion::inicio).toList());
        return new Criterio("h-orden", "Orden cronológico inverso", ok ? 1 : 0.4,
                "Lo más reciente aparece primero.",
                "Pon primero lo más reciente: así se ve de inmediato lo que haces hoy.", "");
    }

    private static boolean recienteAntes(List<String> inicios) {
        String anterior = null;
        for (String i : inicios) {
            if (!hay(i)) continue;
            if (anterior != null && i.compareTo(anterior) > 0) return false;
            anterior = i;
        }
        return true;
    }

    private static Criterio logros(ValoresHoja v) {
        List<ValorExperiencia> x = lista(v.experiencia());
        if (x.isEmpty()) {
            return new Criterio("h-logros", "Logros con resultados", 0, "", "", "Sin experiencia no hay logros que mostrar todavía.");
        }
        long conDescripcion = x.stream().filter(e -> hay(e.descripcion())).count();
        long conCifras = x.stream().filter(e -> hay(e.descripcion()) && CIFRA.matcher(e.descripcion()).find()).count();
        double cumple = 0.5 * conDescripcion / x.size() + 0.5 * conCifras / x.size();
        return new Criterio("h-logros", "Logros con resultados", cumple,
                "Describes lo que hiciste y lo respaldas con cifras.",
                conDescripcion < x.size()
                        ? "Algunas experiencias no tienen descripción. Cuenta qué hiciste, con verbos de acción."
                        : "Tus descripciones no traen cifras. Si las tienes, agrégalas (usuarios, porcentajes, tiempos).",
                "Ninguna experiencia tiene descripción. Cuenta qué hiciste y qué lograste.");
    }

    private static Criterio extension(ValoresHoja v, int cabe, int limite) {
        int n = largo(v);
        double cumple = n <= cabe ? 1 : n <= limite ? 0.5 : 0.2;
        return new Criterio("h-extension", "Extensión", cumple,
                "Cabe en una página.",
                "Es larga (" + n + " caracteres): probablemente pase de una página. Resume lo menos importante.", "");
    }

    private static Criterio habilidades(ValoresHoja v, int minimo) {
        int n = habilidades(v.habilidades()).size();
        return new Criterio("h-habilidades", "Habilidades", Math.min(1.0, (double) n / minimo),
                "Listas " + n + " habilidades.",
                "Tienes " + n + " habilidades. Agrega las que de verdad dominas (herramientas, tecnologías, idiomas).",
                "No hay habilidades. Agrega herramientas, tecnologías e idiomas que dominas.");
    }

    // ---------- Criterios propios de ATS ----------

    private static Criterio atsContacto(ValoresHoja v) {
        return new Criterio("a-contacto", "Correo y teléfono", hay(v.telefono()) ? 1 : 0.5,
                "Tienes correo y teléfono.", "Falta el teléfono: muchos sistemas lo exigen.", "");
    }

    private static Criterio atsSecciones(ValoresHoja v) {
        int presentes = (hay(v.perfil()) ? 1 : 0) + (!lista(v.experiencia()).isEmpty() ? 1 : 0)
                + (!lista(v.educacion()).isEmpty() ? 1 : 0) + (!habilidades(v.habilidades()).isEmpty() ? 1 : 0);
        return new Criterio("a-secciones", "Secciones estándar", presentes / 4.0,
                "Tiene perfil, experiencia, educación y habilidades: los títulos que un ATS espera.",
                "Te falta alguna sección estándar (perfil, experiencia, educación o habilidades).",
                "No tiene ninguna sección estándar.");
    }

    private static Criterio atsFechas(ValoresHoja v) {
        List<String[]> fechas = new ArrayList<>();
        lista(v.experiencia()).forEach(e -> fechas.add(new String[] {e.inicio(), e.fin()}));
        lista(v.educacion()).forEach(e -> fechas.add(new String[] {e.inicio(), e.fin()}));
        if (fechas.isEmpty()) {
            return new Criterio("a-fechas", "Fechas", 0, "", "", "No hay experiencias ni estudios con fechas.");
        }
        long bien = fechas.stream().filter(f -> fechaValida(f[0]) && (!hay(f[1]) || fechaValida(f[1]))).count();
        return new Criterio("a-fechas", "Fechas completas", (double) bien / fechas.size(),
                "Todas las fechas tienen año y mes.",
                "Algunas fechas no tienen año y mes. Los ATS ordenan tu trayectoria con ellas.", "");
    }

    private static Criterio habilidadesAts(ValoresHoja v) {
        int n = habilidades(v.habilidades()).size();
        return new Criterio("a-palabras", "Palabras clave", Math.min(1.0, n / 8.0),
                "Tienes suficientes habilidades para que te encuentren por palabras clave.",
                "Tienes " + n + " habilidades. Con unas 8 o más aumentas las coincidencias con las vacantes.",
                "No hay habilidades: es lo primero que un ATS busca.");
    }

    private static Criterio atsDescripciones(ValoresHoja v) {
        List<ValorExperiencia> x = lista(v.experiencia());
        if (x.isEmpty()) return new Criterio("a-descripciones", "Descripciones", 0, "", "", "Sin experiencia no hay descripciones que leer.");
        long buenas = x.stream().filter(e -> hay(e.descripcion()) && e.descripcion().strip().length() >= 60).count();
        return new Criterio("a-descripciones", "Descripciones con contenido", (double) buenas / x.size(),
                "Cada experiencia tiene una descripción con contenido.",
                "Alguna experiencia tiene una descripción muy corta o vacía. Explica qué hiciste y con qué herramientas.", "");
    }

    /** Sin emojis ni símbolos raros: un ATS puede leerlos mal y el PDF no los dibuja. */
    private static Criterio atsTextoSimple(ValoresHoja v) {
        boolean limpio = todoElTexto(v).codePoints().allMatch(cp -> cp <= 0xFF || "–—‘’“”…•€".indexOf(cp) >= 0);
        return new Criterio("a-texto", "Texto simple", limpio ? 1 : 0.5,
                "El texto no trae símbolos que un ATS lea mal.",
                "Hay símbolos poco comunes (emojis, flechas…). Reemplázalos por texto simple.", "");
    }

    private static Criterio extensionAts(ValoresHoja v) {
        int n = largo(v);
        return new Criterio("a-longitud", "Longitud", n <= 4500 ? 1 : n <= 6500 ? 0.5 : 0.2,
                "Tiene una longitud razonable.", "Es muy larga (" + n + " caracteres): resume.", "");
    }

    // ---------- Utilidades ----------

    private static int largo(ValoresHoja v) {
        return todoElTexto(v).length();
    }

    private static String todoElTexto(ValoresHoja v) {
        StringBuilder s = new StringBuilder();
        for (String t : new String[] {v.nombre(), v.perfil(), v.habilidades()}) {
            if (t != null) s.append(t).append('\n');
        }
        lista(v.experiencia()).forEach(e -> s.append(nz(e.cargo())).append(nz(e.organizacion())).append(nz(e.descripcion())));
        lista(v.educacion()).forEach(e -> s.append(nz(e.titulo())).append(nz(e.institucion())));
        return s.toString();
    }

    static List<String> habilidades(String texto) {
        List<String> lista = new ArrayList<>();
        if (texto == null) return lista;
        for (String h : texto.split("[,;\\n]")) {
            if (!h.isBlank()) lista.add(h.strip());
        }
        return lista;
    }

    private static boolean fechaValida(String f) {
        return hay(f) && FECHA.matcher(f.strip()).matches();
    }

    private static boolean hay(String s) {
        return s != null && !s.isBlank();
    }

    private static String nz(String s) {
        return s == null ? "" : s + "\n";
    }

    private static <T> List<T> lista(List<T> l) {
        return l == null ? List.of() : l;
    }

    private static Map<String, Integer> pesos(Object... pares) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (int i = 0; i < pares.length; i += 2) m.put((String) pares[i], (Integer) pares[i + 1]);
        return m;
    }
}
