package com.camellofacil.backend.postulaciones.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostulacionRepository extends JpaRepository<Postulacion, UUID> {

    /**
     * Registra un clic: la primera vez crea la postulación; las siguientes suman un clic, actualizan la hoja
     * usada y los datos de la vacante, y la devuelven al historial si se había quitado. Es una sola sentencia,
     * así que dos clics a la vez nunca crean dos filas.
     */
    @Modifying
    @Query(value = """
            INSERT INTO postulacion (id, usuario_id, vacante_id, cargo, empresa, ciudad, modalidad, tipo, hoja_usada,
                                     clics, primer_clic_en, ultimo_clic_en)
            VALUES (:id, :usuarioId, :vacanteId, :cargo, :empresa, :ciudad, :modalidad, :tipo, :hoja, 1, :ahora, :ahora)
            ON CONFLICT (usuario_id, vacante_id) DO UPDATE SET
                clics = postulacion.clics + 1,
                ultimo_clic_en = EXCLUDED.ultimo_clic_en,
                hoja_usada = EXCLUDED.hoja_usada,
                cargo = EXCLUDED.cargo,
                empresa = EXCLUDED.empresa,
                ciudad = EXCLUDED.ciudad,
                modalidad = EXCLUDED.modalidad,
                tipo = EXCLUDED.tipo,
                quitada_en = NULL""", nativeQuery = true)
    int registrarClic(
            @Param("id") UUID id,
            @Param("usuarioId") UUID usuarioId,
            @Param("vacanteId") UUID vacanteId,
            @Param("cargo") String cargo,
            @Param("empresa") String empresa,
            @Param("ciudad") String ciudad,
            @Param("modalidad") String modalidad,
            @Param("tipo") String tipo,
            @Param("hoja") String hoja,
            @Param("ahora") Instant ahora);

    @Query("SELECT p.vacanteId FROM Postulacion p WHERE p.usuarioId = :usuarioId")
    List<UUID> vacantesPostuladas(@Param("usuarioId") UUID usuarioId);

    Optional<Postulacion> findByUsuarioIdAndVacanteId(UUID usuarioId, UUID vacanteId);

    /** Solo las de esa persona: así nadie puede tocar el historial de otra. */
    Optional<Postulacion> findByIdAndUsuarioId(UUID id, UUID usuarioId);

    List<Postulacion> findByUsuarioIdAndQuitadaEnIsNullOrderByUltimoClicEnDesc(UUID usuarioId, Limit limite);

    /** Borra del todo las que se quitaron del historial antes de {@code antes} (ya no se pueden deshacer). */
    @Modifying
    @Query("DELETE FROM Postulacion p WHERE p.quitadaEn < :antes")
    int borrarQuitadas(@Param("antes") Instant antes);
}
