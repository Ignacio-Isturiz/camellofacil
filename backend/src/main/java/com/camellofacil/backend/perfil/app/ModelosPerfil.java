package com.camellofacil.backend.perfil.app;

import java.util.Set;

/** Tipos del módulo perfil que usan las capas web y app. */
public final class ModelosPerfil {

    private ModelosPerfil() {}

    public enum TipoPersona {
        ESTUDIANTE,
        EGRESADO
    }

    public enum Modalidad {
        PRESENCIAL,
        REMOTO,
        HIBRIDO
    }

    public enum Disponibilidad {
        TIEMPO_COMPLETO,
        MEDIO_TIEMPO,
        FLEXIBLE
    }

    public record CarreraDatos(String codigo, String nombre) {}

    /** Lo obligatorio del primer ingreso (RF-05 a RF-07). */
    public record DatosBasicos(TipoPersona tipo, String carrera, Integer semestre) {}

    /** Lo opcional (RF-08): modalidades y disponibilidad. */
    public record Preferencias(Set<Modalidad> modalidades, Disponibilidad disponibilidad) {}

    public record PerfilDatos(
            TipoPersona tipo, CarreraDatos carrera, Integer semestre, Preferencias preferencias) {}
}
