package com.camellofacil.backend.vacantes.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConsultaVacantesRepository extends JpaRepository<ConsultaVacantes, UUID> {

    long countByFuenteAndActivaTrue(String fuente);

    /**
     * La búsqueda que más le toca: la de mayor (tiempo sin ejecutarse × peso). Así cada búsqueda se repite
     * con una frecuencia proporcional a su peso. Las que nunca se han ejecutado van primero, por peso.
     */
    @Query(value = """
            SELECT * FROM consulta_vacantes
            WHERE fuente = :fuente AND activa
            ORDER BY ultima_ejecucion IS NOT NULL,
                     EXTRACT(EPOCH FROM (CAST(:ahora AS timestamptz) - ultima_ejecucion)) * peso DESC,
                     peso DESC
            LIMIT 1
            FOR UPDATE SKIP LOCKED""", nativeQuery = true)
    Optional<ConsultaVacantes> siguiente(@Param("fuente") String fuente, @Param("ahora") Instant ahora);
}
