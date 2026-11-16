import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ApiConfiguration } from '../api/api-configuration';
import { ActualizarPerfil, Carrera, CompletarPerfil, EstadoPerfil } from './perfil.modelos';

/**
 * Estado del perfil de quien inició sesión. Lo carga el marco de la aplicación al entrar; si no está completo,
 * el marco muestra el modal obligatorio. El backend también bloquea la API mientras tanto.
 */
@Injectable({ providedIn: 'root' })
export class PerfilServicio {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiConfiguration);

  private readonly actual = signal<EstadoPerfil | null>(null);
  private carrerasEnCache: Promise<Carrera[]> | null = null;

  readonly estado = this.actual.asReadonly();
  /** true solo cuando ya se sabe que falta completarlo (mientras carga, no se muestra el modal). */
  readonly faltaCompletar = computed(() => this.actual()?.completo === false);

  private get url(): string {
    return `${this.api.rootUrl}/api/v1/perfil`;
  }

  async cargar(): Promise<EstadoPerfil> {
    const estado = await firstValueFrom(this.http.get<EstadoPerfil>(this.url));
    this.actual.set(estado);
    return estado;
  }

  /** Las carreras casi no cambian: se piden una vez por visita. */
  carreras(): Promise<Carrera[]> {
    this.carrerasEnCache ??= firstValueFrom(this.http.get<Carrera[]>(`${this.url}/carreras`)).catch((e: unknown) => {
      this.carrerasEnCache = null;
      throw e;
    });
    return this.carrerasEnCache;
  }

  async completar(datos: CompletarPerfil): Promise<void> {
    this.actual.set(await firstValueFrom(this.http.post<EstadoPerfil>(this.url, datos)));
  }

  async actualizar(datos: ActualizarPerfil): Promise<void> {
    this.actual.set(await firstValueFrom(this.http.put<EstadoPerfil>(this.url, datos)));
  }

  async cambiarAnalisisIa(activa: boolean): Promise<void> {
    await firstValueFrom(this.http.put<void>(`${this.api.rootUrl}/api/v1/legal/analisis-ia`, { activa }));
    await this.cargar();
  }

  /** Al cerrar sesión no debe quedar el perfil de la persona anterior en memoria. */
  olvidar(): void {
    this.actual.set(null);
  }
}
