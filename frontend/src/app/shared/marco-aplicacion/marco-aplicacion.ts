import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';

import { Sesion } from '../../core/sesion/sesion';
import { Icono } from '../icono/icono';
import { Logo } from '../logo/logo';
import { Toasts } from '../toast/toasts';

/** Marco de las pantallas de quien ya inició sesión: encabezado con navegación, contenido y aviso legal. */
@Component({
  selector: 'app-marco-aplicacion',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, TranslocoPipe, Icono, Logo, Toasts],
  templateUrl: './marco-aplicacion.html',
  styleUrl: './marco-aplicacion.scss',
})
export class MarcoAplicacion {
  private readonly sesion = inject(Sesion);
  private readonly router = inject(Router);

  protected readonly usuario = this.sesion.usuario;
  protected readonly menuAbierto = signal(false);

  protected cerrarSesion(): void {
    this.menuAbierto.set(false);
    this.sesion.cerrar();
    void this.router.navigate(['/']);
  }
}
