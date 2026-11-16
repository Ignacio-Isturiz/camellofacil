import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { RUTA_YO, Sesion } from './sesion';

/**
 * Si el backend responde 401 con una sesión que el frontend creía activa, la sesión venció (8 horas sin
 * actividad) o se cerró: se olvida y se lleva a /login para volver a entrar con Microsoft.
 */
export const sesionVencidaInterceptor: HttpInterceptorFn = (req, next) => {
  const sesion = inject(Sesion);
  const router = inject(Router);
  return next(req).pipe(
    catchError((error: unknown) => {
      const vencida = error instanceof HttpErrorResponse && error.status === 401 && !req.url.endsWith(RUTA_YO);
      if (vencida && sesion.activa()) {
        sesion.olvidar();
        void router.navigate(['/login'], { queryParams: { error: 'expirada' } });
      }
      return throwError(() => error);
    }),
  );
};
