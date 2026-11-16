package com.camellofacil.backend.vacantes.fuentes;

import com.camellofacil.backend.vacantes.app.FuenteVacantes;
import com.camellofacil.backend.vacantes.app.ModelosVacante.VacanteExterna;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

/**
 * JSearch (OpenWeb Ninja): ofertas de Google for Jobs, que reúne LinkedIn, Computrabajo, El Empleo, Magneto,
 * etc. Es la fuente principal. Cada petición trae una página (hasta 10 ofertas) de Colombia publicadas en la
 * última semana y gasta una unidad del cupo mensual.
 */
@Component
class JSearchFuente implements FuenteVacantes {

    static final String CODIGO = "jsearch";

    private static final Pattern HACE = Pattern.compile(
            "(\\d+)\\+?\\s*(minuto|minute|hora|hour|dia|día|day|semana|week|mes|month)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SEPARADOR_UBICACION = Pattern.compile("\\s*(?:[·•|]|\\ba través de\\b|\\bvia\\b).*$");

    private final RestClient http;
    private final String apiKey;
    private final Cupo cupo;
    private final Clock reloj;

    JSearchFuente(
            @Value("${camellofacil.vacantes.jsearch.api-key:}") String apiKey,
            @Value("${camellofacil.vacantes.jsearch.url}") String url,
            @Value("${camellofacil.vacantes.jsearch.limite}") int limite,
            @Value("${camellofacil.vacantes.jsearch.dias-periodo}") int diasPeriodo,
            Clock reloj) {
        this.apiKey = apiKey;
        this.cupo = new Cupo(limite, Duration.ofDays(diasPeriodo));
        this.http = ClienteHttp.crear(url);
        this.reloj = reloj;
    }

    @Override
    public String codigo() {
        return CODIGO;
    }

    @Override
    public boolean configurada() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public Cupo cupo() {
        return cupo;
    }

    @Override
    public List<VacanteExterna> buscar(String consulta) throws FuenteException {
        try {
            JsonNode respuesta = http.get()
                    .uri(u -> u.path("/search-v2")
                            .queryParam("query", consulta)
                            .queryParam("country", "co")
                            .queryParam("language", "es")
                            .queryParam("date_posted", "week")
                            .queryParam("num_pages", 1)
                            .build())
                    .header("X-API-Key", apiKey)
                    .retrieve()
                    .body(JsonNode.class);
            if (respuesta == null || !"OK".equals(respuesta.path("status").asString(""))) {
                throw new FuenteException(FuenteException.Motivo.OTRO, "JSearch respondió sin estado OK", null);
            }
            return convertir(respuesta, reloj.instant());
        } catch (RestClientException e) {
            throw ClienteHttp.error("JSearch", e);
        }
    }

    /** Traduce la respuesta de JSearch (data.jobs[]) a nuestro formato. */
    static List<VacanteExterna> convertir(JsonNode respuesta, Instant ahora) {
        JsonNode trabajos = respuesta.path("data").path("jobs");
        List<VacanteExterna> vacantes = new ArrayList<>();
        for (JsonNode j : trabajos) {
            JsonNode resaltados = j.path("job_highlights");
            vacantes.add(new VacanteExterna(
                    texto(j, "job_id"),
                    texto(j, "job_title"),
                    texto(j, "employer_name"),
                    ciudad(j),
                    j.path("job_is_remote").asBoolean(false),
                    (texto(j, "job_employment_type") + " " + String.join(" ", textos(j.path("job_employment_types")))).trim(),
                    texto(j, "job_publisher"),
                    texto(j, "job_apply_link"),
                    texto(j, "job_description"),
                    textos(resaltados.path("Responsibilities")),
                    textos(resaltados.path("Qualifications")),
                    publicada(j, ahora)));
        }
        return vacantes;
    }

    /** job_city casi nunca viene en Colombia; job_location trae «Bogotá · a través de El Empleo». */
    static String ciudad(JsonNode j) {
        String ciudad = texto(j, "job_city");
        if (!ciudad.isBlank()) return ciudad;
        String ubicacion = SEPARADOR_UBICACION.matcher(texto(j, "job_location")).replaceAll("").trim();
        return ubicacion.isBlank() ? null : ubicacion;
    }

    /** Con idioma español JSearch no da la marca de tiempo, solo «hace 6 días»: se calcula a partir de eso. */
    static Instant publicada(JsonNode j, Instant ahora) {
        long segundos = j.path("job_posted_at_timestamp").asLong(0);
        if (segundos > 0) return Instant.ofEpochSecond(segundos);
        Matcher m = HACE.matcher(texto(j, "job_posted_at"));
        if (!m.find()) return null;
        long n = Long.parseLong(m.group(1));
        String unidad = m.group(2).toLowerCase(Locale.ROOT);
        ChronoUnit u = switch (unidad) {
            case "minuto", "minute" -> ChronoUnit.MINUTES;
            case "hora", "hour" -> ChronoUnit.HOURS;
            default -> ChronoUnit.DAYS;
        };
        long cantidad = switch (unidad) {
            case "semana", "week" -> n * 7;
            case "mes", "month" -> n * 30;
            default -> n;
        };
        return ahora.minus(cantidad, u);
    }

    private static String texto(JsonNode j, String campo) {
        JsonNode valor = j.path(campo);
        return valor.isNull() || valor.isMissingNode() ? "" : valor.asString("");
    }

    private static List<String> textos(JsonNode arreglo) {
        List<String> textos = new ArrayList<>();
        if (arreglo.isArray()) arreglo.forEach(t -> textos.add(t.asString("")));
        return textos;
    }
}
