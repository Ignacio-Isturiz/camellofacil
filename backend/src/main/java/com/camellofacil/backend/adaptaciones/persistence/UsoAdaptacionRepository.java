package com.camellofacil.backend.adaptaciones.persistence;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface UsoAdaptacionRepository extends JpaRepository<UsoAdaptacion, UUID> {

    /**
     * Gasta una adaptación del día en una sola sentencia atómica. Devuelve 1 si había cupo y 0 si ya se llegó
     * al límite. Si la transacción falla después (p. ej. la IA no responde), el gasto se deshace con ella.
     */
    @Modifying
    @Query(value = """
            INSERT INTO uso_adaptacion (usuario_id, fecha, usadas) VALUES (:usuarioId, :fecha, 1)
            ON CONFLICT (usuario_id, fecha) DO UPDATE SET usadas = uso_adaptacion.usadas + 1
            WHERE uso_adaptacion.usadas < :limite""", nativeQuery = true)
    int consumir(UUID usuarioId, LocalDate fecha, int limite);

    @Query(value = "SELECT usadas FROM uso_adaptacion WHERE usuario_id = :usuarioId AND fecha = :fecha", nativeQuery = true)
    Optional<Integer> usadas(UUID usuarioId, LocalDate fecha);
}
