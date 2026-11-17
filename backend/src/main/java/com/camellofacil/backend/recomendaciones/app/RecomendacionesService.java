package com.camellofacil.backend.recomendaciones.app;

import com.camellofacil.backend.perfil.app.ModelosPerfil.PerfilDatos;
import com.camellofacil.backend.perfil.app.PerfilService;
import com.camellofacil.backend.postulaciones.app.PostulacionesService;
import com.camellofacil.backend.vacantes.app.ModelosVacante.VacanteResumen;
import com.camellofacil.backend.vacantes.app.VacantesService;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** «Recomendadas hoy» de Inicio, según la perfilación de quien inició sesión, de a una página por vez. */
@Service
public class RecomendacionesService {

    /** Recomendadas por página en Inicio. */
    static final int TAMANO_PAGINA = 4;

    private final PerfilService perfiles;
    private final VacantesService vacantes;
    private final PostulacionesService postulaciones;
    private final Clock reloj;

    public RecomendacionesService(
            PerfilService perfiles, VacantesService vacantes, PostulacionesService postulaciones, Clock reloj) {
        this.perfiles = perfiles;
        this.vacantes = vacantes;
        this.postulaciones = postulaciones;
        this.reloj = reloj;
    }

    public record Recomendada(VacanteResumen vacante, boolean nueva) {}

    /**
     * Una página de recomendadas. {@code pagina} empieza en 0; si pide una que ya no existe (porque hay menos
     * recomendadas que antes), se devuelve la última.
     */
    public record ResumenInicio(
            int vacantesNuevas, List<Recomendada> recomendadas, int pagina, int totalPaginas, int total) {}

    /** No se recomienda lo que la persona ya fue a postular. */
    @Transactional(readOnly = true)
    public ResumenInicio resumen(UUID usuarioId, int pagina) {
        PerfilDatos perfil = perfiles.obtener(usuarioId).orElse(null);
        Set<UUID> postuladas = postulaciones.vacantesPostuladas(usuarioId);
        Recomendador.Resultado r =
                Recomendador.recomendar(perfil, vacantes.abiertas(), postuladas, reloj.instant(), Integer.MAX_VALUE);

        int total = r.recomendadas().size();
        int totalPaginas = Math.max(1, (total + TAMANO_PAGINA - 1) / TAMANO_PAGINA);
        int actual = Math.min(Math.max(0, pagina), totalPaginas - 1);
        List<Recomendada> pedidas = r.recomendadas().stream()
                .skip((long) actual * TAMANO_PAGINA)
                .limit(TAMANO_PAGINA)
                .map(x -> new Recomendada(x.vacante(), x.nueva()))
                .toList();
        return new ResumenInicio(r.nuevasDeLaSemana(), pedidas, actual, totalPaginas, total);
    }
}
