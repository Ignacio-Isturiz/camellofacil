import { Injectable, signal } from '@angular/core';

import { Vacante } from './vacante';

export type EstadoCarga = 'cargando' | 'listo' | 'error';

/**
 * Acceso a las vacantes y a las marcas del usuario (guardadas / ocultas).
 *
 * Todavía no existe el módulo de vacantes del backend ni hay datos reales, así que `todas` está vacío y el
 * estado queda en `cargando`: las pantallas muestran esqueletos. Al integrar la API:
 *  - cargar `todas` desde el endpoint (HttpClient) y pasar `estado` a `listo` o `error`;
 *  - guardar/ocultar deben llamar a sus endpoints, que verifican que la marca es del usuario autenticado.
 */
@Injectable({ providedIn: 'root' })
export class VacantesServicio {
  readonly todas = signal<readonly Vacante[]>([]);
  readonly estado = signal<EstadoCarga>('cargando');
  readonly guardadas = signal<ReadonlySet<string>>(new Set());
  readonly ocultas = signal<ReadonlySet<string>>(new Set());

  porId(id: string): Vacante | undefined {
    return this.todas().find((v) => v.id === id);
  }

  alternarGuardada(id: string): void {
    this.guardadas.update((actual) => alternar(actual, id));
  }

  ocultar(id: string): void {
    this.ocultas.update((actual) => new Set(actual).add(id));
  }
}

function alternar(conjunto: ReadonlySet<string>, id: string): ReadonlySet<string> {
  const copia = new Set(conjunto);
  if (!copia.delete(id)) copia.add(id);
  return copia;
}
