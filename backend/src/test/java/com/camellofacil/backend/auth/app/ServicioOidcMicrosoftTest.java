package com.camellofacil.backend.auth.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

class ServicioOidcMicrosoftTest {

    private final ServicioOidcMicrosoft servicio =
            new ServicioOidcMicrosoft(mock(UsuarioService.class), "tenant", "@unaula.edu.co");

    @Test
    void aceptaSoloElDominioInstitucional() {
        assertThat(servicio.correoPermitido("ana.perez@unaula.edu.co")).isTrue();
        assertThat(servicio.correoPermitido("ANA@UNAULA.EDU.CO")).isTrue();
        assertThat(servicio.correoPermitido("ana@gmail.com")).isFalse();
        // Un dominio que solo termina parecido no debe pasar
        assertThat(servicio.correoPermitido("ana@falso-unaula.edu.co")).isFalse();
        assertThat(servicio.correoPermitido("ana@unaula.edu.co.evil.com")).isFalse();
    }

    @Test
    void usaElUpnSiNoViene_elClaimEmail() {
        OidcUser usuario = mock(OidcUser.class);
        when(usuario.getEmail()).thenReturn(null);
        when(usuario.getPreferredUsername()).thenReturn("Ana@Unaula.edu.co");

        assertThat(ServicioOidcMicrosoft.correoDe(usuario)).isEqualTo("ana@unaula.edu.co");
    }

    @Test
    void sinCorreoValidoNoHayCuenta() {
        OidcUser usuario = mock(OidcUser.class);
        when(usuario.getEmail()).thenReturn(null);
        when(usuario.getPreferredUsername()).thenReturn("sin-arroba");

        assertThat(ServicioOidcMicrosoft.correoDe(usuario)).isNull();
    }
}
