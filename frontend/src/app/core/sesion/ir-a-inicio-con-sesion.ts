import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { Sesion } from './sesion';

/** La landing y /login son para quien no ha entrado: con sesión activa, se va directo a Inicio. Es navegación, no seguridad. */
export const irAInicioConSesion: CanActivateFn = async () => {
  const sesion = inject(Sesion);
  const router = inject(Router);
  return sesion.activa() || (await sesion.restaurar()) ? router.createUrlTree(['/inicio']) : true;
};
