/**
 * Rutas del API de adaptación de la hoja de vida a una vacante.
 * TODO: confirmar con el backend / reemplazar por el cliente generado desde OpenAPI.
 */
export const API_ADAPTACION = {
  /** GET estado (adaptación existente y cupo de hoy); POST crea o regenera (429 si se acabó el cupo). */
  deVacante: (vacanteId: string) => `/api/vacantes/${encodeURIComponent(vacanteId)}/adaptacion`,
  /** DELETE descarta. */
  adaptacion: (id: string) => `/api/adaptaciones/${encodeURIComponent(id)}`,
  /** POST `{ postular }` guarda la adaptación como versión ligada a la vacante. */
  aceptar: (id: string) => `/api/adaptaciones/${encodeURIComponent(id)}/aceptacion`,
  descarga: (id: string, formato: 'pdf' | 'docx') =>
    `/api/adaptaciones/${encodeURIComponent(id)}/descarga?formato=${formato}`,
} as const;
