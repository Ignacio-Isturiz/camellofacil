package com.camellofacil.backend.perfil.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PerfilRepository extends JpaRepository<Perfil, UUID> {}
