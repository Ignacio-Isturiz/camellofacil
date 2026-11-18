package com.camellofacil.backend.versiones.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface VersionHojaRepository extends JpaRepository<VersionHoja, UUID> {

    List<VersionHoja> findByUsuarioIdOrderByCreadaEnDesc(UUID usuarioId);

    /** Siempre se busca por dueño: nadie alcanza una versión ajena (IDOR). */
    Optional<VersionHoja> findByIdAndUsuarioId(UUID id, UUID usuarioId);

    long countByUsuarioId(UUID usuarioId);

    boolean existsByUsuarioIdAndEsBaseTrue(UUID usuarioId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update VersionHoja v set v.esBase = false where v.usuarioId = :usuarioId and v.esBase = true")
    void quitarBase(UUID usuarioId);
}
