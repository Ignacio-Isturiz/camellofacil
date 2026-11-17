/** Rutas del API del módulo perfil (y del consentimiento de IA, módulo legal). Siempre de quien inició sesión. */
export const API_PERFIL = {
  /** GET estado; POST completa el primer ingreso; PUT actualiza. */
  perfil: '/api/v1/perfil',
  carreras: '/api/v1/perfil/carreras',
  /** PUT `{ activa }` otorga o revoca la autorización de análisis con IA (RF-93). */
  analisisIa: '/api/v1/legal/analisis-ia',
} as const;
