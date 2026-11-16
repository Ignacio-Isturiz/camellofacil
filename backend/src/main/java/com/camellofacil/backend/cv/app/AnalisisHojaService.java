package com.camellofacil.backend.cv.app;

import com.camellofacil.backend.cv.app.ExtractorIa.Extraccion;
import com.camellofacil.backend.cv.app.ExtractorIa.ExtractorIaException;
import com.camellofacil.backend.cv.app.LectorDocumentos.ArchivoIlegibleException;
import com.camellofacil.backend.cv.app.LectorDocumentos.DocumentoLeido;
import com.camellofacil.backend.cv.app.ModelosHoja.DatosHoja;
import com.camellofacil.backend.cv.app.ModelosHoja.EstadoHoja;
import com.camellofacil.backend.cv.app.ModelosHoja.TipoArchivo;
import com.camellofacil.backend.cv.persistence.ArchivoHojaRepository;
import com.camellofacil.backend.cv.persistence.HojaDeVida;
import com.camellofacil.backend.cv.persistence.HojaDeVidaRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/**
 * Análisis en segundo plano: toma la siguiente hoja de la cola (tabla hoja_de_vida, estado ANALIZANDO), saca
 * su texto, pide los datos a la IA y guarda el resultado. Nada vive en memoria: si el servidor se reinicia,
 * la reserva vence y otra ejecución la retoma.
 */
@Service
public class AnalisisHojaService {

    private static final Logger LOG = LoggerFactory.getLogger(AnalisisHojaService.class);

    static final int MAXIMO_INTENTOS = 3;
    /** Tiempo que una ejecución reserva la hoja; si se cae a mitad, después de esto se reintenta. */
    private static final Duration RESERVA = Duration.ofMinutes(5);
    private static final Duration ESPERA_REINTENTO = Duration.ofSeconds(30);

    private final HojaDeVidaRepository hojas;
    private final ArchivoHojaRepository archivos;
    private final LectorDocumentos lector;
    private final ExtractorIa extractor;
    private final TransactionTemplate transaccion;
    private final JsonMapper json;

    public AnalisisHojaService(
            HojaDeVidaRepository hojas,
            ArchivoHojaRepository archivos,
            LectorDocumentos lector,
            ExtractorIa extractor,
            TransactionTemplate transaccion,
            JsonMapper json) {
        this.hojas = hojas;
        this.archivos = archivos;
        this.lector = lector;
        this.extractor = extractor;
        this.transaccion = transaccion;
        this.json = json;
    }

    private record Reserva(UUID usuarioId, Instant subidaEn, TipoArchivo tipo, int intento) {}

    /** Procesa una hoja si hay alguna pendiente. Devuelve true si procesó (o intentó) una. */
    public boolean procesarSiguiente() {
        // 1. Reserva corta (transacción propia): nadie más la toma mientras se analiza.
        Optional<Reserva> reserva = transaccion.execute(estado -> hojas.siguientePorAnalizar().map(h -> {
            h.reservar(Instant.now().plus(RESERVA));
            return new Reserva(h.getUsuarioId(), h.getSubidaEn(), TipoArchivo.valueOf(h.getTipoArchivo()), h.getIntentos());
        }));
        if (reserva == null || reserva.isEmpty()) return false;
        Reserva r = reserva.get();

        // 2. Lectura y llamada a la IA fuera de la transacción (puede tardar varios segundos).
        try {
            byte[] contenido = archivos.findById(r.usuarioId()).orElseThrow().getContenido();
            DocumentoLeido documento = lector.leer(contenido, r.tipo());
            Extraccion extraccion = extractor.extraer(documento.texto());
            DatosHoja datos = ArmadorDatos.armar(extraccion, documento.texto());
            guardar(r, h -> h.resultado(
                    EstadoHoja.SIN_CONFIRMAR.name(),
                    ArmadorDatos.idioma(extraccion),
                    (short) Math.min(documento.paginas(), Short.MAX_VALUE),
                    json.writeValueAsString(datos),
                    Instant.now()));
        } catch (ArchivoIlegibleException e) {
            LOG.info("Hoja de vida ilegible (usuario {})", r.usuarioId());
            guardar(r, h -> h.resultado(EstadoHoja.ILEGIBLE.name(), null, null, null, Instant.now()));
        } catch (ExtractorIaException | RuntimeException e) {
            // Sin datos personales en el log: solo el id y el tipo de error.
            LOG.warn("Falló el análisis de la hoja (usuario {}, intento {}): {}", r.usuarioId(), r.intento(), e.toString());
            guardar(r, h -> {
                if (r.intento() >= MAXIMO_INTENTOS) {
                    h.resultado(EstadoHoja.ERROR.name(), null, null, null, Instant.now());
                } else {
                    h.reintentarEn(Instant.now().plus(ESPERA_REINTENTO.multipliedBy(r.intento())));
                }
            });
        }
        return true;
    }

    /** 3. Guarda el resultado solo si la persona no subió otro archivo mientras tanto. */
    private void guardar(Reserva r, java.util.function.Consumer<HojaDeVida> cambio) {
        transaccion.executeWithoutResult(estado -> hojas.findById(r.usuarioId())
                .filter(h -> h.getSubidaEn().equals(r.subidaEn())
                        && EstadoHoja.ANALIZANDO.name().equals(h.getEstado()))
                .ifPresent(cambio));
    }
}
