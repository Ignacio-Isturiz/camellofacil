import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { Sesion } from './sesion';

/** Las pantallas de quien ya inició sesión mandan a /login si no hay sesión. Es navegación, no seguridad. */
export const exigirSesion: CanActivateFn = () =>
  inject(Sesion).activa() ? true : inject(Router).createUrlTree(['/login']);
