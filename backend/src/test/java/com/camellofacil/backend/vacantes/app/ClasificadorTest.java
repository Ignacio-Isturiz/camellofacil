package com.camellofacil.backend.vacantes.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.camellofacil.backend.vacantes.app.FuenteVacantes.Cupo;
import com.camellofacil.backend.vacantes.app.ModelosVacante.Modalidad;
import com.camellofacil.backend.vacantes.app.ModelosVacante.TipoVacante;
import com.camellofacil.backend.vacantes.app.ModelosVacante.VacanteExterna;
import com.camellofacil.backend.vacantes.app.ModelosVacante.VacanteNueva;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ClasificadorTest {

    private static final Instant AHORA = Instant.parse("2026-11-16T12:00:00Z");

    private static VacanteExterna externa(String cargo, String empresa, String ciudad, String tipoEmpleo, String url) {
        return new VacanteExterna("id-1", cargo, empresa, ciudad, false, tipoEmpleo, "Portal de Ejemplo", url,
                "Descripción <b>ficticia</b>", List.of(), List.of(), AHORA.minus(Duration.ofDays(2)));
    }

    private static VacanteNueva clasificar(VacanteExterna e) {
        return Clasificador.clasificar("prueba", "informatica", e, AHORA).orElseThrow();
    }

    @Test
    void reconocePracticasPorElCargoOPorElTipoDeEmpleo() {
        assertThat(clasificar(externa("PRACTICANTE EN INGENIERÍA DE SISTEMAS", "Empresa", null, "", "https://e.co/1")).tipo())
                .isEqualTo(TipoVacante.PRACTICA);
        assertThat(clasificar(externa("Auxiliar de soporte", "Empresa", null, "Pasantía INTERN", "https://e.co/1")).tipo())
                .isEqualTo(TipoVacante.PRACTICA);
        assertThat(clasificar(externa("Judicante área laboral", "Empresa", null, "", "https://e.co/1")).tipo())
                .isEqualTo(TipoVacante.PRACTICA);
        assertThat(clasificar(externa("Desarrollador Senior Java", "Empresa", null, "Tiempo completo", "https://e.co/1")).tipo())
                .isEqualTo(TipoVacante.PROFESIONAL);
    }

    @Test
    void asignaLaCarreraDeLaBusquedaYLasQueAparecenEnElCargo() {
        VacanteNueva v = clasificar(externa(
                "Practicante Ing Civil, Ing Sistemas, Ing De Producción", "Empresa", "Bogotá", "", "https://e.co/1"));
        assertThat(v.carreras()).containsExactlyInAnyOrder("informatica", "industrial");
        assertThat(Clasificador.carreras("derecho", "practicante juridico")).containsExactly("derecho");
        assertThat(Clasificador.carreras("inventada", "practicante contable")).containsExactly("contaduria");
        // Afines: «Power BI» sirve a datos e informática; «desarrollo comercial» no es informática
        assertThat(Clasificador.carreras(null, "practicante power bi y dashboards")).containsExactlyInAnyOrder("datos", "informatica");
        assertThat(Clasificador.carreras(null, "practicante en desarrollo comercial")).containsExactly("administracion");
        assertThat(Clasificador.carreras(null, "aprendiz desarrollador web")).containsExactly("informatica");
        assertThat(Clasificador.carreras(null, "community manager redes sociales")).isEmpty();
    }

    @Test
    void laMismaOfertaEnOtroPortalTieneLaMismaHuella() {
        String a = Clasificador.huella("PRACTICANTE INGENIERÍA DE SISTEMAS O AFINES", "Banco de Ejemplo", null);
        String b = Clasificador.huella("Practicante ingenieria de sistemas o afines", "BANCO DE EJEMPLO", "");
        String otraCiudad = Clasificador.huella("Practicante ingenieria de sistemas o afines", "Banco de Ejemplo", "Medellín");
        assertThat(a).isEqualTo(b).hasSize(64);
        assertThat(otraCiudad).isNotEqualTo(a);
    }

    @Test
    void descartaLoInvalidoYLimpiaElTexto() {
        assertThat(Clasificador.clasificar("prueba", null, externa("Cargo", "Empresa", null, "", "javascript:alert(1)"), AHORA))
                .isEmpty();
        assertThat(Clasificador.clasificar("prueba", null, externa("  ", "Empresa", null, "", "https://e.co/1"), AHORA))
                .isEmpty();
        assertThat(Clasificador.clasificar("prueba", null, externa("Cargo", null, null, "", "https://e.co/1"), AHORA))
                .isEmpty();

        VacanteNueva v = clasificar(externa("Practicante <script>x</script> remoto", "Empresa", null, "", "https://e.co/1"));
        assertThat(v.cargo()).doesNotContain("<").contains("Practicante");
        assertThat(v.descripcion()).isEqualTo("Descripción ficticia");
        assertThat(v.modalidad()).isEqualTo(Modalidad.REMOTO);
    }

    @Test
    void unaFechaFuturaONulaSeTomaComoAhora() {
        VacanteExterna futura = new VacanteExterna("1", "Cargo", "Empresa", null, false, "", null, "https://e.co/1",
                null, null, null, AHORA.plus(Duration.ofDays(3)));
        assertThat(Clasificador.clasificar("prueba", null, futura, AHORA).orElseThrow().publicadaEn()).isEqualTo(AHORA);
    }

    @Test
    void soloSirvenLasRemotasOLasDelAreaMetropolitanaDeMedellin() {
        assertThat(Clasificador.enAreaMetropolitana("Medellín")).isTrue();
        assertThat(Clasificador.enAreaMetropolitana("Envigado, Antioquia")).isTrue();
        assertThat(Clasificador.enAreaMetropolitana("Itagüí, Antioquia, Colombia")).isTrue();
        assertThat(Clasificador.enAreaMetropolitana("Caldas, Antioquia")).isTrue();
        assertThat(Clasificador.enAreaMetropolitana("Manizales, Caldas")).isFalse();
        assertThat(Clasificador.enAreaMetropolitana("Barbosa, Santander")).isFalse();
        assertThat(Clasificador.enAreaMetropolitana("Bogotá")).isFalse();
        assertThat(Clasificador.enAreaMetropolitana(null)).isFalse();

        VacanteExterna remotaEnBogota = new VacanteExterna("1", "Practicante", "Empresa", "Bogotá", true, "", null,
                "https://e.co/1", null, null, null, null);
        assertThat(Clasificador.disponibleDesdeMedellin(clasificar(remotaEnBogota))).isTrue();
        assertThat(Clasificador.disponibleDesdeMedellin(clasificar(externa("Practicante", "Empresa", "Bogotá", "", "https://e.co/1"))))
                .isFalse();
        assertThat(Clasificador.disponibleDesdeMedellin(clasificar(externa("Practicante", "Empresa", "Sabaneta", "", "https://e.co/1"))))
                .isTrue();
    }

    @Test
    void elCupoSeRepartePorIgualEnElPeriodo() {
        Cupo mensual = new Cupo(180, Duration.ofDays(30));
        Instant inicio = AHORA;
        assertThat(mensual.permitidasHasta(inicio, inicio)).isEqualTo(1);
        assertThat(mensual.permitidasHasta(inicio, inicio.plus(Duration.ofDays(1)))).isEqualTo(6);
        assertThat(mensual.permitidasHasta(inicio, inicio.plus(Duration.ofDays(15)))).isEqualTo(90);
        assertThat(mensual.permitidasHasta(inicio, inicio.plus(Duration.ofDays(30)))).isEqualTo(180);

        Cupo diario = new Cupo(90, Duration.ofDays(1));
        assertThat(diario.permitidasHasta(inicio, inicio.plus(Duration.ofHours(12)))).isEqualTo(45);
        assertThat(diario.permitidasHasta(inicio, inicio.plus(Duration.ofHours(30)))).isEqualTo(90);
    }

    @Test
    void laCargaInicialSePermiteDeUnaVezSinPasarseDelTotal() {
        Cupo conAdelanto = new Cupo(180, Duration.ofDays(30), 10);
        Instant inicio = AHORA;
        assertThat(conAdelanto.permitidasHasta(inicio, inicio)).isEqualTo(10);
        assertThat(conAdelanto.permitidasHasta(inicio, inicio.plus(Duration.ofDays(15)))).isEqualTo(95);
        assertThat(conAdelanto.permitidasHasta(inicio, inicio.plus(Duration.ofDays(30)))).isEqualTo(180);
        assertThat(new Cupo(5, Duration.ofDays(1), 50).permitidasHasta(inicio, inicio)).isEqualTo(5);
    }
}
