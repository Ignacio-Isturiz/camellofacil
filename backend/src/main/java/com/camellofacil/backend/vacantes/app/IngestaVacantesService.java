package com.camellofacil.backend.vacantes.app;

import com.camellofacil.backend.vacantes.app.FuenteVacantes.FuenteException;
import com.camellofacil.backend.vacantes.app.ModelosVacante.Modalidad;
import com.camellofacil.backend.vacantes.app.ModelosVacante.VacanteExterna;
import com.camellofacil.backend.vacantes.app.ModelosVacante.VacanteNueva;
import com.camellofacil.backend.vacantes.persistence.ConsultaVacantesRepository;
import com.camellofacil.backend.vacantes.persistence.UsoFuente;
import com.camellofacil.backend.vacantes.persistence.UsoFuenteRepository;
import com.camellofacil.backend.vacantes.persistence.VacanteRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/**
 * Trae vacantes de las fuentes sin pasarse de su cupo. En cada ronda, cada fuente hace como mucho una
 * petición, y solo si le queda cupo para este momento del periodo (el cupo se reparte parejo en el tiempo).
 * La búsqueda que se lanza es la que más le toca según la prioridad de su carrera.
 *
 * <p>Todo el estado (cupo gastado, última ejecución de cada búsqueda) vive en Postgres: si el servidor se
 * reinicia o hay dos copias, el cupo se respeta igual.
 */
@Service
public class IngestaVacantesService {

    private static final Logger LOG = LoggerFactory.getLogger(IngestaVacantesService.class);

    /** Llave rechazada: se vuelve a intentar al día siguiente (no tiene sentido gastar peticiones). */
    static final Duration PAUSA_AUTORIZACION = Duration.ofHours(24);
    /** El proveedor dijo que llegamos a su límite: se espera una hora. */
    static final Duration PAUSA_LIMITE = Duration.ofHours(1);
    /** RF-34: una vacante que no vuelve a aparecer en 21 días, o publicada hace más de 45, se cierra. */
    static final Duration CERRAR_SIN_VER = Duration.ofDays(21);
    static final Duration CERRAR_PUBLICADA = Duration.ofDays(45);

    private final List<FuenteVacantes> fuentes;
    private final UsoFuenteRepository usos;
    private final ConsultaVacantesRepository consultas;
    private final VacanteRepository vacantes;
    private final TransactionTemplate transaccion;
    private final JsonMapper json;
    private final Clock reloj;

    public IngestaVacantesService(
            List<FuenteVacantes> fuentes,
            UsoFuenteRepository usos,
            ConsultaVacantesRepository consultas,
            VacanteRepository vacantes,
            TransactionTemplate transaccion,
            JsonMapper json,
            Clock reloj) {
        this.fuentes = fuentes;
        this.usos = usos;
        this.consultas = consultas;
        this.vacantes = vacantes;
        this.transaccion = transaccion;
        this.json = json;
        this.reloj = reloj;
    }

    /** Tope de peticiones seguidas a una misma fuente en una ronda (p. ej. la carga inicial de JSearch). */
    static final int MAXIMO_POR_RONDA = 10;

    /**
     * Qué pasó con una petición. {@code consulto} es falso si no le tocaba (sin cupo, pausada...);
     * {@code fallo} es verdadero si la fuente respondió con error.
     */
    public record Resultado(String fuente, boolean consulto, boolean fallo, int recibidas, int guardadas) {}

    private record Turno(String texto, String carrera, int consumidas, int limite) {}

    /**
     * Una ronda: cada fuente configurada hace las peticiones que su cupo permita en este momento (normalmente
     * una o ninguna; varias en la carga inicial), y luego se cierran las vacantes vencidas. Si una petición
     * falla, esa fuente no sigue en esta ronda.
     */
    public List<Resultado> ronda() {
        List<Resultado> resultados = new ArrayList<>();
        for (FuenteVacantes fuente : fuentes) {
            if (!fuente.configurada()) continue;
            try {
                for (int i = 0; i < MAXIMO_POR_RONDA; i++) {
                    Resultado r = consultar(fuente);
                    resultados.add(r);
                    if (!r.consulto() || r.fallo()) break;
                }
            } catch (RuntimeException e) {
                LOG.warn("Vacantes: error inesperado con la fuente {}: {}", fuente.codigo(), e.toString());
            }
        }
        cerrarVencidas();
        return resultados;
    }

