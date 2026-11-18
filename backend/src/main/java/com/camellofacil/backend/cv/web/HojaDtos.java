package com.camellofacil.backend.cv.web;

import com.camellofacil.backend.cv.app.ModelosAnalisis.Observacion;
import com.camellofacil.backend.cv.app.ModelosAnalisis.OpcionAts;
import com.camellofacil.backend.cv.app.ModelosAnalisis.Puntaje;
import com.camellofacil.backend.cv.app.ModelosAnalisis.PuntajeAts;
import com.camellofacil.backend.cv.app.ModelosHoja.Campo;
import com.camellofacil.backend.cv.app.ModelosHoja.Educacion;
import com.camellofacil.backend.cv.app.ModelosHoja.Experiencia;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/** Contrato HTTP del módulo cv (el mismo que espera core/hoja-de-vida/hoja-de-vida.modelos.ts). */
final class HojaDtos {

    private HojaDtos() {}

    record ArchivoRespuesta(String nombre, Instant actualizadaEn, String idioma, int paginas) {}

    /**
     * El análisis Harvard / ATS solo existe con la hoja confirmada; antes va vacío y la interfaz lo muestra como
     * pendiente. `recomendacionesIa`: lista | pendiente | no_disponible.
     */
    record HojaRespuesta(
            ArchivoRespuesta archivo, Puntaje harvard, PuntajeAts ats, List<OpcionAts> opcionesAts, List<Observacion> observaciones,
            String recomendacionesIa) {}

    /** estado: sin_hoja | sin_confirmar | confirmada. */
    record ResumenRespuesta(String estado, HojaRespuesta hoja, Integer pendientesRevision) {}

    /** estado: analizando | listo | ilegible | error. */
    record AvanceRespuesta(String estado) {}

    record ArchivoBasico(String nombre, String idioma) {}

    record ContactoRespuesta(Campo nombre, Campo correo, Campo telefono, Campo ciudad) {}

    record DatosRespuesta(
            ArchivoBasico archivo,
            ContactoRespuesta contacto,
            Campo perfil,
            List<Experiencia> experiencia,
            List<Educacion> educacion,
            Campo habilidades) {}

    record ExperienciaSolicitud(
            @Size(max = 40) String id,
            @Size(max = 150) String cargo,
            @Size(max = 150) String organizacion,
            @Size(max = 7) String inicio,
            @Size(max = 7) String fin,
            @Size(max = 1500) String descripcion) {}

    record EducacionSolicitud(
            @Size(max = 40) String id,
            @Size(max = 200) String titulo,
            @Size(max = 200) String institucion,
            @Size(max = 7) String inicio,
            @Size(max = 7) String fin) {}

    record ValoresSolicitud(
            @Size(max = 150) String nombre,
            @Size(max = 40) String telefono,
            @Size(max = 100) String ciudad,
            @Size(max = 2000) String perfil,
            @Size(max = 1000) String habilidades,
            @NotNull @Size(max = 15) List<@Valid ExperienciaSolicitud> experiencia,
            @NotNull @Size(max = 15) List<@Valid EducacionSolicitud> educacion) {}
}
