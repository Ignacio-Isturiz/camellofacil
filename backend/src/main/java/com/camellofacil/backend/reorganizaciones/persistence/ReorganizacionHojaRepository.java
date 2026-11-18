package com.camellofacil.backend.reorganizaciones.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReorganizacionHojaRepository extends JpaRepository<ReorganizacionHoja, UUID> {

    /** Siempre se busca por dueño: nadie alcanza una propuesta ajena (IDOR). */
    Optional<ReorganizacionHoja> findByIdAndUsuarioId(UUID id, UUID usuarioId);

    /** La última propuesta con redacción de la IA para esa misma hoja: se reutiliza sin volver a llamarla. */
    Optional<ReorganizacionHoja> findFirstByUsuarioIdAndBaseHuellaAndIaGeneradaTrueOrderByCreadaEnDesc(UUID usuarioId, String baseHuella);

    long countByUsuarioIdAndIaGeneradaTrueAndCreadaEnAfter(UUID usuarioId, Instant desde);

    /** Del más antiguo al más nuevo: sirve para descartar el sobrante. */
    List<ReorganizacionHoja> findByUsuarioIdOrderByCreadaEnAsc(UUID usuarioId);
}
