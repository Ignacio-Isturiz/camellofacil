package com.camellofacil.backend.cv.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.camellofacil.backend.cv.app.ModelosAnalisis.RecomendacionIa;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Lo que dice la IA es solo texto para la persona: se limpia antes de guardarlo. */
class AnalisisFormatoServiceTest {

    @Test
    void sinEnlacesSinVaciosConLargoLimitadoYMaximoCinco() {
        List<RecomendacionIa> crudas = new ArrayList<>();
        crudas.add(new RecomendacionIa("Agrega cifras", "Si las tienes, suma porcentajes o tiempos a tus logros."));
        crudas.add(new RecomendacionIa("Visita este sitio", "Entra a https://malicioso.example para mejorar tu hoja"));
        crudas.add(new RecomendacionIa("Otro", "Mira www.malicioso.example/oferta"));
        crudas.add(new RecomendacionIa("  ", "sin título"));
        crudas.add(new RecomendacionIa("Sin detalle", ""));
        crudas.add(null);
        crudas.add(new RecomendacionIa("T".repeat(300), "D".repeat(900)));
        for (int i = 0; i < 8; i++) crudas.add(new RecomendacionIa("Extra " + i, "Detalle " + i));

        List<RecomendacionIa> limpias = AnalisisFormatoService.limpiar(crudas);

        assertThat(limpias).hasSize(5);
        assertThat(limpias.get(0).titulo()).isEqualTo("Agrega cifras");
        assertThat(limpias).allSatisfy(r -> {
            assertThat(r.titulo()).isNotBlank().hasSizeLessThanOrEqualTo(80);
            assertThat(r.detalle()).isNotBlank().hasSizeLessThanOrEqualTo(240);
            assertThat(r.titulo() + r.detalle()).doesNotContain("http").doesNotContain("www.");
        });
    }

    @Test
    void quitaCaracteresDeControl() {
        List<RecomendacionIa> limpias = AnalisisFormatoService.limpiar(List.of(new RecomendacionIa("Título\u0000\n raro", "Detalle\tcon\u0007 control")));
        assertThat(limpias.get(0).titulo()).isEqualTo("Título raro");
        assertThat(limpias.get(0).detalle()).isEqualTo("Detalle con control");
    }
}
