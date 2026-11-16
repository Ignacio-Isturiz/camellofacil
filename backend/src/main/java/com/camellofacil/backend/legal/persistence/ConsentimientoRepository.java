package com.camellofacil.backend.legal.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentimientoRepository extends JpaRepository<Consentimiento, UUID> {

    /** El consentimiento activo (sin revocar) más reciente de ese tipo. */
    Optional<Consentimiento> findFirstByUsuarioIdAndTipoAndRevocadoEnIsNullOrderByOtorgadoEnDesc(UUID usuarioId, String tipo);
}
