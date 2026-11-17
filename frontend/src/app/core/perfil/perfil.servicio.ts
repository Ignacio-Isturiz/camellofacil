import { HttpClient } from '@angular/common/http';
import { computed, effect, inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { Sesion } from '../sesion/sesion';
import { API_PERFIL } from './perfil.api';
import { ActualizarPerfil, Carrera, CompletarPerfil, EstadoPerfil } from './perfil.modelos';

/**
 * Estado del perfil de quien inició sesión. Lo carga el marco de la aplicación al entrar; si no está completo,
 * el marco muestra el modal obligatorio. El backend también bloquea la API mientras tanto.
 */
@Injectable({ providedIn: 'root' })
export class PerfilServicio {
  private readonly http = inject(HttpClient);

  private readonly actual = signal<EstadoPerfil | null>(null);
  private carrerasEnCache: Promise<Carrera[]> | null = null;

  readonly estado = this.actual.asReadonly();
  /** true solo cuando ya se sabe que falta completarlo (mientras carga, no se muestra el modal). */
  readonly faltaCompletar = computed(() => this.actual()?.completo === false);

  constructor() {
    // Al cerrar sesión (o vencer) no debe quedar en memoria el perfil de la persona anterior.
    const sesion = inject(Sesion);
    effect(() => {
      if (!sesion.activa()) this.actual.set(null);
    });
  }

  async cargar(): Promise<EstadoPerfil> {
    const estado = await firstValueFrom(this.http.get<EstadoPerfil>(API_PERFIL.perfil));
    this.actual.set(estado);
    return estado;
  }

  /** Las carreras casi no cambian: se piden una vez por visita. */
  carreras(): Promise<Carrera[]> {
    this.carrerasEnCache ??= firstValueFrom(this.http.get<Carrera[]>(API_PERFIL.carreras)).catch((e: unknown) => {
      this.carrerasEnCache = null;
      throw e;
    });
    return this.carrerasEnCache;
  }

  async completar(datos: CompletarPerfil): Promise<void> {
    this.actual.set(await firstValueFrom(this.http.post<EstadoPerfil>(API_PERFIL.perfil, datos)));
  }

  async actualizar(datos: ActualizarPerfil): Promise<void> {
    this.actual.set(await firstValueFrom(this.http.put<EstadoPerfil>(API_PERFIL.perfil, datos)));
  }

  async cambiarAnalisisIa(activa: boolean): Promise<void> {
    await firstValueFrom(this.http.put<void>(API_PERFIL.analisisIa, { activa }));
    await this.cargar();
  }
}
