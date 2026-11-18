package com.camellofacil.backend.adaptaciones.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdaptacionHojaRepository extends JpaRepository<AdaptacionHoja, UUID> {

    /** Siempre se busca por dueño: nadie alcanza una adaptación ajena (IDOR). */
    Optional<AdaptacionHoja> findByIdAndUsuarioId(UUID id, UUID usuarioId);

    Optional<AdaptacionHoja> findByUsuarioIdAndVacanteId(UUID usuarioId, UUID vacanteId);
}
