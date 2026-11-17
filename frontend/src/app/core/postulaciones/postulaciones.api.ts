/**
 * Rutas del API de postulaciones (historial de clics en «Postular») y de clics en vacantes.
 * Módulo postulaciones del backend. Siempre se aplican a quien inició sesión.
 */
export const API_POSTULACIONES = {
  /** GET ?q=&orden= → ListaPostulaciones. */
  lista: '/api/v1/postulaciones',
  /** DELETE quita del historial. */
  postulacion: (id: string) => `/api/v1/postulaciones/${encodeURIComponent(id)}`,
  /** POST deshace el quitado (para «Deshacer»). */
  restaurar: (id: string) => `/api/v1/postulaciones/${encodeURIComponent(id)}/restauracion`,
  /** POST registra un clic nuevo desde el historial y devuelve la página original. */
  clicPostulacion: (id: string) => `/api/v1/postulaciones/${encodeURIComponent(id)}/clics`,
  /** POST `{ hoja }` registra el clic en «Postular» de una vacante y devuelve la página original. */
  clicVacante: (vacanteId: string) => `/api/v1/vacantes/${encodeURIComponent(vacanteId)}/clics`,
} as const;
