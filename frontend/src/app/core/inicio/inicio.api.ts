/**
 * Rutas del API de la pantalla de Inicio.
 * Resumen: módulo recomendaciones del backend. Búsqueda y cupo de adaptaciones: aún no existen en el backend
 * (se declaran ya bajo /api/v1).
 */
export const API_INICIO = {
  /** GET ?pagina= → ResumenInicioApi (vacantes nuevas de su carrera, 4 recomendadas por página y la búsqueda guardada). */
  resumen: '/api/v1/inicio',
  /** PUT `{ texto, etiquetas }` guarda «¿Qué estás buscando?». */
  busqueda: '/api/v1/perfil/busqueda',
  /** GET → CupoAdaptaciones (usadas y límite de hoy). */
  cupoAdaptaciones: '/api/v1/adaptaciones/cupo',
} as const;
