package com.camellofacil.backend.perfil.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarreraRepository extends JpaRepository<Carrera, String> {

    List<Carrera> findByActivaTrueOrderByOrdenAsc();
}
