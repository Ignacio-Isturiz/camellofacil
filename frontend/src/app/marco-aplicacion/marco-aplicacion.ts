import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';

import { PerfilServicio } from '../core/perfil/perfil.servicio';
import { Sesion } from '../core/sesion/sesion';
import { CompletarPerfil } from '../features/perfil/completar-perfil/completar-perfil';
import { Icono } from '../shared/icono/icono';
import { Logo } from '../shared/logo/logo';
import { Toasts } from '../shared/toast/toasts';

/**
 * Marco de las pantallas de quien ya inició sesión: encabezado con navegación, contenido y aviso legal.
 * Al entrar carga el perfil; si falta completarlo, muestra el modal obligatorio del primer ingreso.
 */
@Component({
  selector: 'app-marco-aplicacion',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, TranslocoPipe, Icono, Logo, Toasts, CompletarPerfil],
  templateUrl: './marco-aplicacion.html',
  styleUrl: './marco-aplicacion.scss',
})
export class MarcoAplicacion {
  private readonly sesion = inject(Sesion);
  private readonly router = inject(Router);

  protected readonly perfil = inject(PerfilServicio);
  protected readonly usuario = this.sesion.usuario;
  protected readonly menuAbierto = signal(false);

  constructor() {
    // Si falla (red, sesión vencida), no se bloquea la pantalla: el backend igual protege la API.
    this.perfil.cargar().catch(() => undefined);
  }

  protected cerrarSesion(): void {
    this.menuAbierto.set(false);
    void this.sesion.cerrar();
    void this.router.navigate(['/']);
  }
}
