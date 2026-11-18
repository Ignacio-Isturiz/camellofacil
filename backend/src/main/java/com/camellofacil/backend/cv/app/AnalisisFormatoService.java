package com.camellofacil.backend.cv.app;

import com.camellofacil.backend.cv.app.HojaException.Motivo;
import com.camellofacil.backend.cv.app.ModelosAnalisis.Analisis;
import com.camellofacil.backend.cv.app.ModelosAnalisis.Observacion;
import com.camellofacil.backend.cv.app.ModelosAnalisis.PuntajeAts;
import com.camellofacil.backend.cv.app.ModelosAnalisis.RecomendacionIa;
import com.camellofacil.backend.cv.app.ModelosHoja.EstadoHoja;
import com.camellofacil.backend.cv.app.ModelosHoja.ValoresHoja;
import com.camellofacil.backend.cv.app.RecomendadorIa.RecomendadorIaException;
import com.camellofacil.backend.cv.persistence.RecomendacionHojaIa;
import com.camellofacil.backend.cv.persistence.RecomendacionHojaIaRepository;
import com.camellofacil.backend.legal.app.ConsentimientoService;
import com.camellofacil.backend.legal.app.TipoConsentimiento;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Análisis Harvard / ATS de la hoja confirmada: puntajes y observaciones por reglas (siempre, sin IA) más las
 * recomendaciones de la IA, que se piden aparte porque tardan unos segundos y se guardan mientras la hoja no cambie.
 */
@Service
public class AnalisisFormatoService {

    /** Generaciones de la IA por persona y día (cada vez que la hoja cambia hay que pedirlas de nuevo). */
    static final int MAXIMO_POR_DIA = 5;
    static final String OPCION_ATS_INICIAL = "general";

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final Pattern ENLACE = Pattern.compile("(?i)https?://|www\\.");
    private static final TypeReference<List<RecomendacionIa>> LISTA_RECOMENDACIONES = new TypeReference<>() {};

    private final HojaDeVidaService hojas;
    private final RecomendadorIa ia;
    private final RecomendacionHojaIaRepository recomendaciones;
    private final ConsentimientoService consentimientos;
    private final JsonMapper json;
    private final Clock reloj;

    public AnalisisFormatoService(
            HojaDeVidaService hojas,
            RecomendadorIa ia,
            RecomendacionHojaIaRepository recomendaciones,
            ConsentimientoService consentimientos,
            JsonMapper json,
            Clock reloj) {
        this.hojas = hojas;
        this.ia = ia;
        this.recomendaciones = recomendaciones;
        this.consentimientos = consentimientos;
        this.json = json;
        this.reloj = reloj;
    }

    /** El análisis de la hoja confirmada. Sin hoja confirmada no hay nada que analizar. */
    @Transactional(readOnly = true)
    public Analisis analizar(UUID usuarioId) {
        ValoresHoja v = confirmada(usuarioId);
        List<Observacion> observaciones = new ArrayList<>(EvaluadorHoja.observaciones(v));
        List<RecomendacionIa> deIa = vigentes(usuarioId);
        String estadoIa;
        if (deIa != null) {
            estadoIa = "lista";
            for (int i = 0; i < deIa.size(); i++) {
                RecomendacionIa r = deIa.get(i);
                observaciones.add(new Observacion("ia-" + (i + 1), "mejorar", r.titulo(), r.detalle(), "ia"));
            }
        } else {
            estadoIa = consentimientos.vigente(usuarioId, TipoConsentimiento.ANALISIS_IA).isPresent() ? "pendiente" : "no_disponible";
        }
        return new Analisis(
                EvaluadorHoja.harvard(v),
                EvaluadorHoja.ats(v, OPCION_ATS_INICIAL).orElseThrow(),
                EvaluadorHoja.OPCIONES_ATS,
                observaciones,
                estadoIa);
    }

    /** El puntaje ATS con otra opción de evaluación. Vacío si la opción no existe. */
    @Transactional(readOnly = true)
    public java.util.Optional<PuntajeAts> puntajeAts(UUID usuarioId, String opcionId) {
        return EvaluadorHoja.ats(confirmada(usuarioId), opcionId);
    }

