package com.camellofacil.backend.reorganizaciones.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.camellofacil.backend.cv.app.ModelosHoja.ValorEducacion;
import com.camellofacil.backend.cv.app.ModelosHoja.ValorExperiencia;
import com.camellofacil.backend.cv.app.ModelosHoja.ValoresHoja;
import com.camellofacil.backend.reorganizaciones.app.ModelosReorganizacion.CambioPlan;
import com.camellofacil.backend.reorganizaciones.app.ModelosReorganizacion.RedaccionVerificada;
import com.camellofacil.backend.reorganizaciones.app.PlanReorganizacion.Aplicado;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** El plan de cambios: se arma una vez y se aplica o revierte sin llamar a la IA. */
class PlanReorganizacionTest {

    private static final ValoresHoja BASE = new ValoresHoja(
            "Ana Prueba", "300 000 0000", "Medellín", "Estudiante de datos enfocada en análisis.", "Python, SQL",
            List.of(
                    new ValorExperiencia("e1", "Monitora", "Universidad de Ejemplo", "2024-02", "2024-11", "Apoyé las prácticas del laboratorio."),
                    new ValorExperiencia("e2", "Practicante", "Empresa Ejemplo", "2025-06", "",
                            "Construí dashboards que redujeron 30% el tiempo de reporte. Presenté los resultados a la gerencia.")),
            List.of(
                    new ValorEducacion("d1", "Bachiller", "Colegio de Ejemplo", "2018-01", "2022-12"),
                    new ValorEducacion("d2", "Ingeniería de Sistemas", "Universidad de Ejemplo", "2023-01", "")));

    private static final RedaccionVerificada IA = new RedaccionVerificada(
            "Estudiante de datos con enfoque en análisis.",
            Map.of("e1", "Apoyé las prácticas del laboratorio.", "e2", "Reduje 30% el tiempo de reporte con dashboards. Presenté los resultados a la gerencia."));

    private static CambioPlan cambio(List<CambioPlan> plan, String id) {
        return plan.stream().filter(c -> c.id().equals(id)).findFirst().orElseThrow();
    }

    private static List<CambioPlan> con(List<CambioPlan> plan, String id, boolean aplicado) {
        return plan.stream()
                .map(c -> c.id().equals(id) ? new CambioPlan(c.id(), c.tipo(), c.titulo(), c.detalle(), c.seccionId(), aplicado, c.clave(), c.valor()) : c)
                .toList();
    }

    @Test
    void propone_orden_redaccion_y_vinetas_solo_donde_hay_algo_que_cambiar() {
        List<CambioPlan> plan = PlanReorganizacion.construir(BASE, "harvard", IA);

        assertThat(plan).extracting(CambioPlan::id).containsExactly(
                "orden-secciones", "orden-experiencia", "orden-educacion", "perfil", "exp-e2", "vin-e2");
        // e1 no cambia de redacción (la IA devolvió lo mismo) y su descripción tiene una sola frase.
        assertThat(plan).extracting(CambioPlan::tipo).containsExactly("movido", "movido", "movido", "redaccion", "redaccion", "separado");
        assertThat(plan).allMatch(CambioPlan::aplicado);
        assertThat(cambio(plan, "orden-experiencia").valor()).isEqualTo("e2,e1");
        assertThat(cambio(plan, "orden-educacion").valor()).isEqualTo("d2,d1");
    }

    @Test
    void enAtsNoSeReordenanLasSeccionesYSinIaNoHayRedaccion() {
        List<CambioPlan> plan = PlanReorganizacion.construir(BASE, "ats", RedaccionVerificada.vacia());

        assertThat(plan).extracting(CambioPlan::id).containsExactly("orden-experiencia", "orden-educacion", "vin-e2");
    }

