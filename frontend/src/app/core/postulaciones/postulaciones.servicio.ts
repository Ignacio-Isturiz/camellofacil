import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable } from 'rxjs';

import { API_POSTULACIONES } from './postulaciones.api';
import { ClicPostular, HojaUsada, ListaPostulaciones, OrdenPostulaciones, Postulacion } from './postulaciones.modelos';

export type EstadoLista = 'cargando' | 'listo' | 'error';

/**
 * Historial de clics en «Postular». Todo viene del API real; sin backend la pantalla muestra su error.
 * Cada llamada del backend debe verificar que el registro es del usuario autenticado.
 */
@Injectable({ providedIn: 'root' })
export class PostulacionesServicio {
  private readonly http = inject(HttpClient);

  readonly items = signal<readonly Postulacion[]>([]);
  readonly clics90 = signal<number | null>(null);
  readonly estado = signal<EstadoLista>('cargando');

  cargar(texto: string, orden: OrdenPostulaciones): void {
    this.estado.set('cargando');
    let params = new HttpParams().set('orden', orden);
    if (texto.trim()) params = params.set('q', texto.trim());
    this.http.get<ListaPostulaciones>(API_POSTULACIONES.lista, { params }).subscribe({
      next: (r) => {
        this.items.set(r.items);
        this.clics90.set(r.clicsUltimos90Dias);
        this.estado.set('listo');
      },
      error: () => this.estado.set('error'),
    });
  }

  quitar(id: string): Observable<unknown> {
    return this.http.delete(API_POSTULACIONES.postulacion(id));
  }

  restaurar(id: string): Observable<unknown> {
    return this.http.post(API_POSTULACIONES.restaurar(id), {});
  }

  clicDesdeHistorial(id: string): Observable<ClicPostular> {
    return this.http.post<ClicPostular>(API_POSTULACIONES.clicPostulacion(id), {});
  }

  clicEnVacante(vacanteId: string, hoja: HojaUsada): Observable<ClicPostular> {
    return this.http.post<ClicPostular>(API_POSTULACIONES.clicVacante(vacanteId), { hoja });
  }
}
