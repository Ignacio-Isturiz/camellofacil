package com.camellofacil.backend.postulaciones.app;

import com.camellofacil.backend.postulaciones.app.ModelosPostulacion.ClicRegistrado;
import com.camellofacil.backend.postulaciones.app.ModelosPostulacion.HojaUsada;
import com.camellofacil.backend.postulaciones.app.ModelosPostulacion.ListaPostulaciones;
import com.camellofacil.backend.postulaciones.app.ModelosPostulacion.Orden;
import com.camellofacil.backend.postulaciones.app.ModelosPostulacion.PostulacionResumen;
import com.camellofacil.backend.postulaciones.persistence.ClicPostulacion;
import com.camellofacil.backend.postulaciones.persistence.ClicPostulacionRepository;
import com.camellofacil.backend.postulaciones.persistence.Postulacion;
import com.camellofacil.backend.postulaciones.persistence.PostulacionRepository;
import com.camellofacil.backend.vacantes.app.ModelosVacante.VacanteParaPostular;
import com.camellofacil.backend.vacantes.app.VacantesService;
import com.camellofacil.backend.vacantes.app.VacantesService.VacanteNoEncontradaException;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Historial de clics en «Postular» de la persona que inició sesión. La postulación termina en la página de
 * la empresa: aquí no hay estados, solo a dónde fue, cuándo, cuántas veces y con qué hoja.
 * Todo se filtra por el usuario de la sesión; un id de otra persona se trata como inexistente.
 */
@Service
public class PostulacionesService {

    /** Ventana del resumen «N clics en postular · últimos 90 días». */
    static final Duration VENTANA_RESUMEN = Duration.ofDays(90);
    /** Lo que dura el «Deshacer» de una postulación quitada; después se borra del todo. */
    static final Duration PLAZO_DESHACER = Duration.ofDays(1);
    /** El historial que se lee de una vez; la búsqueda y el orden se hacen sobre él. */
    static final int MAXIMO_HISTORIAL = 500;
    static final int MAXIMO_TEXTO_BUSCADO = 100;

    private final PostulacionRepository postulaciones;
    private final ClicPostulacionRepository clics;
    private final VacantesService vacantes;
    private final Clock reloj;

    public PostulacionesService(
            PostulacionRepository postulaciones,
            ClicPostulacionRepository clics,
            VacantesService vacantes,
            Clock reloj) {
        this.postulaciones = postulaciones;
        this.clics = clics;
        this.vacantes = vacantes;
        this.reloj = reloj;
    }

    /** Clic en «Postular» desde una vacante. */
    @Transactional
    public ClicRegistrado clicEnVacante(UUID usuarioId, UUID vacanteId, HojaUsada hoja) {
        VacanteParaPostular v = vacantes.paraPostular(vacanteId).orElseThrow(VacanteNoEncontradaException::new);
        return registrar(usuarioId, v, hoja);
    }

    /** «Ver vacante» desde el historial: vuelve a la página original y cuenta un clic más con la misma hoja. */
    @Transactional
    public ClicRegistrado clicDesdeHistorial(UUID usuarioId, UUID postulacionId) {
        Postulacion p = propia(usuarioId, postulacionId);
        VacanteParaPostular v =
                vacantes.paraPostular(p.getVacanteId()).orElseThrow(PostulacionNoEncontradaException::new);
        return registrar(usuarioId, v, HojaUsada.valueOf(p.getHojaUsada()));
    }

    @Transactional(readOnly = true)
    public ListaPostulaciones listar(UUID usuarioId, String texto, Orden orden) {
        String buscado = normalizar(texto);
        List<PostulacionResumen> items = postulaciones
                .findByUsuarioIdAndQuitadaEnIsNullOrderByUltimoClicEnDesc(usuarioId, Limit.of(MAXIMO_HISTORIAL))
                .stream()
                .filter(p -> buscado.isEmpty()
                        || normalizar(p.getCargo()).contains(buscado)
                        || normalizar(p.getEmpresa()).contains(buscado))
                .map(PostulacionesService::resumen)
                .sorted(comparador(orden))
                .toList();
        long recientes = clics.contarDesde(usuarioId, reloj.instant().minus(VENTANA_RESUMEN));
        return new ListaPostulaciones(items, recientes);
    }

