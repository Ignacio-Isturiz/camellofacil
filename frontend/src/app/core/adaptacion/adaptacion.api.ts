/**
 * Rutas del API de adaptación de la hoja de vida a una vacante.
 * Aún no existen en el backend: se declaran ya bajo /api/v1, la versión que exige la arquitectura.
 */
export const API_ADAPTACION = {
  /** GET estado (adaptación existente y cupo de hoy); POST crea o regenera (429 si se acabó el cupo). */
  deVacante: (vacanteId: string) => `/api/v1/vacantes/${encodeURIComponent(vacanteId)}/adaptacion`,
  /** DELETE descarta. */
  adaptacion: (id: string) => `/api/v1/adaptaciones/${encodeURIComponent(id)}`,
  /** POST `{ postular }` guarda la adaptación como versión ligada a la vacante. */
  aceptar: (id: string) => `/api/v1/adaptaciones/${encodeURIComponent(id)}/aceptacion`,
  descarga: (id: string, formato: 'pdf' | 'docx') =>
    `/api/v1/adaptaciones/${encodeURIComponent(id)}/descarga?formato=${formato}`,
} as const;
