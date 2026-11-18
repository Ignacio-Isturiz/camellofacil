package com.camellofacil.backend.versiones.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BorradorHojaRepository extends JpaRepository<BorradorHoja, UUID> {

    Optional<BorradorHoja> findByIdAndUsuarioId(UUID id, UUID usuarioId);

    /** Del más antiguo al más nuevo: sirve para descartar el sobrante. */
    List<BorradorHoja> findByUsuarioIdOrderByCreadoEnAsc(UUID usuarioId);
}