    /** Las vacantes a las que la persona ya fue a postularse (para no recomendárselas otra vez). */
    @Transactional(readOnly = true)
    public Set<UUID> vacantesPostuladas(UUID usuarioId) {
        return new HashSet<>(postulaciones.vacantesPostuladas(usuarioId));
    }

    @Transactional
    public void quitar(UUID usuarioId, UUID postulacionId) {
        propia(usuarioId, postulacionId).quitar(reloj.instant());
    }

    /** Deshace «Quitar» mientras no se haya borrado del todo. */
    @Transactional
    public void restaurar(UUID usuarioId, UUID postulacionId) {
        propia(usuarioId, postulacionId).restaurar();
    }

    /** Borra del todo las quitadas hace más de un día: lo que la persona quitó no se guarda indefinidamente. */
    @Transactional
    public int borrarQuitadas() {
        return postulaciones.borrarQuitadas(reloj.instant().minus(PLAZO_DESHACER));
    }

    private ClicRegistrado registrar(UUID usuarioId, VacanteParaPostular v, HojaUsada hoja) {
        Instant ahora = reloj.instant();
        postulaciones.registrarClic(
                UUID.randomUUID(),
                usuarioId,
                v.id(),
                v.cargo(),
                v.empresa(),
                v.ciudad(),
                v.modalidad().name(),
                v.tipo().name(),
                hoja.name(),
                ahora);
        Postulacion p = postulaciones.findByUsuarioIdAndVacanteId(usuarioId, v.id()).orElseThrow();
        clics.save(new ClicPostulacion(UUID.randomUUID(), p.getId(), ahora));
        return new ClicRegistrado(p.getId(), v.url());
    }

    private Postulacion propia(UUID usuarioId, UUID postulacionId) {
        return postulaciones.findByIdAndUsuarioId(postulacionId, usuarioId)
                .orElseThrow(PostulacionNoEncontradaException::new);
    }

    private static PostulacionResumen resumen(Postulacion p) {
        return new PostulacionResumen(
                p.getId(),
                p.getVacanteId(),
                p.getCargo(),
                p.getEmpresa(),
                p.getCiudad(),
                p.getModalidad(),
                p.getTipo(),
                p.getUltimoClicEn(),
                p.getClics(),
                HojaUsada.valueOf(p.getHojaUsada()));
    }

    private static Comparator<PostulacionResumen> comparador(Orden orden) {
        Comparator<PostulacionResumen> porFecha = Comparator.comparing(PostulacionResumen::ultimoClicEn);
        Comparator<PostulacionResumen> porCargo = Comparator.comparing(r -> normalizar(r.cargo()));
        return switch (orden) {
            case RECIENTES -> porFecha.reversed();
            case ANTIGUAS -> porFecha;
            case CARGO_AZ -> porCargo.thenComparing(porFecha.reversed());
            case CARGO_ZA -> porCargo.reversed().thenComparing(porFecha.reversed());
        };
    }

    /** Sin tildes ni mayúsculas, para que «ingenieria» encuentre «Ingeniería». */
    static String normalizar(String texto) {
        if (texto == null) return "";
        String recortado = texto.strip();
        if (recortado.length() > MAXIMO_TEXTO_BUSCADO) recortado = recortado.substring(0, MAXIMO_TEXTO_BUSCADO);
        return Normalizer.normalize(recortado, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    /** La postulación no existe o es de otra persona. La capa web la traduce a 404. */
    public static class PostulacionNoEncontradaException extends RuntimeException {
        public PostulacionNoEncontradaException() {
            super("La postulación no existe");
        }
    }
}
