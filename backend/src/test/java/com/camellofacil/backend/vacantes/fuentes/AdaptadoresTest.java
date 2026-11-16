package com.camellofacil.backend.vacantes.fuentes;

import static org.assertj.core.api.Assertions.assertThat;

import com.camellofacil.backend.vacantes.app.ModelosVacante.VacanteExterna;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Respuestas con la forma real de cada proveedor, pero con datos inventados. */
class AdaptadoresTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Instant AHORA = Instant.parse("2026-11-16T12:00:00Z");

    @Test
    void jsearchSeTraduceANuestroFormato() {
        JsonNode respuesta = JSON.readTree("""
                {"status":"OK","request_id":"x","parameters":{},"data":{"cursor":null,"jobs":[
                  {"job_id":"abc==","job_title":"PRACTICANTE EN INGENIERÍA DE SISTEMAS","employer_name":"Empresa Ficticia",
                   "job_publisher":"Portal Ficticio","job_employment_type":"Pasantía","job_employment_types":["INTERN"],
                   "job_apply_link":"https://portal.example/oferta/1","job_description":"Apoyar al equipo.",
                   "job_is_remote":false,"job_posted_at":"hace 6 días","job_posted_at_timestamp":null,
                   "job_location":"Bogotá     ·  a través de Portal Ficticio","job_city":null,
                   "job_highlights":{"Qualifications":["Estudiante de 8 semestre"],"Responsibilities":["Soporte"]}},
                  {"job_id":"def==","job_title":"Analista","employer_name":"Otra Empresa","job_publisher":"P",
                   "job_apply_link":"https://portal.example/oferta/2","job_is_remote":true,
                   "job_posted_at_timestamp":1763208000,"job_city":"Medellín","job_highlights":{}}
                ]}}""");

        List<VacanteExterna> vacantes = JSearchFuente.convertir(respuesta, AHORA);

        assertThat(vacantes).hasSize(2);
        VacanteExterna a = vacantes.get(0);
        assertThat(a.idExterno()).isEqualTo("abc==");
        assertThat(a.ciudad()).isEqualTo("Bogotá");
        assertThat(a.tipoEmpleo()).isEqualTo("Pasantía INTERN");
        assertThat(a.publicadaEn()).isEqualTo(AHORA.minus(Duration.ofDays(6)));
        assertThat(a.requisitos()).containsExactly("Estudiante de 8 semestre");
        assertThat(a.responsabilidades()).containsExactly("Soporte");

        VacanteExterna b = vacantes.get(1);
        assertThat(b.ciudad()).isEqualTo("Medellín");
        assertThat(b.remota()).isTrue();
        assertThat(b.publicadaEn()).isEqualTo(Instant.ofEpochSecond(1763208000));
        assertThat(b.requisitos()).isEmpty();
    }

    @Test
    void jsearchEntiendeFechasRelativasEnEspanolEIngles() {
        assertThat(JSearchFuente.publicada(JSON.readTree("{\"job_posted_at\":\"hace 3 horas\"}"), AHORA))
                .isEqualTo(AHORA.minus(Duration.ofHours(3)));
        assertThat(JSearchFuente.publicada(JSON.readTree("{\"job_posted_at\":\"hace 30+ días\"}"), AHORA))
                .isEqualTo(AHORA.minus(Duration.ofDays(30)));
        assertThat(JSearchFuente.publicada(JSON.readTree("{\"job_posted_at\":\"2 weeks ago\"}"), AHORA))
                .isEqualTo(AHORA.minus(Duration.ofDays(14)));
        assertThat(JSearchFuente.publicada(JSON.readTree("{\"job_posted_at\":\"ayer\"}"), AHORA)).isNull();
    }

    @Test
    void joobleSeTraduceANuestroFormato() {
        JsonNode respuesta = JSON.readTree("""
                {"totalCount":1,"jobs":[{"title":"Practicante contable","location":"Medellín, Antioquia",
                  "snippet":"Apoyo&nbsp;en <b>conciliaciones</b>","salary":"","source":"portal.example","type":"Prácticas",
                  "link":"https://jooble.org/jdp/123","company":"Empresa Ficticia","updated":"2026-11-12T00:00:00.0000000",
                  "id":-5971358319510862156}]}""");

        List<VacanteExterna> vacantes = JoobleFuente.convertir(respuesta);

        assertThat(vacantes).hasSize(1);
        VacanteExterna v = vacantes.get(0);
        assertThat(v.idExterno()).isEqualTo("-5971358319510862156");
        assertThat(v.ciudad()).isEqualTo("Medellín, Antioquia");
        assertThat(v.publicadaEn()).isEqualTo(Instant.parse("2026-11-12T00:00:00Z"));
        assertThat(v.url()).isEqualTo("https://jooble.org/jdp/123");
        assertThat(v.remota()).isFalse();
    }

    @Test
    void joobleReconoceLasRemotasPorLaUbicacionOElCargo() {
        JsonNode respuesta = JSON.readTree("""
                {"jobs":[
                  {"id":1,"title":"Software Engineering Intern","location":"Remote","company":"A","link":"https://jooble.org/jdp/1"},
                  {"id":2,"title":"Coordenador de Sistemas | Remoto","location":"New York, NY","company":"B","link":"https://jooble.org/jdp/2"},
                  {"id":3,"title":"Software Engineer Intern","location":"Torrance, CA","company":"C","link":"https://jooble.org/jdp/3"}
                ]}""");
        assertThat(JoobleFuente.convertir(respuesta)).extracting(VacanteExterna::remota).containsExactly(true, true, false);
    }
}
