import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { tap } from 'rxjs';

export type CausaError = 'conexion' | 'acceso' | 'no-disponible' | 'servidor' | 'otro';

/** Traduce el estado HTTP de una respuesta fallida a la causa que se le explica a la persona. */
export function causaDeEstado(estado: number): CausaError {
  if (estado === 0) return 'conexion';
  if (estado === 401 || estado === 403) return 'acceso';
  if (estado === 404 || estado === 405 || estado === 501) return 'no-disponible';
  if (estado >= 500) return 'servidor';
  return 'otro';
}

/**
 * Guarda la causa de la última llamada fallida al API. Los avisos de error la usan para no decir
 * «revisa tu conexión» cuando el problema es otro (servidor reiniciando, endpoint que aún no existe, sin permiso).
 */
@Injectable({ providedIn: 'root' })
export class UltimoError {
  readonly causa = signal<CausaError>('otro');
}

export const ultimoErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const registro = inject(UltimoError);
  return next(req).pipe(
    tap({
      error: (e: unknown) => {
        if (req.url.startsWith('/api') && e instanceof HttpErrorResponse) registro.causa.set(causaDeEstado(e.status));
      },
    }),
  );
};
