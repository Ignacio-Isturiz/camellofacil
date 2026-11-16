import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { Sesion } from './sesion';

/**
 * Las pantallas de quien ya inició sesión mandan a /login si no hay sesión. Si no hay una en memoria,
 * pregunta al backend (p. ej. al volver de Microsoft). Es navegación, no seguridad.
 */
export const exigirSesion: CanActivateFn = async () => {
  const sesion = inject(Sesion);
  const router = inject(Router);
  if (sesion.activa() || (await sesion.restaurar())) return true;
  return router.createUrlTree(['/login']);
};
