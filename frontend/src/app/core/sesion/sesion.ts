import { computed, Injectable, signal } from '@angular/core';

export interface Usuario {
  correo: string;
  nombre: string;
  iniciales: string;
  /** Clave de la carrera (la misma de `landing.carreras.nombres.*`). */
  carrera: string;
}

const CLAVE = 'camello.sesion-simulada';

/**
 * SESIÓN SIMULADA, solo para poder ver y construir las pantallas de quien ya inició sesión mientras no
 * exista el módulo de autenticación del backend.
 *
 * - No autentica nada ni protege nada: cualquiera puede llamar a `iniciarSimulada()`.
 *   La seguridad real (quién ve qué) la impone el backend; este guardia solo ordena la navegación.
 * - Al integrar el backend, se reemplaza el cuerpo de este servicio (login real + cookie de sesión)
 *   y el resto de la app no cambia: usa `usuario()` / `activa()` / `cerrar()`.
 */
@Injectable({ providedIn: 'root' })
export class Sesion {
  private readonly actual = signal<Usuario | null>(this.leer());

  readonly usuario = this.actual.asReadonly();
  readonly activa = computed(() => this.actual() !== null);

  iniciarSimulada(correo = 'valentina.r@unaula.edu.co'): void {
    const usuario = this.construir(correo.trim().toLowerCase());
    this.actual.set(usuario);
    this.guardar(usuario);
  }

  cerrar(): void {
    this.actual.set(null);
    this.guardar(null);
  }

  private construir(correo: string): Usuario {
    const partes = correo
      .split('@')[0]
      .split(/[._-]+/)
      .filter(Boolean);
    const nombre = partes.map((p) => p.charAt(0).toUpperCase() + p.slice(1)).join(' ') || 'Estudiante';
    const iniciales =
      partes.length > 1
        ? (partes[0][0] + partes[1][0]).toUpperCase()
        : (partes[0] ?? 'E').slice(0, 2).toUpperCase();
    return { correo, nombre, iniciales, carrera: 'datos' };
  }

  // El almacenamiento puede no estar disponible (modo privado): la app funciona igual, sin recordar la sesión.
  private leer(): Usuario | null {
    try {
      const crudo = sessionStorage.getItem(CLAVE);
      return crudo ? (JSON.parse(crudo) as Usuario) : null;
    } catch {
      return null;
    }
  }

  private guardar(usuario: Usuario | null): void {
    try {
      if (usuario) sessionStorage.setItem(CLAVE, JSON.stringify(usuario));
      else sessionStorage.removeItem(CLAVE);
    } catch {
      /* sin almacenamiento: se ignora */
    }
  }
}
