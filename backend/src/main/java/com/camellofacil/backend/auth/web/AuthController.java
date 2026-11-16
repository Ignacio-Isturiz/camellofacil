package com.camellofacil.backend.auth.web;

import com.camellofacil.backend.auth.app.SesionActual;
import com.camellofacil.backend.auth.app.UsuarioActual;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final SesionActual sesion;

    public AuthController(SesionActual sesion) {
        this.sesion = sesion;
    }

    /** Quién tiene la sesión iniciada. Sin sesión responde 401. */
    @GetMapping("/me")
    public UsuarioRespuesta yo() {
        UsuarioActual u = sesion.requerido();
        return new UsuarioRespuesta(u.correo(), u.nombre());
    }
}
