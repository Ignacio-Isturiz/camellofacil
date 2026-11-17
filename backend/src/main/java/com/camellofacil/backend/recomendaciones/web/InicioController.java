package com.camellofacil.backend.recomendaciones.web;

import com.camellofacil.backend.auth.app.SesionActual;
import com.camellofacil.backend.recomendaciones.app.RecomendacionesService;
import com.camellofacil.backend.recomendaciones.app.RecomendacionesService.Recomendada;
import com.camellofacil.backend.recomendaciones.app.RecomendacionesService.ResumenInicio;
import com.camellofacil.backend.vacantes.app.ModelosVacante.VacanteResumen;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Inicio de quien inició sesión: sus recomendadas de hoy, de a 4 por página. */
@RestController
public class InicioController {

    private final SesionActual sesion;
    private final RecomendacionesService recomendaciones;

    public InicioController(SesionActual sesion, RecomendacionesService recomendaciones) {
        this.sesion = sesion;
        this.recomendaciones = recomendaciones;
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

    public record RecomendadaRespuesta(VacanteRespuesta vacante, boolean nueva) {}

    /** {@code busqueda} queda en null hasta que exista «¿Qué estás buscando?». {@code pagina} empieza en 0. */
    public record InicioRespuesta(
            int vacantesNuevas,
            List<RecomendadaRespuesta> recomendadas,
            int pagina,
            int totalPaginas,
            int total,
            Object busqueda) {}

    @GetMapping("/api/v1/inicio")
    public InicioRespuesta inicio(@RequestParam(name = "pagina", defaultValue = "0") int pagina) {
        ResumenInicio r = recomendaciones.resumen(sesion.requerido().id(), pagina);
        return new InicioRespuesta(
                r.vacantesNuevas(),
                r.recomendadas().stream().map(InicioController::respuesta).toList(),
                r.pagina(),
                r.totalPaginas(),
                r.total(),
                null);
    }

    private static RecomendadaRespuesta respuesta(Recomendada r) {
        VacanteResumen v = r.vacante();
        return new RecomendadaRespuesta(
                new VacanteRespuesta(
                        v.id(),
                        v.cargo(),
                        v.empresa(),
                        v.tipo().name().toLowerCase(Locale.ROOT),
                        v.ciudad(),
                        v.modalidad().name().toLowerCase(Locale.ROOT),
                        v.publicadaEn(),
                        v.publicador(),
                        v.carreras()),
                r.nueva());
    }
}
