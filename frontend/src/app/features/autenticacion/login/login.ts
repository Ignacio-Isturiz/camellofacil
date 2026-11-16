import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';

import { Sesion } from '../../../core/sesion/sesion';
import { Icono } from '../../../shared/icono/icono';
import { LogoMicrosoft } from '../../../shared/logo-microsoft/logo-microsoft';
import { MarcoAutenticacion } from '../marco-autenticacion/marco-autenticacion';

/** Avisos según `?error=` (lo pone el backend al fallar Microsoft, o el frontend al vencer la sesión). */
const AVISOS: Record<string, string> = {
  dominio: 'auth.login.errorDominio',
  microsoft: 'auth.login.errorMicrosoft',
  expirada: 'auth.login.sesionExpirada',
};

/** /login — el único ingreso es con la cuenta Microsoft de UNAULA. */
@Component({
  selector: 'app-login',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, TranslocoPipe, Icono, LogoMicrosoft, MarcoAutenticacion],
  templateUrl: './login.html',
})
export class Login {
  private readonly sesion = inject(Sesion);

  protected readonly aviso = AVISOS[inject(ActivatedRoute).snapshot.queryParamMap.get('error') ?? ''] ?? null;

  /** Navegación de página completa: el backend lleva a Microsoft y, al volver, deja la cookie de sesión. */
  protected conMicrosoft(): void {
    window.location.assign(this.sesion.urlMicrosoft);
  }
}
