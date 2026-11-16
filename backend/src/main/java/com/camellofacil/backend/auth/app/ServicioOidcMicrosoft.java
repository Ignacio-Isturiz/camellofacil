package com.camellofacil.backend.auth.app;

import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/**
 * Se ejecuta cuando Microsoft ya validó la identidad. Aquí se decide si la persona puede entrar:
 * debe ser del inquilino de UNAULA y tener correo del dominio permitido. Luego se crea o actualiza su cuenta.
 */
@Service
public class ServicioOidcMicrosoft extends OidcUserService {

    public static final String ERROR_DOMINIO = "dominio_no_permitido";
    public static final String ERROR_CUENTA = "cuenta_invalida";

    private final UsuarioService usuarios;
    private final String tenantId;
    private final String dominioPermitido;

    public ServicioOidcMicrosoft(
            UsuarioService usuarios,
            @Value("${camellofacil.microsoft.tenant-id}") String tenantId,
            @Value("${camellofacil.microsoft.dominio-permitido}") String dominioPermitido) {
        this.usuarios = usuarios;
        this.tenantId = tenantId;
        this.dominioPermitido = dominioPermitido.toLowerCase(Locale.ROOT);
    }

    @Override
    public OidcUser loadUser(OidcUserRequest solicitud) throws OAuth2AuthenticationException {
        OidcUser usuario = super.loadUser(solicitud);

        // Defensa en profundidad: Azure ya limita el registro a un solo inquilino, pero se vuelve a comprobar.
        if (!tenantId.equalsIgnoreCase(usuario.getClaimAsString("tid"))) {
            throw rechazo(ERROR_DOMINIO);
        }
        String idExterno = idExternoDe(usuario);
        String correo = correoDe(usuario);
        if (idExterno == null || correo == null) {
            throw rechazo(ERROR_CUENTA);
        }
        if (!correoPermitido(correo)) {
            throw rechazo(ERROR_DOMINIO);
        }
        String nombre = usuario.getFullName() != null ? usuario.getFullName() : correo.substring(0, correo.indexOf('@'));
        usuarios.registrarAcceso(UsuarioService.PROVEEDOR_MICROSOFT, idExterno, correo, nombre);
        return usuario;
    }

    boolean correoPermitido(String correo) {
        return correo.toLowerCase(Locale.ROOT).endsWith(dominioPermitido);
    }

    /** «oid» es el id estable de la persona en el inquilino; «sub» solo es estable por aplicación. */
    public static String idExternoDe(OidcUser usuario) {
        String oid = usuario.getClaimAsString("oid");
        return oid != null && !oid.isBlank() ? oid : usuario.getSubject();
    }

    /** El claim «email» es opcional en Entra; «preferred_username» (el UPN) siempre viene en tokens v2. */
    static String correoDe(OidcUser usuario) {
        String correo = usuario.getEmail() != null ? usuario.getEmail() : usuario.getPreferredUsername();
        return correo == null || !correo.contains("@") ? null : correo.trim().toLowerCase(Locale.ROOT);
    }

    private static OAuth2AuthenticationException rechazo(String codigo) {
        return new OAuth2AuthenticationException(new OAuth2Error(codigo));
    }
}
