package com.camellofacil.backend.auth.web;

import com.camellofacil.backend.auth.app.ServicioOidcMicrosoft;
import com.camellofacil.backend.auth.app.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UsuarioService usuarios;

    public AuthController(UsuarioService usuarios) {
        this.usuarios = usuarios;
    }

    /** Quién tiene la sesión iniciada. Sin sesión responde 401 (lo resuelve la configuración de seguridad). */
    @GetMapping("/me")
    public ResponseEntity<UsuarioRespuesta> yo(@AuthenticationPrincipal OidcUser principal) {
        return usuarios.buscar(UsuarioService.PROVEEDOR_MICROSOFT, ServicioOidcMicrosoft.idExternoDe(principal))
                .map(u -> ResponseEntity.ok(new UsuarioRespuesta(u.correo(), u.nombre())))
                .orElseGet(() -> ResponseEntity.status(401).build());
    }
}
