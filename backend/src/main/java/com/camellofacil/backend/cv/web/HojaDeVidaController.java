package com.camellofacil.backend.cv.web;

import com.camellofacil.backend.auth.app.SesionActual;
import com.camellofacil.backend.auth.app.UsuarioActual;
import com.camellofacil.backend.cv.app.AnalisisFormatoService;
import com.camellofacil.backend.cv.app.HojaDeVidaService;
import com.camellofacil.backend.cv.app.HojaException;
import com.camellofacil.backend.cv.app.ModelosAnalisis.Analisis;
import com.camellofacil.backend.cv.app.ModelosAnalisis.PuntajeAts;
import com.camellofacil.backend.cv.app.ModelosHoja.ArchivoInfo;
import com.camellofacil.backend.cv.app.ModelosHoja.Campo;
import com.camellofacil.backend.cv.app.ModelosHoja.DatosHoja;
import com.camellofacil.backend.cv.app.ModelosHoja.EstadoHoja;
import com.camellofacil.backend.cv.app.ModelosHoja.HojaConDatos;
import com.camellofacil.backend.cv.app.ModelosHoja.ValorEducacion;
import com.camellofacil.backend.cv.app.ModelosHoja.ValorExperiencia;
import com.camellofacil.backend.cv.app.ModelosHoja.ValoresHoja;
import com.camellofacil.backend.cv.web.HojaDtos.ArchivoBasico;
import com.camellofacil.backend.cv.web.HojaDtos.ArchivoRespuesta;
import com.camellofacil.backend.cv.web.HojaDtos.AvanceRespuesta;
import com.camellofacil.backend.cv.web.HojaDtos.ContactoRespuesta;
import com.camellofacil.backend.cv.web.HojaDtos.DatosRespuesta;
import com.camellofacil.backend.cv.web.HojaDtos.HojaRespuesta;
import com.camellofacil.backend.cv.web.HojaDtos.ResumenRespuesta;
import com.camellofacil.backend.cv.web.HojaDtos.ValoresSolicitud;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Hoja de vida base de quien inició sesión. Cada operación usa solo la hoja de esa persona (sin ids en la URL). */
@RestController
@RequestMapping("/api/v1/hoja-de-vida")
public class HojaDeVidaController {

    private final SesionActual sesion;
    private final HojaDeVidaService hojas;
    private final AnalisisFormatoService analisis;

    public HojaDeVidaController(SesionActual sesion, HojaDeVidaService hojas, AnalisisFormatoService analisis) {
        this.sesion = sesion;
        this.hojas = hojas;
        this.analisis = analisis;
    }

    @GetMapping
    public ResumenRespuesta resumen() {
        UUID usuarioId = id();
        return hojas.resumen(usuarioId)
                .filter(r -> r.estado() == EstadoHoja.SIN_CONFIRMAR || r.estado() == EstadoHoja.CONFIRMADA)
                .map(r -> {
                    // El análisis Harvard / ATS solo existe con la hoja confirmada.
                    Analisis a = r.estado() == EstadoHoja.CONFIRMADA ? analisis.analizar(usuarioId) : null;
                    HojaRespuesta hoja = a == null
                            ? new HojaRespuesta(archivo(r.archivo()), null, null, List.of(), List.of(), "no_disponible")
                            : new HojaRespuesta(archivo(r.archivo()), a.harvard(), a.ats(), a.opcionesAts(), a.observaciones(), a.recomendacionesIa());
                    return new ResumenRespuesta(
                            r.estado() == EstadoHoja.CONFIRMADA ? "confirmada" : "sin_confirmar", hoja, r.pendientesRevision());
                })
                .orElseGet(() -> new ResumenRespuesta("sin_hoja", null, null));
    }

