package com.camellofacil.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

/**
 * Registro de Microsoft Entra ID para un solo inquilino (el de UNAULA). Se arma en código, con las rutas
 * del inquilino, para no depender de internet al arrancar y para que el emisor del token sí se valide.
 */
@Configuration
public class MicrosoftOAuthConfig {

    @Bean
    ClientRegistrationRepository registros(
            @Value("${camellofacil.microsoft.client-id}") String clientId,
            @Value("${camellofacil.microsoft.client-secret}") String clientSecret,
            @Value("${camellofacil.microsoft.tenant-id}") String tenantId,
            @Value("${camellofacil.backend-url}") String backendUrl) {
        String base = "https://login.microsoftonline.com/" + tenantId;
        ClientRegistration microsoft = ClientRegistration.withRegistrationId("microsoft")
            .clientName("Microsoft")
            .clientId(clientId)
            .clientSecret(clientSecret)
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri(backendUrl + "/login/oauth2/code/microsoft")
            .scope("openid", "profile", "email")
            .authorizationUri(base + "/oauth2/v2.0/authorize")
            .tokenUri(base + "/oauth2/v2.0/token")
            .jwkSetUri(base + "/discovery/v2.0/keys")
            .issuerUri(base + "/v2.0")
            .userNameAttributeName("sub")
            .build();
        return new InMemoryClientRegistrationRepository(microsoft);
    }
}
