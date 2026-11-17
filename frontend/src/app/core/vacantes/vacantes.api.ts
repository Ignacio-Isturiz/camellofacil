/**
 * Rutas del API de vacantes (módulo vacantes del backend). Las marcas siempre se aplican a quien inició
 * sesión. TODO: reemplazar por el cliente generado desde OpenAPI.
 */
export const API_VACANTES = {
  /** GET → vacantes abiertas e ids de las guardadas. */
  lista: '/api/v1/vacantes',
  /** GET → detalle con descripción, responsabilidades y requisitos. */
  vacante: (id: string) => `/api/v1/vacantes/${encodeURIComponent(id)}`,
  /** PUT guarda, DELETE quita de guardadas. */
  guardada: (id: string) => `/api/v1/vacantes/${encodeURIComponent(id)}/guardada`,
} as const;
