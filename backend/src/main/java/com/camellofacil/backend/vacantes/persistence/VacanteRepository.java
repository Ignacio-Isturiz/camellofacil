package com.camellofacil.backend.vacantes.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VacanteRepository extends JpaRepository<Vacante, UUID> {

    /**
     * Inserta la vacante o, si ya existe una con la misma huella (la misma oferta vista en otro portal o en
     * otra búsqueda), solo la marca como vista, la reabre, suma carreras y se queda con la descripción más
     * completa. Así no hay duplicados aunque dos copias del servidor guarden a la vez.
     */
    @Modifying
    @Query(value = """
            INSERT INTO vacante (id, fuente, id_externo, huella, cargo, empresa, ciudad, modalidad, tipo, publicador,
                                 url_postulacion, descripcion, responsabilidades, requisitos, carreras,
                                 publicada_en, vista_en, creada_en)
            VALUES (:id, :fuente, :idExterno, :huella, :cargo, :empresa, :ciudad, :modalidad, :tipo, :publicador,
                    :url, :descripcion, CAST(:responsabilidades AS jsonb), CAST(:requisitos AS jsonb),
                    string_to_array(:carreras, ','), :publicadaEn, :ahora, :ahora)
            ON CONFLICT (huella) DO UPDATE SET
                vista_en = EXCLUDED.vista_en,
                cerrada_en = NULL,
                carreras = ARRAY(SELECT DISTINCT c FROM unnest(vacante.carreras || EXCLUDED.carreras) AS c ORDER BY c),
                descripcion = CASE WHEN length(coalesce(EXCLUDED.descripcion, '')) > length(coalesce(vacante.descripcion, ''))
                                   THEN EXCLUDED.descripcion ELSE vacante.descripcion END,
                publicada_en = LEAST(vacante.publicada_en, EXCLUDED.publicada_en)""", nativeQuery = true)
    int guardarOActualizar(
            @Param("id") UUID id,
            @Param("fuente") String fuente,
            @Param("idExterno") String idExterno,
            @Param("huella") String huella,
            @Param("cargo") String cargo,
            @Param("empresa") String empresa,
            @Param("ciudad") String ciudad,
            @Param("modalidad") String modalidad,
            @Param("tipo") String tipo,
            @Param("publicador") String publicador,
            @Param("url") String url,
            @Param("descripcion") String descripcion,
            @Param("responsabilidades") String responsabilidades,
            @Param("requisitos") String requisitos,
            @Param("carreras") String carreras,
            @Param("publicadaEn") Instant publicadaEn,
            @Param("ahora") Instant ahora);

    /** Abiertas, de la más reciente a la más antigua. */
    @Query("SELECT v FROM Vacante v WHERE v.cerradaEn IS NULL ORDER BY v.publicadaEn DESC")
    List<Vacante> abiertas(Limit limite);

    /**
     * RF-34: cierra las que ya no aparecen en las fuentes (no vistas desde {@code vistaAntesDe}) o que llevan
     * demasiado tiempo publicadas (antes de {@code publicadaAntesDe}).
     */
    @Modifying
    @Query("""
            UPDATE Vacante v SET v.cerradaEn = :ahora
            WHERE v.cerradaEn IS NULL AND (v.vistaEn < :vistaAntesDe OR v.publicadaEn < :publicadaAntesDe)""")
    int cerrarVencidas(
            @Param("ahora") Instant ahora,
            @Param("vistaAntesDe") Instant vistaAntesDe,
            @Param("publicadaAntesDe") Instant publicadaAntesDe);
}