    /**
     * Pide las recomendaciones a la IA. Si ya hay unas vigentes (la hoja no cambió) no vuelve a llamarla. El gasto
     * del cupo diario y la llamada van en la misma transacción: si la IA falla, no se gasta nada.
     */
    @Transactional
    public Analisis generarRecomendaciones(UUID usuarioId) {
        ValoresHoja v = confirmada(usuarioId);
        if (consentimientos.vigente(usuarioId, TipoConsentimiento.ANALISIS_IA).isEmpty()) {
            throw new HojaException(Motivo.SIN_AUTORIZACION_IA, "Necesitamos tu autorización de análisis con IA");
        }
        if (vigentes(usuarioId) == null) {
            Instant actualizadaEn = hojas.resumen(usuarioId).orElseThrow().archivo().actualizadaEn();
            LocalDate hoy = LocalDate.now(reloj.withZone(ZONA));
            RecomendacionHojaIa existente = recomendaciones.findById(usuarioId).orElse(null);
            int intentos = existente != null && hoy.equals(existente.getDia()) ? existente.getIntentosHoy() : 0;
            if (intentos >= MAXIMO_POR_DIA) {
                throw new HojaException(Motivo.LIMITE, "Ya pediste recomendaciones " + MAXIMO_POR_DIA + " veces hoy. Vuelve a intentar mañana");
            }
            List<RecomendacionIa> nuevas;
            try {
                nuevas = limpiar(ia.recomendar(sinContacto(v)));
            } catch (RecomendadorIaException e) {
                throw new HojaException(Motivo.IA_NO_DISPONIBLE, "No pudimos generar las recomendaciones ahora. Intenta de nuevo en un momento");
            }
            String items = json.writeValueAsString(nuevas);
            if (existente == null) {
                recomendaciones.save(new RecomendacionHojaIa(usuarioId, actualizadaEn, items, hoy, intentos + 1, Instant.now()));
            } else {
                existente.reemplazar(actualizadaEn, items, hoy, intentos + 1, Instant.now());
            }
        }
        return analizar(usuarioId);
    }

    /** Las recomendaciones guardadas, solo si son de la hoja actual; null si hay que pedirlas. */
    private List<RecomendacionIa> vigentes(UUID usuarioId) {
        Instant actualizadaEn = hojas.resumen(usuarioId).orElseThrow().archivo().actualizadaEn();
        return recomendaciones.findById(usuarioId)
                .filter(r -> !r.getHojaActualizadaEn().isBefore(actualizadaEn))
                .map(r -> json.readValue(r.getItems(), LISTA_RECOMENDACIONES))
                .orElse(null);
    }

    private ValoresHoja confirmada(UUID usuarioId) {
        EstadoHoja estado = hojas.estado(usuarioId)
                .orElseThrow(() -> new HojaException(Motivo.SIN_HOJA, "Aún no has subido tu hoja de vida"));
        if (estado != EstadoHoja.CONFIRMADA) {
            throw new HojaException(Motivo.ESTADO_INVALIDO, "Confirma los datos de tu hoja para ver el análisis");
        }
        return hojas.valores(usuarioId);
    }

    /** Minimiza datos: la IA no recibe nombre, teléfono ni ciudad. */
    private static ValoresHoja sinContacto(ValoresHoja v) {
        return new ValoresHoja("", "", "", v.perfil(), v.habilidades(), v.experiencia(), v.educacion());
    }

    /** Hasta 5 recomendaciones de largo razonable y sin enlaces: es texto que se le muestra a la persona. */
    static List<RecomendacionIa> limpiar(List<RecomendacionIa> crudas) {
        List<RecomendacionIa> limpias = new ArrayList<>();
        for (RecomendacionIa r : crudas) {
            if (r == null || limpias.size() >= 5) continue;
            String titulo = texto(r.titulo(), 80);
            String detalle = texto(r.detalle(), 240);
            if (titulo.isEmpty() || detalle.isEmpty() || ENLACE.matcher(titulo + " " + detalle).find()) continue;
            limpias.add(new RecomendacionIa(titulo, detalle));
        }
        return limpias;
    }

    private static String texto(String s, int maximo) {
        String t = s == null ? "" : s.replaceAll("\\p{Cntrl}", " ").replaceAll(" {2,}", " ").strip();
        return t.length() > maximo ? t.substring(0, maximo).strip() : t;
    }
}
