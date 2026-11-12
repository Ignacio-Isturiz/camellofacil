package com.camellofacil.backend.conexion.app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ConexionServiceTest {

    @Test
    void devuelveElMensajeDeConexion() {
        assertEquals("Conexión con el backend establecida", new ConexionService().mensajeDeConexion());
    }
}
