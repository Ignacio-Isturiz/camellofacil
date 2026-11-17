package com.camellofacil.backend.vacantes.app;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Tipos de dominio del módulo de vacantes. */
public final class ModelosVacante {

    private ModelosVacante() {}

    public enum TipoVacante { PRACTICA, PROFESIONAL }

    public enum Modalidad { PRESENCIAL, HIBRIDO, REMOTO }

    /** Carreras de UNAULA (mismos códigos de la tabla de carreras del perfil). */
    public static final Set<String> CARRERAS =
            Set.of("informatica", "industrial", "datos", "economia", "contaduria", "administracion", "derecho", "sociales");

    /**
     * Una oferta tal como la entrega una fuente, ya traducida a nuestros campos pero sin clasificar.
     * Es contenido no confiable: se limpia y se recorta antes de guardarlo.
     *
     * @param tipoEmpleo  texto o códigos de tipo de empleo de la fuente (p. ej. «Pasantía», «INTERN»)
     * @param publicadaEn null si la fuente no la da
     */
    public record VacanteExterna(
            String idExterno,
            String cargo,
            String empresa,
            String ciudad,
            boolean remota,
            String tipoEmpleo,
            String publicador,
            String url,
            String descripcion,
            List<String> responsabilidades,
            List<String> requisitos,
            Instant publicadaEn) {}

    /** Lo que se guarda: la oferta limpia, clasificada (RF-31) y con sus carreras (RF-32). */
    public record VacanteNueva(
            String fuente,
            String idExterno,
            String huella,
            String cargo,
            String empresa,
            String ciudad,
            Modalidad modalidad,
            TipoVacante tipo,
            String publicador,
            String url,
            String descripcion,
            List<String> responsabilidades,
            List<String> requisitos,
            Set<String> carreras,
            Instant publicadaEn) {}

    public record VacanteResumen(
            UUID id,
            String cargo,
            String empresa,
            TipoVacante tipo,
            String ciudad,
            Modalidad modalidad,
            Instant publicadaEn,
            String publicador,
            List<String> carreras) {}

    public record VacanteDetalle(
            VacanteResumen resumen,
            String descripcion,
            List<String> responsabilidades,
            List<String> requisitos) {}

    /** Vacantes abiertas y cuáles tiene guardadas la persona. */
    public record ListaVacantes(List<VacanteResumen> vacantes, Set<UUID> guardadas) {}

    /**
     * Lo que otro módulo necesita para registrar una postulación: los datos a mostrar y la página original de
     * la oferta (solo http/https, ya validada al guardarla).
     */
    public record VacanteParaPostular(
            UUID id, String cargo, String empresa, String ciudad, Modalidad modalidad, TipoVacante tipo, String url) {}
}
