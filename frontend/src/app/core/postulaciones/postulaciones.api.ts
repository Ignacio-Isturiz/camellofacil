/**
 * Rutas del API de postulaciones (historial de clics en «Postular») y de clics en vacantes.
 * TODO: confirmar con el backend / reemplazar por el cliente generado desde OpenAPI.
 */
export const API_POSTULACIONES = {
  /** GET ?q=&orden= → ListaPostulaciones. */
  lista: '/api/postulaciones',
  /** DELETE quita del historial. */
  postulacion: (id: string) => `/api/postulaciones/${encodeURIComponent(id)}`,
  /** POST deshace el quitado (para «Deshacer»). */
  restaurar: (id: string) => `/api/postulaciones/${encodeURIComponent(id)}/restauracion`,
  /** POST registra un clic nuevo desde el historial y devuelve la página original. */
  clicPostulacion: (id: string) => `/api/postulaciones/${encodeURIComponent(id)}/clics`,
  /** POST `{ hoja }` registra el clic en «Postular» de una vacante y devuelve la página original. */
  clicVacante: (vacanteId: string) => `/api/vacantes/${encodeURIComponent(vacanteId)}/clics`,
} as const;
