package com.camellofacil.backend.auth.app;

import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/**
 * Servicio público del módulo auth para que los demás módulos sepan quién hace la petición,
 * sin conocer cómo se inició sesión (hoy, Microsoft).
 */
@Service
public class SesionActual {

    private final UsuarioService usuarios;

    public SesionActual(UsuarioService usuarios) {
        this.usuarios = usuarios;
    }

    public Optional<UsuarioActual> usuario() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !(autenticacion.getPrincipal() instanceof OidcUser oidc)) {
            return Optional.empty();
        }
        return usuarios.buscar(UsuarioService.PROVEEDOR_MICROSOFT, ServicioOidcMicrosoft.idExternoDe(oidc));
    }

    /** Usuario de la petición; si no hay (sesión de una cuenta ya borrada), responde 401. */
    public UsuarioActual requerido() {
        return usuario().orElseThrow(SinSesionException::new);
    }
}
