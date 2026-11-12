package com.camellofacil.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
@SpringBootTest
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
                .andExpect(status().isForbidden());
    }
}