    @Test
    void unaHojaYaOrdenadaYBienRedactadaNoTieneNadaQueProponer() {
        ValoresHoja ordenada = new ValoresHoja("Ana", "", "", "Perfil breve.", "Python",
                List.of(new ValorExperiencia("e1", "Monitora", "U", "2025-01", "", "Apoyé el laboratorio.")),
                List.of(new ValorEducacion("d1", "Ingeniería", "U", "2023-01", "")));

        assertThat(PlanReorganizacion.construir(ordenada, "ats", RedaccionVerificada.vacia())).isEmpty();
    }

    @Test
    void aplicarTodoDaLaHojaReorganizada_conViñetasYElOrdenNuevo() {
        List<CambioPlan> plan = PlanReorganizacion.construir(BASE, "harvard", IA);

        Aplicado a = PlanReorganizacion.aplicar(BASE, plan);

        assertThat(a.ordenSecciones()).isTrue();
        assertThat(a.valores().perfil()).isEqualTo("Estudiante de datos con enfoque en análisis.");
        assertThat(a.valores().experiencia()).extracting(ValorExperiencia::id).containsExactly("e2", "e1");
        assertThat(a.valores().experiencia().get(0).descripcion())
                .isEqualTo("• Reduje 30% el tiempo de reporte con dashboards.\n• Presenté los resultados a la gerencia.");
        assertThat(a.valores().educacion()).extracting(ValorEducacion::id).containsExactly("d2", "d1");
        // Lo que no es redacción ni orden no se toca jamás.
        assertThat(a.valores().experiencia().get(0).cargo()).isEqualTo("Practicante");
        assertThat(a.valores().experiencia().get(0).inicio()).isEqualTo("2025-06");
        assertThat(a.valores().nombre()).isEqualTo("Ana Prueba");
        assertThat(a.valores().habilidades()).isEqualTo("Python, SQL");
    }

    @Test
    void revertirTodoDejaLaHojaBaseIntacta() {
        List<CambioPlan> plan = PlanReorganizacion.construir(BASE, "harvard", IA);
        for (CambioPlan c : List.copyOf(plan)) plan = con(plan, c.id(), false);

        Aplicado a = PlanReorganizacion.aplicar(BASE, plan);

        assertThat(a.valores()).isEqualTo(BASE);
        assertThat(a.ordenSecciones()).isFalse();
    }

    @Test
    void cadaCambioSeRevierteSolo() {
        List<CambioPlan> plan = PlanReorganizacion.construir(BASE, "harvard", IA);

        Aplicado sinOrden = PlanReorganizacion.aplicar(BASE, con(plan, "orden-experiencia", false));
        assertThat(sinOrden.valores().experiencia()).extracting(ValorExperiencia::id).containsExactly("e1", "e2");
        assertThat(sinOrden.valores().perfil()).startsWith("Estudiante de datos con enfoque"); // lo demás sigue aplicado

        Aplicado sinVinetas = PlanReorganizacion.aplicar(BASE, con(plan, "vin-e2", false));
        assertThat(sinVinetas.valores().experiencia().get(0).descripcion()).doesNotContain("•").startsWith("Reduje 30%");

        Aplicado sinRedaccion = PlanReorganizacion.aplicar(BASE, con(con(plan, "exp-e2", false), "perfil", false));
        assertThat(sinRedaccion.valores().perfil()).isEqualTo(BASE.perfil());
        assertThat(sinRedaccion.valores().experiencia().get(0).descripcion()).startsWith("• Construí dashboards");
    }

    @Test
    void sinFechaDeInicioAlFinalYElOrdenEntreIgualesSeConserva() {
        ValoresHoja v = new ValoresHoja("A", "", "", "", "", List.of(
                new ValorExperiencia("a", "Sin fecha", "X", "", "", ""),
                new ValorExperiencia("b", "Reciente", "X", "2025-01", "", ""),
                new ValorExperiencia("c", "Igual", "X", "2025-01", "", "")), List.of());

        List<CambioPlan> plan = PlanReorganizacion.construir(v, "ats", RedaccionVerificada.vacia());

        assertThat(cambio(plan, "orden-experiencia").valor()).isEqualTo("b,c,a");
    }
}
