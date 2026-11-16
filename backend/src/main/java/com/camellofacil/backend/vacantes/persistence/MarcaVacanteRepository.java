package com.camellofacil.backend.vacantes.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MarcaVacanteRepository extends JpaRepository<MarcaVacante, MarcaVacante.Clave> {

    @Query("SELECT m.id.vacanteId FROM MarcaVacante m WHERE m.id.usuarioId = :usuarioId AND m.id.tipo = :tipo")
    List<UUID> vacantesMarcadas(@Param("usuarioId") UUID usuarioId, @Param("tipo") String tipo);
}
