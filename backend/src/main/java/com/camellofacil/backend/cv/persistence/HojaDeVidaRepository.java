package com.camellofacil.backend.cv.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface HojaDeVidaRepository extends JpaRepository<HojaDeVida, UUID> {

    /**
     * La siguiente hoja por analizar, bloqueada para esta transacción. SKIP LOCKED permite que varias copias
     * del servidor trabajen a la vez sin tomar la misma.
     */
    @Query(value = """
            SELECT * FROM hoja_de_vida
            WHERE estado = 'ANALIZANDO'
              AND (siguiente_intento_en IS NULL OR siguiente_intento_en <= now())
            ORDER BY subida_en
            LIMIT 1
            FOR UPDATE SKIP LOCKED""", nativeQuery = true)
    Optional<HojaDeVida> siguientePorAnalizar();
}
