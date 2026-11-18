package com.camellofacil.backend.reorganizaciones.app;

import com.camellofacil.backend.cv.app.ModelosHoja.ValorEducacion;
import com.camellofacil.backend.cv.app.ModelosHoja.ValorExperiencia;
import com.camellofacil.backend.cv.app.ModelosHoja.ValoresHoja;
import com.camellofacil.backend.plantillas.app.ExportadorDocumento;
import com.camellofacil.backend.reorganizaciones.app.ModelosReorganizacion.CambioPlan;
import com.camellofacil.backend.reorganizaciones.app.ModelosReorganizacion.RedaccionVerificada;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * El plan de cambios de una reorganización y cómo se aplica. Es lógica pura: dada la hoja base y el plan (con lo que
 * la persona aplicó o revirtió), siempre da el mismo resultado, sin llamar a la IA. Los cambios solo mueven, separan
 * o reescriben (con redacción ya verificada) lo que está en la hoja.
 */
final class PlanReorganizacion {

    private PlanReorganizacion() {}

    static final String ORDEN_SECCIONES = "orden-secciones";
    static final String ORDEN_EXPERIENCIA = "orden-experiencia";
    static final String ORDEN_EDUCACION = "orden-educacion";
    static final String PERFIL = "perfil";
    private static final String PREFIJO_DESCRIPCION = "descripcion:";
    private static final String PREFIJO_VINETAS = "vinetas:";

    /** La hoja con el plan aplicado y si las secciones van en el orden del formato elegido. */
    record Aplicado(ValoresHoja valores, boolean ordenSecciones) {}

    static List<CambioPlan> construir(ValoresHoja base, String formato, RedaccionVerificada ia) {
        List<CambioPlan> cambios = new ArrayList<>();
        List<ValorExperiencia> experiencias = lista(base.experiencia());
        List<ValorEducacion> estudios = lista(base.educacion());

        // Movidos
        if ("harvard".equals(formato)) {
            cambios.add(new CambioPlan(ORDEN_SECCIONES, "movido", "Secciones en el orden Harvard",
                    "La formación académica va primero, luego la experiencia y las habilidades.", null, true, ORDEN_SECCIONES, ""));
        }
        List<String> idsExperiencia = ids(experiencias.stream().sorted(masRecientePrimero(ValorExperiencia::inicio)).map(ValorExperiencia::id));
        if (!idsExperiencia.equals(ids(experiencias.stream().map(ValorExperiencia::id)))) {
            cambios.add(new CambioPlan(ORDEN_EXPERIENCIA, "movido", "Experiencia de la más reciente a la más antigua",
                    "Lo que haces hoy se ve primero.", "experiencia", true, ORDEN_EXPERIENCIA, String.join(",", idsExperiencia)));
        }
        List<String> idsEstudios = ids(estudios.stream().sorted(masRecientePrimero(ValorEducacion::inicio)).map(ValorEducacion::id));
        if (!idsEstudios.equals(ids(estudios.stream().map(ValorEducacion::id)))) {
            cambios.add(new CambioPlan(ORDEN_EDUCACION, "movido", "Estudios del más reciente al más antiguo",
                    "Tu formación actual se ve primero.", "educacion", true, ORDEN_EDUCACION, String.join(",", idsEstudios)));
        }

        // Redacción (ya verificada contra la hoja base)
        if (hay(base.perfil()) && hay(ia.perfil()) && !ia.perfil().strip().equals(base.perfil().strip())) {
            cambios.add(new CambioPlan(PERFIL, "redaccion", "Perfil con redacción más directa",
                    "Los mismos hechos, dichos de forma más concisa y profesional.", "perfil", true, PERFIL, ia.perfil().strip()));
        }
        for (ValorExperiencia e : experiencias) {
            String nueva = ia.descripciones().get(e.id());
            if (hay(e.descripcion()) && hay(nueva) && !nueva.strip().equals(e.descripcion().strip())) {
                cambios.add(new CambioPlan("exp-" + e.id(), "redaccion", "Descripción de «" + e.cargo() + "» mejor redactada",
                        "Verbos de acción y frases más cortas; los datos son los mismos.", "experiencia", true,
                        PREFIJO_DESCRIPCION + e.id(), nueva.strip()));
            }
        }

        // Separados: cada logro en su línea
        for (ValorExperiencia e : experiencias) {
            if (hay(e.descripcion()) && !e.descripcion().contains("\n") && ExportadorDocumento.dividirEnVinetas(e.descripcion()).size() >= 2) {
                cambios.add(new CambioPlan("vin-" + e.id(), "separado", "Logros de «" + e.cargo() + "» en viñetas",
                        "Separamos cada logro en su propia línea para leerlo más rápido.", "experiencia", true, PREFIJO_VINETAS + e.id(), ""));
            }
        }
        return cambios;
    }

