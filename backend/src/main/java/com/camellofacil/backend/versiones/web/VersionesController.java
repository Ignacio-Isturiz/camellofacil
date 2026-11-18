package com.camellofacil.backend.versiones.web;

import com.camellofacil.backend.auth.app.SesionActual;
import com.camellofacil.backend.auth.app.UsuarioActual;
import com.camellofacil.backend.cv.app.ModelosHoja.ValorEducacion;
import com.camellofacil.backend.cv.app.ModelosHoja.ValorExperiencia;
import com.camellofacil.backend.cv.app.ModelosHoja.ValoresHoja;
import com.camellofacil.backend.plantillas.app.ExportadorDocumento.Archivo;
import com.camellofacil.backend.plantillas.app.ExportadorDocumento.Formato;
import com.camellofacil.backend.plantillas.app.ModelosPlantilla.DocumentoHoja;
import com.camellofacil.backend.plantillas.app.PlantillasService;
import com.camellofacil.backend.plantillas.web.Descargas;
import com.camellofacil.backend.versiones.app.BorradoresService;
import com.camellofacil.backend.versiones.app.ModelosVersion.BorradorInfo;
import com.camellofacil.backend.versiones.app.ModelosVersion.SolicitudBorrador;
import com.camellofacil.backend.versiones.app.ModelosVersion.VersionResumen;
import com.camellofacil.backend.versiones.app.VersionesService;
import com.camellofacil.backend.versiones.web.VersionesDtos.BorradorSolicitud;
import com.camellofacil.backend.versiones.web.VersionesDtos.NombreSolicitud;
import com.camellofacil.backend.versiones.web.VersionesDtos.ValoresSolicitud;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/hoja-de-vida")
public class VersionesController {

    private final SesionActual sesion;
    private final VersionesService versiones;
    private final BorradoresService borradores;
    private final PlantillasService plantillas;

    public VersionesController(
            SesionActual sesion, VersionesService versiones, BorradoresService borradores, PlantillasService plantillas) {
        this.sesion = sesion;
        this.versiones = versiones;
        this.borradores = borradores;
        this.plantillas = plantillas;
    }

    @GetMapping("/versiones")
    public List<VersionResumen> listar() {
        UsuarioActual u = sesion.requerido();
        return versiones.listar(u.id(), u.correo());
    }

    @GetMapping("/versiones/{id}/documento")
    public DocumentoHoja documento(@PathVariable UUID id) {
        return versiones.documento(sesion.requerido().id(), id);
    }

    /** Descarga la versión en PDF o Word (`formato=pdf|docx`). Se arma al vuelo: no se guarda ningún archivo. */
    @GetMapping("/versiones/{id}/descarga")
    public ResponseEntity<byte[]> descargar(@PathVariable UUID id, @RequestParam(defaultValue = "pdf") String formato) {
        Formato elegido = Descargas.formato(formato);
        UUID usuarioId = sesion.requerido().id();
        Archivo archivo = plantillas.exportar(
                versiones.contenido(usuarioId, id).plantillaId(), versiones.documento(usuarioId, id), elegido);
        return Descargas.respuesta(archivo, "hoja-de-vida");
    }

    @PatchMapping("/versiones/{id}")
    public VersionResumen renombrar(@PathVariable UUID id, @Valid @RequestBody NombreSolicitud cuerpo) {
        return versiones.renombrar(sesion.requerido().id(), id, cuerpo.nombre());
    }

    @PostMapping("/versiones/{id}/duplicado")
    public ResponseEntity<VersionResumen> duplicar(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(versiones.duplicar(sesion.requerido().id(), id));
    }

    @PostMapping("/versiones/{id}/base")
    public ResponseEntity<Void> usarComoBase(@PathVariable UUID id) {
        versiones.usarComoBase(sesion.requerido().id(), id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/versiones/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        versiones.eliminar(sesion.requerido().id(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/borradores")
    public ResponseEntity<BorradorInfo> crearBorrador(@Valid @RequestBody BorradorSolicitud cuerpo) {
        UsuarioActual u = sesion.requerido();
        BorradorInfo b = borradores.crear(u.id(), u.correo(), new SolicitudBorrador(cuerpo.plantillaId(), cuerpo.versionId(), cuerpo.adaptacionId(), cuerpo.idioma()));
        return ResponseEntity.status(HttpStatus.CREATED).body(b);
    }

    @GetMapping("/borradores/{id}")
    public BorradorInfo borrador(@PathVariable UUID id) {
        UsuarioActual u = sesion.requerido();
        return borradores.obtener(u.id(), u.correo(), id);
    }

    @PutMapping("/borradores/{id}")
    public BorradorInfo guardarBorrador(@PathVariable UUID id, @Valid @RequestBody ValoresSolicitud v) {
        UsuarioActual u = sesion.requerido();
        return borradores.guardar(u.id(), u.correo(), id, new ValoresHoja(
                v.nombre(), v.telefono(), v.ciudad(), v.perfil(), v.habilidades(),
                v.experiencia().stream()
                        .map(x -> new ValorExperiencia(x.id(), x.cargo(), x.organizacion(), x.inicio(), x.fin(), x.descripcion()))
                        .toList(),
                v.educacion().stream()
                        .map(x -> new ValorEducacion(x.id(), x.titulo(), x.institucion(), x.inicio(), x.fin()))
                        .toList()));
    }

    @PostMapping("/borradores/{id}/version")
    public ResponseEntity<VersionResumen> guardarComoVersion(@PathVariable UUID id) {
        UsuarioActual u = sesion.requerido();
        return ResponseEntity.status(HttpStatus.CREATED).body(borradores.guardarComoVersion(u.id(), u.correo(), id));
    }
}