    private Resultado consultar(FuenteVacantes fuente) {
        Instant ahora = reloj.instant();
        // 1. Reserva (transacción corta, cupo bloqueado): se descuenta la petición ANTES de hacerla, para que
        //    un fallo a mitad de camino nunca deje gastar más de lo permitido.
        Optional<Turno> turno = transaccion.execute(estado -> reservar(fuente, ahora));
        if (turno == null || turno.isEmpty()) return new Resultado(fuente.codigo(), false, false, 0, 0);
        Turno t = turno.get();

        // 2. La petición, fuera de la transacción.
        List<VacanteExterna> recibidas;
        try {
            recibidas = fuente.buscar(t.texto());
        } catch (FuenteException e) {
            LOG.warn("Vacantes: la fuente {} falló ({}): {}", fuente.codigo(), e.motivo(), e.getMessage());
            Duration pausa = switch (e.motivo()) {
                case AUTORIZACION -> PAUSA_AUTORIZACION;
                case LIMITE -> PAUSA_LIMITE;
                case OTRO -> null;
            };
            if (pausa != null) {
                transaccion.executeWithoutResult(estado ->
                        usos.bloquear(fuente.codigo()).ifPresent(u -> u.pausarHasta(ahora.plus(pausa))));
            }
            return new Resultado(fuente.codigo(), true, true, 0, 0);
        }

        // 3. Se guardan (sin duplicados).
        Instant guardadoEn = reloj.instant();
        List<VacanteNueva> nuevas = recibidas.stream()
                .map(e -> Clasificador.clasificar(fuente.codigo(), t.carrera(), e, guardadoEn))
                .flatMap(Optional::stream)
                .filter(v -> !fuente.soloRemotas() || v.modalidad() == Modalidad.REMOTO)
                .filter(Clasificador::disponibleDesdeMedellin)
                .toList();
        transaccion.executeWithoutResult(estado -> nuevas.forEach(v -> guardar(v, guardadoEn)));
        LOG.info("Vacantes: {} «{}» -> {} recibidas, {} válidas (cupo {}/{})",
                fuente.codigo(), t.texto(), recibidas.size(), nuevas.size(), t.consumidas(), t.limite());
        return new Resultado(fuente.codigo(), true, false, recibidas.size(), nuevas.size());
    }

    private Optional<Turno> reservar(FuenteVacantes fuente, Instant ahora) {
        String codigo = fuente.codigo();
        // Carga inicial: al empezar cada periodo, cada búsqueda puede correr una vez de inmediato; el resto del
        // cupo se reparte en el periodo. El total nunca pasa del límite.
        FuenteVacantes.Cupo base = fuente.cupo();
        int adelanto = Math.max(base.adelanto(), (int) consultas.countByFuenteAndActivaTrue(codigo));
        FuenteVacantes.Cupo cupo = new FuenteVacantes.Cupo(base.limite(), base.periodo(), adelanto);
        usos.crearSiNoExiste(codigo, ahora);
        UsoFuente uso = usos.bloquear(codigo).orElseThrow();

        // El proveedor reinicia el cupo cada periodo (30 días JSearch, 1 día Jooble).
        Duration transcurrido = Duration.between(uso.getPeriodoInicio(), ahora);
        if (transcurrido.compareTo(cupo.periodo()) >= 0) {
            long periodos = transcurrido.toMillis() / cupo.periodo().toMillis();
            uso.nuevoPeriodo(uso.getPeriodoInicio().plus(cupo.periodo().multipliedBy(periodos)));
        }
        if (uso.getPausadaHasta() != null && ahora.isBefore(uso.getPausadaHasta())) return Optional.empty();
        if (uso.getConsumidas() >= cupo.permitidasHasta(uso.getPeriodoInicio(), ahora)) return Optional.empty();

        return consultas.siguiente(codigo, ahora).map(consulta -> {
            uso.consumir();
            consulta.ejecutada(ahora);
            return new Turno(consulta.getTexto(), consulta.getCarreraCodigo(), uso.getConsumidas(), cupo.limite());
        });
    }

    private void guardar(VacanteNueva v, Instant ahora) {
        vacantes.guardarOActualizar(
                UUID.randomUUID(),
                v.fuente(),
                v.idExterno(),
                v.huella(),
                v.cargo(),
                v.empresa(),
                v.ciudad(),
                v.modalidad().name(),
                v.tipo().name(),
                v.publicador(),
                v.url(),
                v.descripcion(),
                json.writeValueAsString(v.responsabilidades()),
                json.writeValueAsString(v.requisitos()),
                String.join(",", v.carreras()),
                v.publicadaEn(),
                ahora);
    }

    private void cerrarVencidas() {
        Instant ahora = reloj.instant();
        Integer cerradas = transaccion.execute(estado ->
                vacantes.cerrarVencidas(ahora, ahora.minus(CERRAR_SIN_VER), ahora.minus(CERRAR_PUBLICADA)));
        if (cerradas != null && cerradas > 0) LOG.info("Vacantes: {} cerradas por vencidas", cerradas);
    }
}
