import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { Sesion } from './sesion';

/** La landing es para quien no ha entrado: con sesión activa, se va directo a Inicio. Es navegación, no seguridad. */
export const irAInicioConSesion: CanActivateFn = () =>
  inject(Sesion).activa() ? inject(Router).createUrlTree(['/inicio']) : true;
