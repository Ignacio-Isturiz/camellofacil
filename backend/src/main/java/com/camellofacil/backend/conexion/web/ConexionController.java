package com.camellofacil.backend.conexion.web;

import com.camellofacil.backend.conexion.app.ConexionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/conexion", produces = MediaType.APPLICATION_JSON_VALUE)
public class ConexionController {

    private final ConexionService conexionService;

    public ConexionController(ConexionService conexionService) {
        this.conexionService = conexionService;
    }

    @GetMapping
    public MensajeRespuesta comprobar() {
        return new MensajeRespuesta(conexionService.mensajeDeConexion());
    }
}
