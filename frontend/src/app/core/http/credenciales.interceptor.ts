import { HttpInterceptorFn } from '@angular/common/http';

/** Dominio propio: el API puede vivir en un subdominio (p. ej. api.camellofacil.com). */
const DOMINIO = 'camellofacil.com';

/**
 * ¿La petición va a nuestro API? Las rutas relativas (`/api/...`) siempre; una URL absoluta solo si su
 * host es exactamente el dominio o un subdominio suyo. Se compara el host ya interpretado, no el texto:
 * así `https://camellofacil.com.otro.io` o `https://otro.io/?camellofacil.com` no reciben la cookie.
 */
export function esNuestroApi(url: string): boolean {
  if (url.startsWith('/api')) return true;
  let host: string;
  try {
    const u = new URL(url);
    if (u.protocol !== 'https:') return false;
    host = u.hostname;
  } catch {
    return false;
  }
  return host === DOMINIO || host.endsWith(`.${DOMINIO}`);
}

/**
 * Envía la cookie de sesión en cada llamada al API (hace falta si el API vive en otro dominio).
 * La cabecera X-XSRF-TOKEN la agrega HttpClient por su cuenta (CSRF).
 */
export const credencialesInterceptor: HttpInterceptorFn = (req, next) =>
  next(esNuestroApi(req.url) ? req.clone({ withCredentials: true }) : req);
