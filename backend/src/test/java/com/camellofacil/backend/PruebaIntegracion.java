package com.camellofacil.backend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base de las pruebas de integración: un solo Postgres real (Testcontainers) y un solo contexto de Spring
 * compartidos por todas las clases, para que la suite no arranque uno por clase.
 */
@SpringBootTest(properties = {
    // Valores ficticios: las pruebas no hablan con Microsoft
    "camellofacil.microsoft.client-id=cliente-de-prueba",
    "camellofacil.microsoft.client-secret=secreto-de-prueba",
    "camellofacil.microsoft.tenant-id=00000000-0000-0000-0000-000000000000",
    // El análisis de hojas se dispara a mano en las pruebas, con una IA falsa (ExtractorIaDePrueba)
    "camellofacil.cv.analisis-automatico=false",
    // Las vacantes se traen a mano, solo de la fuente falsa (FuenteDePrueba): nunca de JSearch ni Jooble reales
    "camellofacil.vacantes.ingesta-automatica=false",
    "camellofacil.vacantes.jsearch.api-key=",
    "camellofacil.vacantes.jooble.api-key="
})
@Import({ExtractorIaDePrueba.class, AdaptadorIaDePrueba.class, RecomendadorIaDePrueba.class, ReorganizadorIaDePrueba.class, FuenteDePrueba.class})
@AutoConfigureMockMvc
public abstract class PruebaIntegracion {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17");

    static {
        POSTGRES.start();
    }

    @Autowired
    protected MockMvc mockMvc;
}
