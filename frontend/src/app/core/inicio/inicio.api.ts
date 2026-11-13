/**
 * Rutas del API de la pantalla de Inicio.
 * TODO: confirmar con el backend / reemplazar por el cliente generado desde OpenAPI.
 */
export const API_INICIO = {
  /** GET → ResumenInicio (vacantes nuevas, recomendadas de hoy y la búsqueda guardada). */
  resumen: '/api/inicio',
  /** POST `{ sirvio }` responde «¿Te sirvió?» sobre una recomendación. */
  utilidad: (vacanteId: string) => `/api/vacantes/${encodeURIComponent(vacanteId)}/utilidad`,
  /** PUT `{ texto, etiquetas }` guarda «¿Qué estás buscando?». */
  busqueda: '/api/perfil/busqueda',
  /** GET → CupoAdaptaciones (usadas y límite de hoy). */
  cupoAdaptaciones: '/api/adaptaciones/cupo',
} as const;
