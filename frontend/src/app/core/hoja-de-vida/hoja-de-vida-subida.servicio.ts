import { HttpClient, HttpErrorResponse, HttpEventType } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Subscription, filter, switchMap, take, timer } from 'rxjs';

import { API_HOJA_DE_VIDA } from './hoja-de-vida.api';
import { AvanceAnalisis, ErrorSubida } from './hoja-de-vida.modelos';

export type EstadoSubida = 'reposo' | 'subiendo' | 'analizando' | 'listo' | 'error';

/** Peso máximo del archivo (el servidor vuelve a validarlo). */
export const PESO_MAXIMO_BYTES = 5 * 1024 * 1024;
const EXTENSIONES = ['.pdf', '.doc', '.docx'];
const INTERVALO_ANALISIS_MS = 2500;
/** Se deja de preguntar después de este tiempo y se muestra un error del servidor. */
const LIMITE_ANALISIS_MS = 3 * 60 * 1000;

/**
 * Subida del archivo y seguimiento de su análisis. Todo contra el API real:
 * POST multipart y luego consultas periódicas al avance (no hay respuestas simuladas).
 * La autorización de IA (RF-93) viaja junto con el archivo.
 */
@Injectable({ providedIn: 'root' })
export class HojaDeVidaSubidaServicio {
  private readonly http = inject(HttpClient);
  private peticion: Subscription | null = null;

  readonly estado = signal<EstadoSubida>('reposo');
  readonly progreso = signal(0);
  readonly error = signal<ErrorSubida | null>(null);
  readonly nombreArchivo = signal<string | null>(null);

  /** Valida en el cliente y, si pasa, sube el archivo. Devuelve false si lo rechazó sin enviarlo. */
  subir(archivo: File, autorizado: boolean): boolean {
    if (!autorizado) return false;
    this.nombreArchivo.set(archivo.name);

    const nombre = archivo.name.toLowerCase();
    if (!EXTENSIONES.some((e) => nombre.endsWith(e))) return this.fallar('formato');
    if (archivo.size > PESO_MAXIMO_BYTES) return this.fallar('peso');
    if (archivo.size === 0) return this.fallar('ilegible');

    this.cancelar();
    this.error.set(null);
    this.progreso.set(0);
    this.estado.set('subiendo');

    const cuerpo = new FormData();
    cuerpo.append('archivo', archivo, archivo.name);
    cuerpo.append('autorizacionIa', 'true');

    this.peticion = this.http
      .post(API_HOJA_DE_VIDA.archivo, cuerpo, { observe: 'events', reportProgress: true })
      .subscribe({
        next: (evento) => {
          if (evento.type === HttpEventType.UploadProgress && evento.total) {
            this.progreso.set(Math.round((evento.loaded / evento.total) * 100));
          } else if (evento.type === HttpEventType.Response) {
            this.seguirAnalisis();
          }
        },
        error: (e: HttpErrorResponse) => this.fallar(this.clasificar(e)),
      });
    return true;
  }

  /** Detiene la subida o el seguimiento en curso (al salir de la pantalla o al cancelar). */
  cancelar(): void {
    this.peticion?.unsubscribe();
    this.peticion = null;
  }

  reiniciar(): void {
    this.cancelar();
    this.estado.set('reposo');
    this.progreso.set(0);
    this.error.set(null);
    this.nombreArchivo.set(null);
  }

  private seguirAnalisis(): void {
    this.estado.set('analizando');
    const inicio = Date.now();
    this.peticion = timer(0, INTERVALO_ANALISIS_MS)
      .pipe(
        switchMap(() => this.http.get<AvanceAnalisis>(API_HOJA_DE_VIDA.analisis)),
        filter((a) => {
          if (a.estado === 'analizando' && Date.now() - inicio > LIMITE_ANALISIS_MS) throw new Error('tiempo');
          return a.estado !== 'analizando';
        }),
        take(1),
      )
      .subscribe({
        next: (a) => (a.estado === 'listo' ? this.estado.set('listo') : void this.fallar('ilegible')),
        error: (e: unknown) => this.fallar(e instanceof HttpErrorResponse ? this.clasificar(e) : 'servidor'),
      });
  }

  private clasificar(e: HttpErrorResponse): ErrorSubida {
    switch (e.status) {
      case 0:
        return 'conexion';
      case 413:
        return 'peso';
      case 415:
        return 'formato';
      case 422:
        return 'ilegible';
      default:
        return 'servidor';
    }
  }

  private fallar(tipo: ErrorSubida): false {
    this.cancelar();
    this.error.set(tipo);
    this.estado.set('error');
    return false;
  }
}
