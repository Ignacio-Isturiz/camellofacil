import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Subscription } from 'rxjs';

import { DetalleVacanteApi, ListaVacantesApi, Vacante, VacanteDetalle, desdeApi } from './vacantes.modelos';
import { API_VACANTES } from './vacantes.api';
import { EstadoCarga } from '../http/estado-carga';

/** Pasado este tiempo, volver a la lista la recarga (las vacantes se actualizan durante el día). */
const VIGENCIA_MS = 5 * 60 * 1000;

/**
 * Vacantes y guardadas de quien inició sesión. Todo viene del API; guardar se aplica al instante en
 * pantalla y, si el servidor falla, se deshace.
 */
@Injectable({ providedIn: 'root' })
export class VacantesServicio {
  private readonly http = inject(HttpClient);

  readonly #todas = signal<readonly Vacante[]>([]);
  readonly todas = this.#todas.asReadonly();
  readonly #estado = signal<EstadoCarga>('cargando');
  readonly estado = this.#estado.asReadonly();
  readonly #guardadas = signal<ReadonlySet<string>>(new Set());
  readonly guardadas = this.#guardadas.asReadonly();

  /** Detalle de la vacante abierta; null mientras carga o si no existe. */
  readonly #detalle = signal<VacanteDetalle | null>(null);
  readonly detalle = this.#detalle.asReadonly();
  readonly #estadoDetalle = signal<EstadoCarga | 'no-encontrada'>('cargando');
  readonly estadoDetalle = this.#estadoDetalle.asReadonly();

  private cargadaEn = 0;
  /** Lista en vuelo: Inicio y la lista pueden pedirla a la vez; se hace una sola petición. */
  private listaEnCurso: Subscription | null = null;
  /** Detalle en vuelo: al abrir otra vacante, la petición anterior se cancela. */
  private detalleEnCurso: Subscription | null = null;

  /** Carga la lista si nunca se cargó o si ya está vieja. */
  cargar(forzar = false): void {
    if (!forzar && this.estado() === 'listo' && Date.now() - this.cargadaEn < VIGENCIA_MS) return;
    if (!forzar && this.listaEnCurso && !this.listaEnCurso.closed) return;
    if (this.estado() !== 'listo') this.#estado.set('cargando');
    this.listaEnCurso?.unsubscribe();
    this.listaEnCurso = this.http.get<ListaVacantesApi>(API_VACANTES.lista).subscribe({
      next: (r) => {
        const ahora = Date.now();
        this.#todas.set(r.vacantes.map((v) => desdeApi(v, ahora)));
        this.#guardadas.set(new Set(r.guardadas));
        this.cargadaEn = ahora;
        this.#estado.set('listo');
      },
      error: () => this.#estado.set('error'),
    });
  }

  /** Carga el detalle de una vacante; `forzar` lo vuelve a pedir aunque ya esté (botón «Reintentar»). */
  cargarDetalle(id: string, forzar = false): void {
    if (!forzar && this.detalle()?.id === id) return;
    this.#detalle.set(null);
    this.#estadoDetalle.set('cargando');
    this.detalleEnCurso?.unsubscribe();
    this.detalleEnCurso = this.http.get<DetalleVacanteApi>(API_VACANTES.vacante(id)).subscribe({
      next: (d) => {
        this.#detalle.set({
          ...desdeApi(d.vacante),
          descripcion: d.descripcion,
          responsabilidades: d.responsabilidades,
          requisitos: d.requisitos,
        });
        this.#estadoDetalle.set('listo');
      },
      error: (e: { status?: number }) => this.#estadoDetalle.set(e.status === 404 || e.status === 400 ? 'no-encontrada' : 'error'),
    });
  }

  alternarGuardada(id: string): void {
    const estaba = this.guardadas().has(id);
    this.#guardadas.update((actual) => alternar(actual, id));
    const peticion = estaba
      ? this.http.delete<void>(API_VACANTES.guardada(id))
      : this.http.put<void>(API_VACANTES.guardada(id), null);
    peticion.subscribe({ error: () => this.#guardadas.update((actual) => alternar(actual, id)) });
  }
}

function alternar(conjunto: ReadonlySet<string>, id: string): ReadonlySet<string> {
  const copia = new Set(conjunto);
  if (!copia.delete(id)) copia.add(id);
  return copia;
}
