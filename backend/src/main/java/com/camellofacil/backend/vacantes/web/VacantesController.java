package com.camellofacil.backend.vacantes.web;

import com.camellofacil.backend.auth.app.SesionActual;
import com.camellofacil.backend.vacantes.app.ModelosVacante.ListaVacantes;
import com.camellofacil.backend.vacantes.app.ModelosVacante.VacanteDetalle;
import com.camellofacil.backend.vacantes.app.ModelosVacante.VacanteResumen;
import com.camellofacil.backend.vacantes.app.VacantesService;
import com.camellofacil.backend.vacantes.app.VacantesService.VacanteNoEncontradaException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Vacantes para la persona que inició sesión. Las guardadas siempre son del usuario de la sesión, nunca de
 * uno que venga en la petición.
 */
@RestController
@RequestMapping("/api/v1/vacantes")
public class VacantesController {

    private final SesionActual sesion;
    private final VacantesService vacantes;

    public VacantesController(SesionActual sesion, VacantesService vacantes) {
        this.sesion = sesion;
        this.vacantes = vacantes;
    }

    public record VacanteRespuesta(
            UUID id,
            String cargo,
            String empresa,
            String tipo,
            String ciudad,
            String modalidad,
            Instant publicadaEn,
            String fuente,
            List<String> carreras) {}

    public record ListaRespuesta(List<VacanteRespuesta> vacantes, List<UUID> guardadas) {}

    public record DetalleRespuesta(
            VacanteRespuesta vacante, String descripcion, List<String> responsabilidades, List<String> requisitos) {}

    @GetMapping
    public ListaRespuesta listar() {
        ListaVacantes lista = vacantes.listar(sesion.requerido().id());
        return new ListaRespuesta(
                lista.vacantes().stream().map(VacantesController::respuesta).toList(), List.copyOf(lista.guardadas()));
    }

    @GetMapping("/{id}")
    public DetalleRespuesta detalle(@PathVariable UUID id) {
        sesion.requerido();
        VacanteDetalle d = vacantes.detalle(id).orElseThrow(VacanteNoEncontradaException::new);
        return new DetalleRespuesta(respuesta(d.resumen()), d.descripcion(), d.responsabilidades(), d.requisitos());
    }

    @PutMapping("/{id}/guardada")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void guardar(@PathVariable UUID id) {
        vacantes.guardar(sesion.requerido().id(), id);
    }

    @DeleteMapping("/{id}/guardada")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void quitarGuardada(@PathVariable UUID id) {
        vacantes.quitarGuardada(sesion.requerido().id(), id);
    }

    @ExceptionHandler(VacanteNoEncontradaException.class)
    ProblemDetail noEncontrada(VacanteNoEncontradaException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problema.setProperty("codigo", "vacante_no_encontrada");
        return problema;
    }

    private static VacanteRespuesta respuesta(VacanteResumen v) {
        return new VacanteRespuesta(
                v.id(),
                v.cargo(),
                v.empresa(),
                v.tipo().name().toLowerCase(Locale.ROOT),
                v.ciudad(),
                v.modalidad().name().toLowerCase(Locale.ROOT),
                v.publicadaEn(),
                v.publicador(),
                v.carreras());
    }
}