    /** El puntaje ATS con otra opción de evaluación (404 si la opción no existe). */
    @GetMapping("/puntajes/ats/{opcionId}")
    public PuntajeAts puntajeAts(@PathVariable String opcionId) {
        return analisis.puntajeAts(id(), opcionId)
                .orElseThrow(() -> new HojaException(HojaException.Motivo.SIN_HOJA, "Esa opción de evaluación no existe"));
    }

    /**
     * Pide las recomendaciones de la IA (tardan unos segundos) y devuelve el resumen ya con ellas. Si la hoja no cambió
     * desde la última vez, devuelve las guardadas sin llamar a la IA.
     */
    @PostMapping("/recomendaciones-ia")
    public ResumenRespuesta recomendacionesIa() {
        UUID usuarioId = id();
        Analisis a = analisis.generarRecomendaciones(usuarioId);
        ArchivoInfo archivo = hojas.resumen(usuarioId).orElseThrow().archivo();
        return new ResumenRespuesta(
                "confirmada",
                new HojaRespuesta(archivo(archivo), a.harvard(), a.ats(), a.opcionesAts(), a.observaciones(), a.recomendacionesIa()),
                null);
    }

    /** Sube o reemplaza la hoja base (RF-13, RF-16). Responde 202: el análisis sigue en segundo plano. */
    @PostMapping(path = "/archivo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> subir(@RequestParam("archivo") MultipartFile archivo) throws IOException {
        hojas.subir(id(), archivo.getOriginalFilename(), archivo.getBytes());
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @GetMapping("/analisis")
    public AvanceRespuesta analisis() {
        EstadoHoja estado = hojas.estado(id())
                .orElseThrow(() -> new HojaException(HojaException.Motivo.SIN_HOJA, "Aún no has subido tu hoja de vida"));
        return new AvanceRespuesta(switch (estado) {
            case ANALIZANDO -> "analizando";
            case SIN_CONFIRMAR, CONFIRMADA -> "listo";
            case ILEGIBLE -> "ilegible";
            case ERROR -> "error";
        });
    }

    @GetMapping("/datos")
    public DatosRespuesta datos() {
        UsuarioActual usuario = sesion.requerido();
        HojaConDatos h = hojas.datos(usuario.id());
        DatosHoja d = h.datos();
        // El correo es el de la cuenta (no el del archivo) y no se edita.
        ContactoRespuesta contacto = new ContactoRespuesta(
                d.contacto().nombre(), Campo.leido(usuario.correo()), d.contacto().telefono(), d.contacto().ciudad());
        return new DatosRespuesta(
                new ArchivoBasico(h.archivo().nombre(), h.archivo().idioma()),
                contacto, d.perfil(), d.experiencia(), d.educacion(), d.habilidades());
    }

    @PutMapping("/datos")
    public ResponseEntity<Void> corregir(@Valid @RequestBody ValoresSolicitud v) {
        hojas.corregir(id(), new ValoresHoja(
                v.nombre(), v.telefono(), v.ciudad(), v.perfil(), v.habilidades(),
                v.experiencia().stream()
                        .map(x -> new ValorExperiencia(x.id(), x.cargo(), x.organizacion(), x.inicio(), x.fin(), x.descripcion()))
                        .toList(),
                v.educacion().stream()
                        .map(x -> new ValorEducacion(x.id(), x.titulo(), x.institucion(), x.inicio(), x.fin()))
                        .toList()));
        return ResponseEntity.noContent().build();
    }

    /** Confirma los datos revisados (el análisis Harvard / ATS llega en la siguiente etapa). */
    @PostMapping("/datos/confirmacion")
    public ResponseEntity<Void> confirmar() {
        hojas.confirmar(id());
        return ResponseEntity.noContent().build();
    }

    private UUID id() {
        return sesion.requerido().id();
    }

    private static ArchivoRespuesta archivo(ArchivoInfo a) {
        return new ArchivoRespuesta(a.nombre(), a.actualizadaEn(), a.idioma(), a.paginas());
    }
}
