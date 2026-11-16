package com.camellofacil.backend.vacantes.fuentes;

import com.camellofacil.backend.vacantes.app.FuenteVacantes;
import com.camellofacil.backend.vacantes.app.ModelosVacante.VacanteExterna;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

/**
 * Jooble: agregador de ofertas. Fuente secundaria (JSearch es la principal). La llave va en la ruta de la
 * URL, así que nunca se registra la URL en los logs.
 *
 * <p>El país lo da el servidor: jooble.org es internacional (sobre todo EE. UU.) y co.jooble.org es Colombia.
 * Con el internacional se busca con ubicación «Remote» y solo se guardan las ofertas que dicen ser remotas
 * ({@code solo-remotas}), porque son las que alguien en Colombia puede aplicar.
 */
@Component
class JoobleFuente implements FuenteVacantes {

    static final String CODIGO = "jooble";

    private static final Pattern SEGMENTO_SEGURO = Pattern.compile("[A-Za-z0-9-]{8,100}");
    /** Ubicación o cargo que indica trabajo remoto. */
    private static final Pattern REMOTA = Pattern.compile(
            "\\b(remote|remoto|remota|teletrabajo|anywhere|work from home|home office|trabajo desde casa)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private final RestClient http;
    private final String apiKey;
    private final Cupo cupo;
    private final String ubicacion;
    private final boolean soloRemotas;

    JoobleFuente(
            @Value("${camellofacil.vacantes.jooble.api-key:}") String apiKey,
            @Value("${camellofacil.vacantes.jooble.url}") String url,
            @Value("${camellofacil.vacantes.jooble.limite}") int limite,
            @Value("${camellofacil.vacantes.jooble.dias-periodo}") int diasPeriodo,
            @Value("${camellofacil.vacantes.jooble.ubicacion:}") String ubicacion,
            @Value("${camellofacil.vacantes.jooble.solo-remotas:true}") boolean soloRemotas) {
        this.apiKey = apiKey;
        this.ubicacion = ubicacion == null ? "" : ubicacion.trim();
        this.soloRemotas = soloRemotas;
        this.cupo = new Cupo(limite, Duration.ofDays(diasPeriodo));
        this.http = ClienteHttp.crear(url);
    }

    @Override
    public String codigo() {
        return CODIGO;
    }

    /** Solo si la llave tiene la forma esperada (va dentro de la URL: no se acepta cualquier texto). */
    @Override
    public boolean configurada() {
        return apiKey != null && SEGMENTO_SEGURO.matcher(apiKey).matches();
    }

    @Override
    public Cupo cupo() {
        return cupo;
    }

    @Override
    public boolean soloRemotas() {
        return soloRemotas;
    }

    @Override
    public List<VacanteExterna> buscar(String consulta) throws FuenteException {
        try {
            JsonNode respuesta = http.post()
                    .uri("/{llave}", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ubicacion.isEmpty()
                            ? Map.of("keywords", consulta, "page", "1")
                            : Map.of("keywords", consulta, "location", ubicacion, "page", "1"))
                    .retrieve()
                    .body(JsonNode.class);
            if (respuesta == null) throw new FuenteException(FuenteException.Motivo.OTRO, "Jooble respondió vacío", null);
            return convertir(respuesta);
        } catch (RestClientException e) {
            throw ClienteHttp.error("Jooble", e);
        }
    }

    /** Traduce la respuesta de Jooble (jobs[]) a nuestro formato. Jooble solo da un extracto de la oferta. */
    static List<VacanteExterna> convertir(JsonNode respuesta) {
        List<VacanteExterna> vacantes = new ArrayList<>();
        for (JsonNode j : respuesta.path("jobs")) {
            vacantes.add(new VacanteExterna(
                    texto(j, "id"),
                    texto(j, "title"),
                    texto(j, "company"),
                    texto(j, "location"),
                    REMOTA.matcher(texto(j, "location") + " " + texto(j, "title")).find(),
                    texto(j, "type"),
                    texto(j, "source"),
                    texto(j, "link"),
                    texto(j, "snippet"),
                    List.of(),
                    List.of(),
                    fecha(texto(j, "updated"))));
        }
        return vacantes;
    }

    /** Jooble manda «2026-11-12T00:00:00.0000000» (sin zona): se toma como UTC. */
    static Instant fecha(String valor) {
        if (valor.isBlank()) return null;
        try {
            return LocalDateTime.parse(valor).toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String texto(JsonNode j, String campo) {
        JsonNode valor = j.path(campo);
        return valor.isNull() || valor.isMissingNode() ? "" : valor.asString("");
    }
}
