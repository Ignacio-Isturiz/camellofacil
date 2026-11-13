import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Envía la cookie de sesión en cada llamada al API (hace falta si el API vive en otro dominio, p. ej.
 * api.camellofacil.com). La cabecera X-XSRF-TOKEN la agrega HttpClient por su cuenta (CSRF).
 */
export const credencialesInterceptor: HttpInterceptorFn = (req, next) =>
  next(req.url.startsWith('/api') || req.url.includes('camellofacil.com') ? req.clone({ withCredentials: true }) : req);
