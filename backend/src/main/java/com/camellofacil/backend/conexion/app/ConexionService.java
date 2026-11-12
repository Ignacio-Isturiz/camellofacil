package com.camellofacil.backend.conexion.app;

import org.springframework.stereotype.Service;

/**
 * Módulo temporal para comprobar que el frontend y el backend se comunican.
 * Se elimina cuando exista el primer módulo real (auth).
 */
@Service
public class ConexionService {

    public String mensajeDeConexion() {
        return "Conexión con el backend establecida";
    }
}
