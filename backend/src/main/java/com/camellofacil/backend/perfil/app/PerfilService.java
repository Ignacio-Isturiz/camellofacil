package com.camellofacil.backend.perfil.app;

import com.camellofacil.backend.legal.app.ConsentimientoService;
import com.camellofacil.backend.legal.app.TipoConsentimiento;
import com.camellofacil.backend.perfil.app.ModelosPerfil.CarreraDatos;
import com.camellofacil.backend.perfil.app.ModelosPerfil.DatosBasicos;
import com.camellofacil.backend.perfil.app.ModelosPerfil.Disponibilidad;
import com.camellofacil.backend.perfil.app.ModelosPerfil.Modalidad;
import com.camellofacil.backend.perfil.app.ModelosPerfil.PerfilDatos;
import com.camellofacil.backend.perfil.app.ModelosPerfil.Preferencias;
import com.camellofacil.backend.perfil.app.ModelosPerfil.TipoPersona;
import com.camellofacil.backend.perfil.app.PerfilException.Motivo;
import com.camellofacil.backend.perfil.persistence.Carrera;
import com.camellofacil.backend.perfil.persistence.CarreraRepository;
import com.camellofacil.backend.perfil.persistence.Perfil;
import com.camellofacil.backend.perfil.persistence.PerfilRepository;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PerfilService {

    private final PerfilRepository perfiles;
    private final CarreraRepository carreras;
    private final ConsentimientoService consentimientos;

    public PerfilService(PerfilRepository perfiles, CarreraRepository carreras, ConsentimientoService consentimientos) {
        this.perfiles = perfiles;
        this.carreras = carreras;
        this.consentimientos = consentimientos;
    }

    @Transactional(readOnly = true)
    public List<CarreraDatos> carreras() {
        return carreras.findByActivaTrueOrderByOrdenAsc().stream()
                .map(c -> new CarreraDatos(c.getCodigo(), c.getNombre()))
                .toList();
    }

    /**
     * Completo = tiene perfil y aceptó los términos vigentes. Si se publica una versión nueva de los términos,
     * la persona vuelve a ver el modal del primer ingreso.
     */
    @Transactional(readOnly = true)
    public boolean estaCompleto(UUID usuarioId) {
        return perfiles.existsById(usuarioId)
                && consentimientos.vigente(usuarioId, TipoConsentimiento.TERMINOS_PRIVACIDAD).isPresent();
    }

    @Transactional(readOnly = true)
    public Optional<PerfilDatos> obtener(UUID usuarioId) {
        return perfiles.findById(usuarioId).map(this::aDatos);
    }

    /** Modal del primer ingreso: datos obligatorios y los dos consentimientos (términos y análisis con IA). */
    @Transactional
    public void completar(UUID usuarioId, DatosBasicos datos) {
        if (estaCompleto(usuarioId)) {
            throw new PerfilException(Motivo.YA_COMPLETO, "El perfil ya está completo");
        }
        Carrera carrera = validar(datos);
        Instant ahora = Instant.now();
        Perfil perfil = perfiles.findById(usuarioId).orElseGet(() -> new Perfil(usuarioId, ahora));
        perfil.datosBasicos(datos.tipo().name(), carrera.getCodigo(), semestreDe(datos), ahora);
        perfiles.save(perfil);
        consentimientos.otorgar(usuarioId, TipoConsentimiento.TERMINOS_PRIVACIDAD);
        consentimientos.otorgar(usuarioId, TipoConsentimiento.ANALISIS_IA);
    }

    /** Editar perfil: datos básicos y preferencias. */
    @Transactional
    public PerfilDatos actualizar(UUID usuarioId, DatosBasicos datos, Preferencias preferencias) {
        Perfil perfil = perfiles.findById(usuarioId)
                .filter(p -> estaCompleto(usuarioId))
                .orElseThrow(() -> new PerfilException(Motivo.INCOMPLETO, "Primero completa tu perfil"));
        Carrera carrera = validar(datos);
        perfil.datosBasicos(datos.tipo().name(), carrera.getCodigo(), semestreDe(datos), Instant.now());
        perfil.preferencias(
                unir(preferencias.modalidades()),
                preferencias.disponibilidad() == null ? null : preferencias.disponibilidad().name());
        return aDatos(perfil);
    }

    private Carrera validar(DatosBasicos datos) {
        Carrera carrera = carreras.findById(datos.carrera())
                .filter(Carrera::isActiva)
                .orElseThrow(() -> new PerfilException(Motivo.DATOS_INVALIDOS, "La carrera no existe"));
        if (datos.tipo() == TipoPersona.ESTUDIANTE
                && (datos.semestre() == null || datos.semestre() < 1 || datos.semestre() > 10)) {
            throw new PerfilException(Motivo.DATOS_INVALIDOS, "El semestre debe estar entre 1 y 10");
        }
        return carrera;
    }

    /** A quien es egresado no se le guarda semestre, aunque llegue uno. */
    private static Short semestreDe(DatosBasicos datos) {
        return datos.tipo() == TipoPersona.ESTUDIANTE ? datos.semestre().shortValue() : null;
    }

    private PerfilDatos aDatos(Perfil p) {
        CarreraDatos carrera = carreras.findById(p.getCarreraCodigo())
                .map(c -> new CarreraDatos(c.getCodigo(), c.getNombre()))
                .orElse(new CarreraDatos(p.getCarreraCodigo(), p.getCarreraCodigo()));
        Preferencias preferencias = new Preferencias(
                separar(p.getModalidades()),
                p.getDisponibilidad() == null ? null : Disponibilidad.valueOf(p.getDisponibilidad()));
        return new PerfilDatos(
                TipoPersona.valueOf(p.getTipo()),
                carrera,
                p.getSemestre() == null ? null : p.getSemestre().intValue(),
                preferencias);
    }

    private static String unir(Set<Modalidad> modalidades) {
        return modalidades == null || modalidades.isEmpty()
                ? null
                : EnumSet.copyOf(modalidades).stream().map(Enum::name).collect(Collectors.joining(","));
    }

    private static Set<Modalidad> separar(String texto) {
        if (texto == null || texto.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(texto.split(","))
                .map(Modalidad::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Modalidad.class)));
    }
}
