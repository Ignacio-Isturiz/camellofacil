package com.camellofacil.backend.perfil.web;

import com.camellofacil.backend.perfil.app.ModelosPerfil.CarreraDatos;
import com.camellofacil.backend.perfil.app.ModelosPerfil.Disponibilidad;
import com.camellofacil.backend.perfil.app.ModelosPerfil.Modalidad;
import com.camellofacil.backend.perfil.app.ModelosPerfil.TipoPersona;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Set;

/** Contrato HTTP del módulo perfil. */
final class PerfilDtos {

    private PerfilDtos() {}

    /** Modal del primer ingreso: todo es obligatorio, incluidos los dos consentimientos. */
    record CompletarSolicitud(
            @NotNull TipoPersona tipo,
            @NotBlank @Size(max = 40) String carrera,
            @Min(1) @Max(10) Integer semestre,
            @NotBlank @Size(max = 10) String versionTerminos,
            @AssertTrue boolean aceptaTerminos,
            @AssertTrue boolean autorizaAnalisisIa) {}

    record ActualizarSolicitud(
            @NotNull TipoPersona tipo,
            @NotBlank @Size(max = 40) String carrera,
            @Min(1) @Max(10) Integer semestre,
            @Size(max = 3) Set<Modalidad> modalidades,
            Disponibilidad disponibilidad) {}

    record ConsentimientoRespuesta(String version, Instant otorgadoEn) {}

    record DatosRespuesta(
            TipoPersona tipo,
            CarreraDatos carrera,
            Integer semestre,
            Set<Modalidad> modalidades,
            Disponibilidad disponibilidad) {}

    /**
     * Estado del perfil. Con `completo=false` el frontend muestra el modal obligatorio del primer ingreso.
     * `terminos` y `analisisIa` son null si no hay un consentimiento vigente.
     */
    record PerfilRespuesta(
            boolean completo,
            String nombre,
            String correo,
            String versionTerminosVigente,
            DatosRespuesta datos,
            ConsentimientoRespuesta terminos,
            ConsentimientoRespuesta analisisIa) {}
}
