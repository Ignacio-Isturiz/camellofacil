package com.camellofacil.backend.auth.app;

import java.util.UUID;

/** Datos de la persona con sesión iniciada, listos para la API. */
public record UsuarioActual(UUID id, String correo, String nombre) {}
