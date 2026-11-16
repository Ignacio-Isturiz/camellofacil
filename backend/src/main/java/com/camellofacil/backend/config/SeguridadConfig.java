package com.camellofacil.backend.config;

import com.camellofacil.backend.auth.app.ServicioOidcMicrosoft;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class SeguridadConfig {

    private static final Logger LOG = LoggerFactory.getLogger(SeguridadConfig.class);

    @Bean
    SecurityFilterChain filtros(
            HttpSecurity http,
            ServicioOidcMicrosoft servicioMicrosoft,
            ClientRegistrationRepository registros,
            @Value("${camellofacil.frontend-url}") String frontendUrl) throws Exception {
        http
            .cors(Customizer.withDefaults())
            // CSRF activo. Angular lee la cookie XSRF-TOKEN y la reenvía en X-XSRF-TOKEN.
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .addFilterAfter(new CookieCsrfFilter(), CsrfFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/v1/conexion", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated())
            // Sin sesión, la API responde 401 (no redirige a Microsoft): el frontend decide qué mostrar.
            .exceptionHandling(e -> e.defaultAuthenticationEntryPointFor(
                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                PathPatternRequestMatcher.withDefaults().matcher("/api/**")))
            .oauth2Login(oauth -> oauth
                .authorizationEndpoint(ep -> ep.authorizationRequestResolver(resolverConPkce(registros)))
                .userInfoEndpoint(info -> info.oidcUserService(servicioMicrosoft))
                .successHandler(volverAlFrontend(frontendUrl))
                .failureHandler((req, res, error) -> {
                    registrarFallo(error);
                    res.sendRedirect(frontendUrl + "/login?error=" + codigoPublico(error));
                }))
            .logout(logout -> logout
                .logoutUrl("/api/v1/auth/logout")
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler()));
        return http.build();
    }

    /**
     * PKCE además del secreto: si alguien interceptara el código de autorización, no le serviría.
     * prompt=login: Microsoft pide de nuevo las credenciales en cada ingreso (también tras vencer la sesión
     * por inactividad), en lugar de reutilizar en silencio la sesión que tenga abierta en el navegador.
     */
    private static OAuth2AuthorizationRequestResolver resolverConPkce(ClientRegistrationRepository registros) {
        DefaultOAuth2AuthorizationRequestResolver resolver =
            new DefaultOAuth2AuthorizationRequestResolver(registros, "/oauth2/authorization");
        resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce()
            .andThen(solicitud -> solicitud.additionalParameters(params -> params.put("prompt", "login"))));
        return resolver;
    }

    private static SimpleUrlAuthenticationSuccessHandler volverAlFrontend(String frontendUrl) {
        SimpleUrlAuthenticationSuccessHandler manejador = new SimpleUrlAuthenticationSuccessHandler(frontendUrl + "/inicio");
        manejador.setAlwaysUseDefaultTargetUrl(true);
        return manejador;
    }

    /** Deja en el log el código del error (p. ej. AADSTS7000215), sin datos de la persona. */
    private static void registrarFallo(Exception error) {
        if (error instanceof OAuth2AuthenticationException oauth) {
            LOG.warn("Ingreso con Microsoft rechazado: {} - {}", oauth.getError().getErrorCode(), oauth.getError().getDescription());
        } else {
            LOG.warn("Ingreso con Microsoft rechazado: {}", error.getClass().getSimpleName());
        }
    }

    /** Al frontend solo llega un código corto, nunca el detalle del error del proveedor. */
    private static String codigoPublico(Exception error) {
        if (error instanceof OAuth2AuthenticationException oauth
            && ServicioOidcMicrosoft.ERROR_DOMINIO.equals(oauth.getError().getErrorCode())) {
            return "dominio";
        }
        return "microsoft";
    }

    /** CORS solo para el dominio del frontend, configurado por variable de entorno. */
    @Bean
    CorsConfigurationSource cors(@Value("${camellofacil.frontend-url}") String frontendUrl) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(frontendUrl));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
        config.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", config);
        return fuente;
    }

    /**
     * Spring entrega el token CSRF de forma perezosa: sin leerlo, la cookie XSRF-TOKEN nunca se envía y
     * Angular no tendría qué reenviar en el cierre de sesión (POST).
     */
    private static final class CookieCsrfFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain cadena)
            throws ServletException, IOException {
            CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (token != null) {
                token.getToken();
            }
            cadena.doFilter(request, response);
        }
    }
}
