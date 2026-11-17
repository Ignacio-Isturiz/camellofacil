package com.camellofacil.backend.postulaciones.persistence;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClicPostulacionRepository extends JpaRepository<ClicPostulacion, UUID> {

    @Query("""
            SELECT count(c) FROM ClicPostulacion c, Postulacion p
            WHERE c.postulacionId = p.id AND p.usuarioId = :usuarioId AND c.creadoEn >= :desde""")
    long contarDesde(@Param("usuarioId") UUID usuarioId, @Param("desde") Instant desde);
}
