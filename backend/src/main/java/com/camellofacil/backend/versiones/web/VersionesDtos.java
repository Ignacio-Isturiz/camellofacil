package com.camellofacil.backend.versiones.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Cuerpos de las peticiones de versiones y borradores. Los límites repiten los de la hoja base. */
interface VersionesDtos {

    record NombreSolicitud(@NotBlank @Size(max = 120) String nombre) {}

    record BorradorSolicitud(
            @Size(max = 40) String plantillaId, @Size(max = 40) String versionId, @Size(max = 40) String adaptacionId,
            @Size(max = 2) String idioma) {}

    record ExperienciaSolicitud(
            @Size(max = 40) String id, @Size(max = 150) String cargo, @Size(max = 150) String organizacion,
            @Size(max = 7) String inicio, @Size(max = 7) String fin, @Size(max = 2000) String descripcion) {}

    record EducacionSolicitud(
            @Size(max = 40) String id, @Size(max = 150) String titulo, @Size(max = 150) String institucion,
            @Size(max = 7) String inicio, @Size(max = 7) String fin) {}

    record ValoresSolicitud(
            @Size(max = 150) String nombre,
            @Size(max = 40) String telefono,
            @Size(max = 100) String ciudad,
            @Size(max = 2000) String perfil,
            @Size(max = 1000) String habilidades,
            @NotNull @Size(max = 15) List<@Valid ExperienciaSolicitud> experiencia,
            @NotNull @Size(max = 15) List<@Valid EducacionSolicitud> educacion) {}
}
