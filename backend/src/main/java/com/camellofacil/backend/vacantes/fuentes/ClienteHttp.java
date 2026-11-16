package com.camellofacil.backend.vacantes.fuentes;

import com.camellofacil.backend.vacantes.app.FuenteVacantes.FuenteException;
import com.camellofacil.backend.vacantes.app.FuenteVacantes.FuenteException.Motivo;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Lo común de los adaptadores HTTP: tiempos de espera y traducción de errores a {@link FuenteException}. */
final class ClienteHttp {

    private ClienteHttp() {}

    static RestClient crear(String urlBase) {
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
        fabrica.setReadTimeout(Duration.ofSeconds(40));
        return RestClient.builder().baseUrl(urlBase).requestFactory(fabrica).build();
    }

    /** 401/403: llave inválida o sin permiso; 429: límite del proveedor; lo demás, un fallo cualquiera. */
    static FuenteException error(String fuente, RestClientException e) {
        if (e instanceof RestClientResponseException r) {
            int codigo = r.getStatusCode().value();
            Motivo motivo = codigo == 401 || codigo == 403 ? Motivo.AUTORIZACION : codigo == 429 ? Motivo.LIMITE : Motivo.OTRO;
            return new FuenteException(motivo, fuente + " respondió " + codigo, e);
        }
        return new FuenteException(Motivo.OTRO, fuente + ": " + e.getClass().getSimpleName(), e);
    }
}
