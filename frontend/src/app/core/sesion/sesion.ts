import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ApiConfiguration } from '../api/api-configuration';

export interface Usuario {
  correo: string;
  nombre: string;
  iniciales: string;
}

/** Respuesta de GET /api/v1/auth/me. */
interface UsuarioApi {
  correo: string;
  nombre: string;
}

/** Ruta de «quién soy»: un 401 aquí solo significa «no hay sesión», no que haya vencido. */
export const RUTA_YO = '/api/v1/auth/me';

/**
 * Sesión de quien entró con su cuenta Microsoft de UNAULA (único medio de ingreso).
 *
 * La sesión real vive en el backend (cookie, vence tras 8 horas sin actividad). Aquí solo se guarda en
 * memoria quién es, para pintar la interfaz; nada personal va a localStorage ni sessionStorage.
 */
@Injectable({ providedIn: 'root' })
export class Sesion {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiConfiguration);

  private readonly actual = signal<Usuario | null>(null);

  readonly usuario = this.actual.asReadonly();
  readonly activa = computed(() => this.actual() !== null);

  /** URL a la que se navega (página completa) para entrar con Microsoft. */
  get urlMicrosoft(): string {
    return `${this.api.rootUrl}/oauth2/authorization/microsoft`;
  }

  /** Pregunta al backend si hay sesión (cookie). Devuelve true si la hay. */
  async restaurar(): Promise<boolean> {
    try {
      const datos = await firstValueFrom(this.http.get<UsuarioApi>(`${this.api.rootUrl}${RUTA_YO}`));
      this.actual.set(construir(datos.correo, datos.nombre));
      return true;
    } catch {
      this.actual.set(null);
      return false;
    }
  }

  /** Olvida la sesión en memoria (p. ej. cuando el backend responde que venció). */
  olvidar(): void {
    this.actual.set(null);
  }

  async cerrar(): Promise<void> {
    this.olvidar();
    try {
      await firstValueFrom(this.http.post(`${this.api.rootUrl}/api/v1/auth/logout`, null));
    } catch {
      /* si falla, la sesión del servidor vence sola */
    }
  }
}

function construir(correo: string, nombreCompleto: string): Usuario {
  const nombre = nombreCompleto.trim() || correo.split('@')[0];
  const partes = nombre.split(/[\s._-]+/).filter(Boolean);
  const iniciales =
    partes.length > 1 ? (partes[0][0] + partes[1][0]).toUpperCase() : (partes[0] ?? 'E').slice(0, 2).toUpperCase();
  return { correo, nombre, iniciales };
}
