package com.camellofacil.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Integración con Postgres real: arranca la app, corre Flyway y llama al endpoint de conexión. */
@SpringBootTest(properties = {
    // Valores ficticios: las pruebas no hablan con Microsoft
    "camellofacil.microsoft.client-id=cliente-de-prueba",
    "camellofacil.microsoft.client-secret=secreto-de-prueba",
    "camellofacil.microsoft.tenant-id=00000000-0000-0000-0000-000000000000"
})
@AutoConfigureMockMvc
@Testcontainers
class BackendApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    MockMvc mockMvc;

    @Test
    void endpointDeConexionResponde() throws Exception {
        mockMvc.perform(get("/api/v1/conexion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Conexión con el backend establecida"));
    }

    @Test
    void endpointsPrivadosExigenSesion() throws Exception {
        mockMvc.perform(get("/api/v1/otra-cosa"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void quienSoyExigeSesion() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void elInicioConMicrosoftRedirigeAlInquilino() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/microsoft"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.startsWith("https://login.microsoftonline.com/00000000-0000-0000-0000-000000000000/oauth2/v2.0/authorize")))
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("code_challenge=")))
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("prompt=login")));
    }

    @Test
    void laSesionVenceTrasOchoHorasDeInactividad(@Autowired org.springframework.core.env.Environment entorno) {
        org.assertj.core.api.Assertions.assertThat(entorno.getProperty("spring.session.timeout")).isEqualTo("8h");
    }
}
