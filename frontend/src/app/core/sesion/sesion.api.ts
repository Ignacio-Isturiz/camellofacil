/** Rutas de autenticación. El ingreso es una navegación de página completa a Microsoft (no una llamada HTTP). */
export const API_SESION = {
  /** GET «quién soy»: un 401 aquí solo significa «no hay sesión», no que haya vencido. */
  yo: '/api/v1/auth/me',
  salir: '/api/v1/auth/logout',
  ingresoMicrosoft: '/oauth2/authorization/microsoft',
} as const;
