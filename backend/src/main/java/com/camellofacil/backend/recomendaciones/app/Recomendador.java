package com.camellofacil.backend.recomendaciones.app;

import com.camellofacil.backend.perfil.app.ModelosPerfil.PerfilDatos;
import com.camellofacil.backend.perfil.app.ModelosPerfil.Preferencias;
import com.camellofacil.backend.perfil.app.ModelosPerfil.TipoPersona;
import com.camellofacil.backend.vacantes.app.ModelosVacante.TipoVacante;
import com.camellofacil.backend.vacantes.app.ModelosVacante.VacanteResumen;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Elige las recomendadas de una persona a partir de su perfilación. Solo cuentan reglas que se pueden
 * explicar: la carrera manda (sin carrera no hay recomendación), luego el tipo que le sirve (práctica si
 * estudia, empleo si es egresada), la modalidad que prefiere y lo reciente. Las razones de cada una
 * deciden el orden; no se calcula un porcentaje de coincidencia porque todavía no se compara con la hoja de vida.
 */
final class Recomendador {

    /** Una vacante «nueva» se publicó hace menos de esto. */
    static final Duration NUEVA = Duration.ofDays(2);
    /** Ventana de «Tienes N vacantes nuevas de tu carrera». */
    static final Duration SEMANA = Duration.ofDays(7);

    private Recomendador() {}

    enum Razon { CARRERA, TIPO, MODALIDAD }

    record Recomendacion(VacanteResumen vacante, boolean nueva, List<Razon> razones) {}

    record Resultado(List<Recomendacion> recomendadas, int nuevasDeLaSemana) {}

    static Resultado recomendar(
            PerfilDatos perfil, List<VacanteResumen> abiertas, Set<UUID> excluir, Instant ahora, int limite) {
        if (perfil == null || perfil.carrera() == null) return new Resultado(List.of(), 0);
        String carrera = perfil.carrera().codigo();
        TipoVacante tipoPreferido = perfil.tipo() == TipoPersona.EGRESADO ? TipoVacante.PROFESIONAL : TipoVacante.PRACTICA;
        Set<String> modalidades = modalidadesPreferidas(perfil.preferencias());

        List<Recomendacion> candidatas = abiertas.stream()
                .filter(v -> v.carreras().contains(carrera))
                .filter(v -> !excluir.contains(v.id()))
                .map(v -> {
                    List<Razon> razones = new ArrayList<>(List.of(Razon.CARRERA));
                    if (v.tipo() == tipoPreferido) razones.add(Razon.TIPO);
                    if (modalidades.contains(v.modalidad().name())) razones.add(Razon.MODALIDAD);
                    boolean nueva = v.publicadaEn().isAfter(ahora.minus(NUEVA));
                    return new Recomendacion(v, nueva, List.copyOf(razones));
                })
                .toList();

        int nuevasDeLaSemana = (int) candidatas.stream()
                .filter(r -> r.vacante().publicadaEn().isAfter(ahora.minus(SEMANA)))
                .count();

        Comparator<Recomendacion> orden = Comparator
                .comparing((Recomendacion r) -> r.razones().contains(Razon.TIPO))
                .thenComparing(r -> r.razones().contains(Razon.MODALIDAD))
                .thenComparing(r -> r.vacante().publicadaEn())
                .reversed();
        List<Recomendacion> elegidas = candidatas.stream().sorted(orden).limit(limite).toList();
        return new Resultado(elegidas, nuevasDeLaSemana);
    }

    private static Set<String> modalidadesPreferidas(Preferencias preferencias) {
        if (preferencias == null || preferencias.modalidades() == null) return Set.of();
        return preferencias.modalidades().stream().map(Enum::name).collect(java.util.stream.Collectors.toSet());
    }
}
