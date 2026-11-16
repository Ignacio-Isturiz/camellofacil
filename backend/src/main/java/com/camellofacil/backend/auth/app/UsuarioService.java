package com.camellofacil.backend.auth.app;

import com.camellofacil.backend.auth.persistence.Usuario;
import com.camellofacil.backend.auth.persistence.UsuarioRepository;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    public static final String PROVEEDOR_MICROSOFT = "microsoft";

    private final UsuarioRepository repositorio;

    public UsuarioService(UsuarioRepository repositorio) {
        this.repositorio = repositorio;
    }

    /** Crea la cuenta la primera vez y, en los ingresos siguientes, actualiza nombre, correo y último acceso. */
    @Transactional
    public UsuarioActual registrarAcceso(String proveedor, String idExterno, String correo, String nombre) {
        Instant ahora = Instant.now();
        Usuario usuario = repositorio.findByProveedorAndIdExterno(proveedor, idExterno)
                .map(existente -> {
                    existente.registrarAcceso(correo, nombre, ahora);
                    return existente;
                })
                .orElseGet(() -> repositorio.save(new Usuario(proveedor, idExterno, correo, nombre, ahora)));
        return aActual(usuario);
    }

    @Transactional(readOnly = true)
    public Optional<UsuarioActual> buscar(String proveedor, String idExterno) {
        return repositorio.findByProveedorAndIdExterno(proveedor, idExterno).map(UsuarioService::aActual);
    }

    private static UsuarioActual aActual(Usuario u) {
        return new UsuarioActual(u.getId(), u.getCorreo(), u.getNombre());
    }
}
