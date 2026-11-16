package com.camellofacil.backend.config;

import com.camellofacil.backend.auth.app.SesionActual;
import com.camellofacil.backend.perfil.app.PerfilService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Mientras la persona no complete el modal del primer ingreso (perfil y consentimientos), la API solo le
 * responde lo necesario para completarlo. Así la regla no depende de que el frontend muestre el modal.
 * Se registra dentro de la cadena de seguridad (no es un bean, para que no se aplique dos veces).
 */
class PerfilCompletoFilter extends OncePerRequestFilter {

    /** Rutas permitidas sin perfil completo: sesión, perfil, consentimientos y la prueba de conexión. */
    private static final List<String> PERMITIDAS = List.of(
            "/api/v1/auth/", "/api/v1/perfil", "/api/v1/legal/", "/api/v1/conexion");

    private static final String CUERPO = """
            {"type":"about:blank","title":"Perfil incompleto","status":403,\
            "detail":"Completa tu perfil para continuar.","codigo":"perfil_incompleto"}""";

    private final SesionActual sesion;
    private final PerfilService perfiles;

    PerfilCompletoFilter(SesionActual sesion, PerfilService perfiles) {
        this.sesion = sesion;
        this.perfiles = perfiles;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String ruta = request.getRequestURI();
        return !ruta.startsWith("/api/v1/") || PERMITIDAS.stream().anyMatch(ruta::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain cadena)
            throws ServletException, IOException {
        boolean bloquear = sesion.usuario().map(u -> !perfiles.estaCompleto(u.id())).orElse(false);
        if (bloquear) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(CUERPO);
            return;
        }
        cadena.doFilter(request, response);
    }
}
