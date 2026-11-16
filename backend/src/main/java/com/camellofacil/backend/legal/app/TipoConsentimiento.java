package com.camellofacil.backend.legal.app;

/** Cada finalidad del tratamiento de datos tiene su propio consentimiento (Ley 1581). */
public enum TipoConsentimiento {
    /** Términos y condiciones y política de privacidad (RF-04). */
    TERMINOS_PRIVACIDAD,
    /** Análisis de la hoja de vida con IA. Se puede revocar desde Mi perfil. */
    ANALISIS_IA
}
