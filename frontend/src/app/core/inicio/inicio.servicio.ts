import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable } from 'rxjs';

import { ListaPostulaciones, Postulacion } from '../postulaciones/postulaciones.modelos';
import { API_POSTULACIONES } from '../postulaciones/postulaciones.api';
import { API_INICIO } from './inicio.api';
import { Busqueda, CupoAdaptaciones, EstadoBloque, ResumenInicio } from './inicio.modelos';

/**
 * Datos de Inicio. Cada bloque (resumen, cupo, últimas postulaciones) se pide por separado y falla por separado:
 * si uno no responde, los demás se ven. Sin backend, cada bloque muestra su error.
 */
@Injectable({ providedIn: 'root' })
export class InicioServicio {
  private readonly http = inject(HttpClient);

  readonly resumen = signal<ResumenInicio | null>(null);
  readonly estadoResumen = signal<EstadoBloque>('cargando');

  readonly cupo = signal<CupoAdaptaciones | null>(null);
  readonly estadoCupo = signal<EstadoBloque>('cargando');

  readonly ultimas = signal<readonly Postulacion[]>([]);
  readonly estadoUltimas = signal<EstadoBloque>('cargando');

  cargarTodo(): void {
    this.cargarResumen();
    this.cargarCupo();
    this.cargarUltimas();
  }

  cargarResumen(): void {
    this.estadoResumen.set('cargando');
    this.http.get<ResumenInicio>(API_INICIO.resumen).subscribe({
      next: (r) => {
        this.resumen.set(r);
        this.estadoResumen.set('listo');
      },
      error: () => this.estadoResumen.set('error'),
    });
  }

  cargarCupo(): void {
    this.estadoCupo.set('cargando');
    this.http.get<CupoAdaptaciones>(API_INICIO.cupoAdaptaciones).subscribe({
      next: (c) => {
        this.cupo.set(c);
        this.estadoCupo.set('listo');
      },
      error: () => this.estadoCupo.set('error'),
    });
  }

  cargarUltimas(): void {
    this.estadoUltimas.set('cargando');
    const params = new HttpParams().set('orden', 'recientes');
    this.http.get<ListaPostulaciones>(API_POSTULACIONES.lista, { params }).subscribe({
      next: (r) => {
        this.ultimas.set(r.items.slice(0, 2));
        this.estadoUltimas.set('listo');
      },
      error: () => this.estadoUltimas.set('error'),
    });
  }

  enviarUtilidad(vacanteId: string, sirvio: boolean): Observable<unknown> {
    return this.http.post(API_INICIO.utilidad(vacanteId), { sirvio });
  }

  guardarBusqueda(busqueda: Busqueda): Observable<Busqueda> {
    return this.http.put<Busqueda>(API_INICIO.busqueda, busqueda);
  }

  /** Tras guardar, la búsqueda nueva se refleja sin volver a pedir todo el resumen. */
  aplicarBusqueda(busqueda: Busqueda): void {
    this.resumen.update((r) => (r ? { ...r, busqueda } : r));
  }
}
