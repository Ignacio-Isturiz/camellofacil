/**
 * Rutas del API de «Hoja de vida». Todas en un solo lugar.
 *
 * TODO: confirmar con el backend / reemplazar por el cliente generado desde OpenAPI (ng-openapi-gen).
 *
 * Las rutas son relativas: en desarrollo pasan por el proxy de /api (proxy.conf.json) y en producción
 * por nginx. Si el API vive en otro dominio (api.camellofacil.com), anteponer la base aquí.
 */
export const API_HOJA_DE_VIDA = {
  /** Estado de la hoja base del usuario autenticado (siempre 200; el campo `estado` dice en qué punto está). */
  resumen: '/api/hoja-de-vida',
  /** Puntaje ATS recalculado para otra opción de evaluación. */
  puntajeAts: (opcionId: string) => `/api/hoja-de-vida/puntajes/ats/${encodeURIComponent(opcionId)}`,
  /** Subida (multipart: `archivo` + `autorizacionIa=true`). Responde 202 y el análisis sigue en segundo plano. */
  archivo: '/api/hoja-de-vida/archivo',
  /** Avance del análisis del archivo recién subido. */
  analisis: '/api/hoja-de-vida/analisis',
  /** Datos extraídos del archivo (GET) y su corrección (PUT). */
  datos: '/api/hoja-de-vida/datos',
  /** Confirma los datos revisados y dispara el análisis Harvard / ATS (POST). */
  confirmarDatos: '/api/hoja-de-vida/datos/confirmacion',
  plantillas: '/api/plantillas',
  /** Detalle de una plantilla, con la guía de qué va en cada sección. */
  plantilla: (id: string) => `/api/plantillas/${encodeURIComponent(id)}`,
  /** La plantilla renderizada con los datos del usuario (documento estructurado). */
  vistaPreviaPlantilla: (id: string, idioma: string) =>
    `/api/plantillas/${encodeURIComponent(id)}/vista-previa?idioma=${encodeURIComponent(idioma)}`,
  versiones: '/api/hoja-de-vida/versiones',
  /** Versión concreta: PATCH renombra, DELETE elimina. */
  version: (id: string) => `/api/hoja-de-vida/versiones/${encodeURIComponent(id)}`,
  documentoVersion: (id: string) => `/api/hoja-de-vida/versiones/${encodeURIComponent(id)}/documento`,
  duplicarVersion: (id: string) => `/api/hoja-de-vida/versiones/${encodeURIComponent(id)}/duplicado`,
  usarVersionComoBase: (id: string) => `/api/hoja-de-vida/versiones/${encodeURIComponent(id)}/base`,
  /** Reorganización: POST propone; PUT cambia un cambio; POST aceptación guarda la versión; DELETE descarta. */
  reorganizaciones: '/api/hoja-de-vida/reorganizaciones',
  reorganizacion: (id: string) => `/api/hoja-de-vida/reorganizaciones/${encodeURIComponent(id)}`,
  cambioReorganizacion: (id: string, cambioId: string) =>
    `/api/hoja-de-vida/reorganizaciones/${encodeURIComponent(id)}/cambios/${encodeURIComponent(cambioId)}`,
  aceptarReorganizacion: (id: string) => `/api/hoja-de-vida/reorganizaciones/${encodeURIComponent(id)}/aceptacion`,
  /** Editor: POST crea el borrador (desde plantilla o versión); PUT guarda; POST version lo guarda como versión. */
  borradores: '/api/hoja-de-vida/borradores',
  borrador: (id: string) => `/api/hoja-de-vida/borradores/${encodeURIComponent(id)}`,
  guardarBorradorComoVersion: (id: string) => `/api/hoja-de-vida/borradores/${encodeURIComponent(id)}/version`,
  descargarVersion: (versionId: string, formato: 'pdf' | 'docx') =>
    `/api/hoja-de-vida/versiones/${encodeURIComponent(versionId)}/descarga?formato=${formato}`,
} as const;
