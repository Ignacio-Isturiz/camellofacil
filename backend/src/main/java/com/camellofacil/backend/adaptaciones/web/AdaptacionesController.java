package com.camellofacil.backend.adaptaciones.web;

import com.camellofacil.backend.adaptaciones.app.AdaptacionesService;
import com.camellofacil.backend.adaptaciones.app.ModelosAdaptacion.AdaptacionInfo;
import com.camellofacil.backend.adaptaciones.app.ModelosAdaptacion.CupoAdaptaciones;
import com.camellofacil.backend.adaptaciones.app.ModelosAdaptacion.EstadoAdaptacion;
import com.camellofacil.backend.auth.app.SesionActual;
import com.camellofacil.backend.auth.app.UsuarioActual;
import com.camellofacil.backend.adaptaciones.app.ModelosAdaptacion.ContenidoDescarga;
import com.camellofacil.backend.plantillas.app.ExportadorDocumento.Formato;
import com.camellofacil.backend.plantillas.app.PlantillasService;
import com.camellofacil.backend.plantillas.web.Descargas;
import com.camellofacil.backend.versiones.app.ModelosVersion.VersionResumen;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdaptacionesController {

    private final SesionActual sesion;
    private final AdaptacionesService adaptaciones;
    private final PlantillasService plantillas;

    public AdaptacionesController(SesionActual sesion, AdaptacionesService adaptaciones, PlantillasService plantillas) {
        this.sesion = sesion;
        this.adaptaciones = adaptaciones;
        this.plantillas = plantillas;
    }

    /** La adaptación ya guardada para la vacante (si hay) y cuántas le quedan hoy. */
    @GetMapping("/api/v1/vacantes/{vacanteId}/adaptacion")
    public EstadoAdaptacion estado(@PathVariable UUID vacanteId) {
        return adaptaciones.estado(sesion.requerido().id(), vacanteId);
    }

    /** Crea la adaptación o regenera la existente. Gasta una del cupo de hoy (429 si no quedan). */
    @PostMapping("/api/v1/vacantes/{vacanteId}/adaptacion")
    public AdaptacionInfo crear(@PathVariable UUID vacanteId) {
        UsuarioActual u = sesion.requerido();
        return adaptaciones.crear(u.id(), u.correo(), vacanteId);
    }

    @GetMapping("/api/v1/adaptaciones/cupo")
    public CupoAdaptaciones cupo() {
        return adaptaciones.cupo(sesion.requerido().id());
    }

    /** Descarga la hoja adaptada en PDF o Word (`formato=pdf|docx`), sin guardarla antes como versión. */
    @GetMapping("/api/v1/adaptaciones/{id}/descarga")
    public ResponseEntity<byte[]> descargar(@PathVariable UUID id, @RequestParam(defaultValue = "pdf") String formato) {
        Formato elegido = Descargas.formato(formato);
        ContenidoDescarga c = adaptaciones.paraDescargar(sesion.requerido().id(), id);
        return Descargas.respuesta(plantillas.exportar(c.plantillaId(), c.documento(), elegido), "hoja-de-vida-adaptada");
    }

    @DeleteMapping("/api/v1/adaptaciones/{id}")
    public ResponseEntity<Void> descartar(@PathVariable UUID id) {
        adaptaciones.descartar(sesion.requerido().id(), id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Guarda la adaptación como versión de la hoja. El cuerpo `{ postular }` lo manda la interfaz; la postulación
     * en sí la registra el módulo de postulaciones al hacer clic, no este.
     */
    @PostMapping("/api/v1/adaptaciones/{id}/aceptacion")
    public ResponseEntity<VersionResumen> aceptar(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adaptaciones.aceptar(sesion.requerido().id(), id));
    }
}
