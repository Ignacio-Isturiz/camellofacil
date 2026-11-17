import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, Subscription } from 'rxjs';

import { desdeApi } from '../vacantes/vacantes.modelos';
import { ListaPostulaciones, Postulacion } from '../postulaciones/postulaciones.modelos';
import { API_POSTULACIONES } from '../postulaciones/postulaciones.api';
import { API_INICIO } from './inicio.api';
import { Busqueda, CupoAdaptaciones, ResumenInicio, ResumenInicioApi } from './inicio.modelos';
import { EstadoCarga } from '../http/estado-carga';

/**
 * Datos de Inicio. Cada bloque (resumen, cupo, últimas postulaciones) se pide por separado y falla por separado:
 * si uno no responde, los demás se ven. Sin backend, cada bloque muestra su error.
 */
@Injectable({ providedIn: 'root' })
export class InicioServicio {
  private readonly http = inject(HttpClient);

  readonly #resumen = signal<ResumenInicio | null>(null);
  readonly resumen = this.#resumen.asReadonly();
  readonly #estadoResumen = signal<EstadoCarga>('cargando');
  readonly estadoResumen = this.#estadoResumen.asReadonly();
  /** Se está trayendo otra página de recomendadas: las tarjetas actuales siguen a la vista. */
  readonly #cambiandoPagina = signal(false);
  readonly cambiandoPagina = this.#cambiandoPagina.asReadonly();

  readonly #cupo = signal<CupoAdaptaciones | null>(null);
  readonly cupo = this.#cupo.asReadonly();
  readonly #estadoCupo = signal<EstadoCarga>('cargando');
  readonly estadoCupo = this.#estadoCupo.asReadonly();

  readonly #ultimas = signal<readonly Postulacion[]>([]);
  readonly ultimas = this.#ultimas.asReadonly();
  readonly #estadoUltimas = signal<EstadoCarga>('cargando');
  readonly estadoUltimas = this.#estadoUltimas.asReadonly();

  /** Página de recomendadas en vuelo: si se pide otra, esta se cancela (no llegan respuestas desordenadas). */
  private paginaEnCurso: Subscription | null = null;

  cargarTodo(): void {
    this.cargarResumen();
    this.cargarCupo();
    this.cargarUltimas();
  }

  /** Recomendadas de a 4: la primera carga muestra el esqueleto; al cambiar de página se conserva lo que hay. */
  cargarResumen(pagina = 0): void {
    if (this.resumen() && this.estadoResumen() === 'listo') this.#cambiandoPagina.set(true);
    else this.#estadoResumen.set('cargando');
    const params = new HttpParams().set('pagina', pagina);
    this.paginaEnCurso?.unsubscribe();
    this.paginaEnCurso = this.http.get<ResumenInicioApi>(API_INICIO.resumen, { params }).subscribe({
      next: (r) => {
        const ahora = Date.now();
        this.#resumen.set({
          vacantesNuevas: r.vacantesNuevas,
          busqueda: r.busqueda,
          pagina: r.pagina,
          totalPaginas: r.totalPaginas,
          total: r.total,
          recomendadas: r.recomendadas.map((x) => ({ ...desdeApi(x.vacante, ahora), nueva: x.nueva })),
        });
        this.#cambiandoPagina.set(false);
        this.#estadoResumen.set('listo');
      },
      error: () => {
        this.#cambiandoPagina.set(false);
        this.#estadoResumen.set('error');
      },
    });
  }

  cargarCupo(): void {
    this.#estadoCupo.set('cargando');
    this.http.get<CupoAdaptaciones>(API_INICIO.cupoAdaptaciones).subscribe({
      next: (c) => {
        this.#cupo.set(c);
        this.#estadoCupo.set('listo');
      },
      error: () => this.#estadoCupo.set('error'),
    });
  }

  cargarUltimas(): void {
    this.#estadoUltimas.set('cargando');
    const params = new HttpParams().set('orden', 'recientes');
    this.http.get<ListaPostulaciones>(API_POSTULACIONES.lista, { params }).subscribe({
      next: (r) => {
        this.#ultimas.set(r.items.slice(0, 2));
        this.#estadoUltimas.set('listo');
      },
      error: () => this.#estadoUltimas.set('error'),
    });
  }

  guardarBusqueda(busqueda: Busqueda): Observable<Busqueda> {
    return this.http.put<Busqueda>(API_INICIO.busqueda, busqueda);
  }

  /** Tras guardar, la búsqueda nueva se refleja sin volver a pedir todo el resumen. */
  aplicarBusqueda(busqueda: Busqueda): void {
    this.#resumen.update((r) => (r ? { ...r, busqueda } : r));
  }
}
