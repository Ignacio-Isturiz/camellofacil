package com.camellofacil.backend.auth.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByProveedorAndIdExterno(String proveedor, String idExterno);
}
