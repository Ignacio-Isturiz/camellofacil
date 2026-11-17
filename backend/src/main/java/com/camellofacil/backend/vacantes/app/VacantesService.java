package com.camellofacil.backend.vacantes.app;

import com.camellofacil.backend.vacantes.app.ModelosVacante.ListaVacantes;
import com.camellofacil.backend.vacantes.app.ModelosVacante.Modalidad;
import com.camellofacil.backend.vacantes.app.ModelosVacante.TipoVacante;
import com.camellofacil.backend.vacantes.app.ModelosVacante.VacanteDetalle;
import com.camellofacil.backend.vacantes.app.ModelosVacante.VacanteParaPostular;
import com.camellofacil.backend.vacantes.app.ModelosVacante.VacanteResumen;
import com.camellofacil.backend.vacantes.persistence.MarcaVacante;
import com.camellofacil.backend.vacantes.persistence.MarcaVacanteRepository;
import com.camellofacil.backend.vacantes.persistence.Vacante;
import com.camellofacil.backend.vacantes.persistence.VacanteRepository;
import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Consulta de vacantes y vacantes guardadas de la persona que inició sesión. */
@Service
public class VacantesService {

    /** Las vacantes abiertas más recientes que se envían de una vez; el filtrado fino lo hace la interfaz. */
    static final int MAXIMO_LISTA = 500;

    private static final String GUARDADA = "GUARDADA";
    private static final TypeReference<List<String>> LISTA_TEXTOS = new TypeReference<>() {};

    private final VacanteRepository vacantes;
    private final MarcaVacanteRepository marcas;
    private final JsonMapper json;
    private final Clock reloj;

    public VacantesService(VacanteRepository vacantes, MarcaVacanteRepository marcas, JsonMapper json, Clock reloj) {
        this.vacantes = vacantes;
        this.marcas = marcas;
        this.json = json;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public ListaVacantes listar(UUID usuarioId) {
        return new ListaVacantes(abiertas(), new HashSet<>(marcas.vacantesMarcadas(usuarioId, GUARDADA)));
    }

    /** Las abiertas más recientes (hasta {@value #MAXIMO_LISTA}), de la más nueva a la más antigua. */
    @Transactional(readOnly = true)
    public List<VacanteResumen> abiertas() {
        return vacantes.abiertas(Limit.of(MAXIMO_LISTA)).stream().map(VacantesService::resumen).toList();
    }

    /** Detalle de una vacante (también de una cerrada: la persona puede tenerla guardada). */
    @Transactional(readOnly = true)
    public Optional<VacanteDetalle> detalle(UUID vacanteId) {
        return vacantes.findById(vacanteId).map(v -> new VacanteDetalle(
                resumen(v), v.getDescripcion(), lista(v.getResponsabilidades()), lista(v.getRequisitos())));
    }

    /** Para el módulo de postulaciones: los datos de la vacante y su página original (también si está cerrada). */
    @Transactional(readOnly = true)
    public Optional<VacanteParaPostular> paraPostular(UUID vacanteId) {
        return vacantes.findById(vacanteId).map(v -> new VacanteParaPostular(
                v.getId(),
                v.getCargo(),
                v.getEmpresa(),
                v.getCiudad(),
                Modalidad.valueOf(v.getModalidad()),
                TipoVacante.valueOf(v.getTipo()),
                v.getUrlPostulacion()));
    }

    @Transactional
    public void guardar(UUID usuarioId, UUID vacanteId) {
        marcar(usuarioId, vacanteId, GUARDADA);
    }

    @Transactional
    public void quitarGuardada(UUID usuarioId, UUID vacanteId) {
        marcas.deleteById(new MarcaVacante.Clave(usuarioId, vacanteId, GUARDADA));
    }

    private void marcar(UUID usuarioId, UUID vacanteId, String tipo) {
        if (!vacantes.existsById(vacanteId)) throw new VacanteNoEncontradaException();
        MarcaVacante.Clave clave = new MarcaVacante.Clave(usuarioId, vacanteId, tipo);
        if (!marcas.existsById(clave)) marcas.save(new MarcaVacante(clave, reloj.instant()));
    }

    private static VacanteResumen resumen(Vacante v) {
        return new VacanteResumen(
                v.getId(),
                v.getCargo(),
                v.getEmpresa(),
                TipoVacante.valueOf(v.getTipo()),
                v.getCiudad(),
                Modalidad.valueOf(v.getModalidad()),
                v.getPublicadaEn(),
                v.getPublicador(),
                List.of(v.getCarreras()));
    }

    private List<String> lista(String jsonLista) {
        if (jsonLista == null) return List.of();
        try {
            return json.readValue(jsonLista, LISTA_TEXTOS);
        } catch (JacksonException e) {
            return List.of();
        }
    }

    /** La vacante no existe. Excepción simple: la capa web la traduce a 404. */
    public static class VacanteNoEncontradaException extends RuntimeException {
        public VacanteNoEncontradaException() {
            super("La vacante no existe");
        }
    }
}