    static Aplicado aplicar(ValoresHoja base, List<CambioPlan> plan) {
        Map<String, CambioPlan> aplicados = new LinkedHashMap<>();
        for (CambioPlan c : plan) {
            if (c.aplicado()) aplicados.put(c.clave(), c);
        }

        String perfil = aplicados.containsKey(PERFIL) ? aplicados.get(PERFIL).valor() : base.perfil();

        List<ValorExperiencia> experiencias = new ArrayList<>();
        for (ValorExperiencia e : lista(base.experiencia())) {
            String descripcion = e.descripcion();
            CambioPlan redaccion = aplicados.get(PREFIJO_DESCRIPCION + e.id());
            if (redaccion != null) descripcion = redaccion.valor();
            if (aplicados.containsKey(PREFIJO_VINETAS + e.id())) {
                List<String> vinetas = ExportadorDocumento.dividirEnVinetas(descripcion);
                if (vinetas.size() >= 2) descripcion = vinetas.stream().map(v -> "• " + v).collect(Collectors.joining("\n"));
            }
            experiencias.add(new ValorExperiencia(e.id(), e.cargo(), e.organizacion(), e.inicio(), e.fin(), descripcion));
        }
        CambioPlan ordenExperiencia = aplicados.get(ORDEN_EXPERIENCIA);
        if (ordenExperiencia != null) experiencias = ordenar(experiencias, ordenExperiencia.valor(), ValorExperiencia::id);

        List<ValorEducacion> estudios = new ArrayList<>(lista(base.educacion()));
        CambioPlan ordenEducacion = aplicados.get(ORDEN_EDUCACION);
        if (ordenEducacion != null) estudios = ordenar(estudios, ordenEducacion.valor(), ValorEducacion::id);

        ValoresHoja valores = new ValoresHoja(
                base.nombre(), base.telefono(), base.ciudad(), perfil, base.habilidades(), experiencias, estudios);
        return new Aplicado(valores, aplicados.containsKey(ORDEN_SECCIONES));
    }

    /** Pone los elementos en el orden de los ids dados; los que no estén en la lista van al final, en su orden. */
    private static <T> List<T> ordenar(List<T> elementos, String idsEnOrden, java.util.function.Function<T, String> id) {
        Map<String, T> porId = new LinkedHashMap<>();
        for (T t : elementos) porId.put(id.apply(t), t);
        List<T> resultado = new ArrayList<>();
        for (String i : idsEnOrden.split(",")) {
            T t = porId.remove(i);
            if (t != null) resultado.add(t);
        }
        resultado.addAll(porId.values());
        return resultado;
    }

    /** Lo más reciente primero (por mes de inicio); sin fecha, al final. El orden entre iguales se conserva. */
    private static <T> Comparator<T> masRecientePrimero(java.util.function.Function<T, String> inicio) {
        return Comparator.comparing((T t) -> hay(inicio.apply(t)) ? inicio.apply(t).strip() : "", Comparator.reverseOrder());
    }

    private static List<String> ids(java.util.stream.Stream<String> flujo) {
        return flujo.toList();
    }

    private static boolean hay(String s) {
        return s != null && !s.isBlank();
    }

    private static <T> List<T> lista(List<T> l) {
        return l == null ? List.of() : l;
    }
}
