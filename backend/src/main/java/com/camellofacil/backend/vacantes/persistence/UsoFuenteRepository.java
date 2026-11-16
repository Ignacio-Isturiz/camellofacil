package com.camellofacil.backend.vacantes.persistence;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsoFuenteRepository extends JpaRepository<UsoFuente, String> {

    /** Crea el registro de la fuente si no existe (sin fallar si otra copia del servidor lo crea a la vez). */
    @Modifying
    @Query(value = """
            INSERT INTO uso_fuente (fuente, periodo_inicio, consumidas) VALUES (:fuente, :inicio, 0)
            ON CONFLICT (fuente) DO NOTHING""", nativeQuery = true)
    void crearSiNoExiste(@Param("fuente") String fuente, @Param("inicio") Instant inicio);

    /** Lee el cupo y lo bloquea hasta el final de la transacción: nadie más lo gasta mientras tanto. */
    @Query(value = "SELECT * FROM uso_fuente WHERE fuente = :fuente FOR UPDATE", nativeQuery = true)
    Optional<UsoFuente> bloquear(@Param("fuente") String fuente);
}
