package com.camellofacil.backend.reorganizaciones.web;

import com.camellofacil.backend.auth.app.SesionActual;
import com.camellofacil.backend.auth.app.UsuarioActual;
import com.camellofacil.backend.reorganizaciones.app.ModelosReorganizacion.PropuestaInfo;
import com.camellofacil.backend.reorganizaciones.app.ReorganizacionesService;
import com.camellofacil.backend.versiones.app.ModelosVersion.VersionResumen;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/hoja-de-vida/reorganizaciones")
public class ReorganizacionesController {

    record SolicitudReorganizacion(@NotBlank @Size(max = 10) String formato, @NotBlank @Size(max = 2) String idioma) {}

    record CambioSolicitud(boolean aplicado) {}

    private final SesionActual sesion;
    private final ReorganizacionesService reorganizaciones;

    public ReorganizacionesController(SesionActual sesion, ReorganizacionesService reorganizaciones) {
        this.sesion = sesion;
        this.reorganizaciones = reorganizaciones;
    }

    /** Propone cómo quedaría la hoja base en el formato elegido. Puede tardar unos segundos (redacción con IA). */
    @PostMapping
    public ResponseEntity<PropuestaInfo> proponer(@Valid @RequestBody SolicitudReorganizacion cuerpo) {
        UsuarioActual u = sesion.requerido();
        return ResponseEntity.status(HttpStatus.CREATED).body(reorganizaciones.proponer(u.id(), u.correo(), cuerpo.formato(), cuerpo.idioma()));
    }

    /** Aplica o revierte un cambio y devuelve la propuesta recalculada (sin llamar a la IA). */
    @PutMapping("/{id}/cambios/{cambioId}")
    public PropuestaInfo cambiar(@PathVariable UUID id, @PathVariable String cambioId, @RequestBody CambioSolicitud cuerpo) {
        UsuarioActual u = sesion.requerido();
        return reorganizaciones.cambiar(u.id(), u.correo(), id, cambioId, cuerpo.aplicado());
    }

    @PostMapping("/{id}/aceptacion")
    public ResponseEntity<VersionResumen> aceptar(@PathVariable UUID id) {
        UsuarioActual u = sesion.requerido();
        return ResponseEntity.status(HttpStatus.CREATED).body(reorganizaciones.aceptar(u.id(), u.correo(), id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> descartar(@PathVariable UUID id) {
        reorganizaciones.descartar(sesion.requerido().id(), id);
        return ResponseEntity.noContent().build();
    }
}
