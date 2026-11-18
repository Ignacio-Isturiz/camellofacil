/**
 * Rutas del API de «Hoja de vida». Todas en un solo lugar.
 *
 * Las de subida, análisis, datos y confirmación ya existen en el backend (módulo cv); las demás llegan con
 * las siguientes etapas. TODO: reemplazar por el cliente generado desde OpenAPI (ng-openapi-gen).
 *
 * Las rutas son relativas: en desarrollo pasan por el proxy de /api (proxy.conf.json) y en producción
 * por nginx. Si el API vive en otro dominio (api.camellofacil.com), anteponer la base aquí.
 */
export const API_HOJA_DE_VIDA = {
  /** Estado de la hoja base del usuario autenticado (siempre 200; el campo `estado` dice en qué punto está). */
  resumen: '/api/v1/hoja-de-vida',
  /** Puntaje ATS recalculado para otra opción de evaluación. */
  puntajeAts: (opcionId: string) => `/api/v1/hoja-de-vida/puntajes/ats/${encodeURIComponent(opcionId)}`,
  /** Subida (multipart: `archivo`). Exige la autorización de IA vigente (Mi perfil). Responde 202 y el análisis sigue en segundo plano. */
  archivo: '/api/v1/hoja-de-vida/archivo',
  /** Avance del análisis del archivo recién subido. */
  analisis: '/api/v1/hoja-de-vida/analisis',
  /** Datos extraídos del archivo (GET) y su corrección (PUT). */
  datos: '/api/v1/hoja-de-vida/datos',
  /** Confirma los datos revisados y dispara el análisis Harvard / ATS (POST). */
  confirmarDatos: '/api/v1/hoja-de-vida/datos/confirmacion',
  /** POST: pide las recomendaciones de la IA (tardan unos segundos) y devuelve el resumen con ellas. */
  recomendacionesIa: '/api/v1/hoja-de-vida/recomendaciones-ia',
  plantillas: '/api/v1/plantillas',
  /** Detalle de una plantilla, con la guía de qué va en cada sección. */
  plantilla: (id: string) => `/api/v1/plantillas/${encodeURIComponent(id)}`,
  /** La plantilla renderizada con los datos del usuario (documento estructurado). */
  vistaPreviaPlantilla: (id: string, idioma: string) =>
    `/api/v1/plantillas/${encodeURIComponent(id)}/vista-previa?idioma=${encodeURIComponent(idioma)}`,
  versiones: '/api/v1/hoja-de-vida/versiones',
  /** Versión concreta: PATCH renombra, DELETE elimina. */
  version: (id: string) => `/api/v1/hoja-de-vida/versiones/${encodeURIComponent(id)}`,
  documentoVersion: (id: string) => `/api/v1/hoja-de-vida/versiones/${encodeURIComponent(id)}/documento`,
  duplicarVersion: (id: string) => `/api/v1/hoja-de-vida/versiones/${encodeURIComponent(id)}/duplicado`,
  usarVersionComoBase: (id: string) => `/api/v1/hoja-de-vida/versiones/${encodeURIComponent(id)}/base`,
  /** Reorganización: POST propone; PUT cambia un cambio; POST aceptación guarda la versión; DELETE descarta. */
  reorganizaciones: '/api/v1/hoja-de-vida/reorganizaciones',
  reorganizacion: (id: string) => `/api/v1/hoja-de-vida/reorganizaciones/${encodeURIComponent(id)}`,
  cambioReorganizacion: (id: string, cambioId: string) =>
    `/api/v1/hoja-de-vida/reorganizaciones/${encodeURIComponent(id)}/cambios/${encodeURIComponent(cambioId)}`,
  aceptarReorganizacion: (id: string) => `/api/v1/hoja-de-vida/reorganizaciones/${encodeURIComponent(id)}/aceptacion`,
  /** Editor: POST crea el borrador (desde plantilla o versión); PUT guarda; POST version lo guarda como versión. */
  borradores: '/api/v1/hoja-de-vida/borradores',
  borrador: (id: string) => `/api/v1/hoja-de-vida/borradores/${encodeURIComponent(id)}`,
  guardarBorradorComoVersion: (id: string) => `/api/v1/hoja-de-vida/borradores/${encodeURIComponent(id)}/version`,
  descargarVersion: (versionId: string, formato: 'pdf' | 'docx') =>
    `/api/v1/hoja-de-vida/versiones/${encodeURIComponent(versionId)}/descarga?formato=${formato}`,
} as const;
