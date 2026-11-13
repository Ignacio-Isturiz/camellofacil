import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';

import { Logo } from '../../../shared/logo/logo';

/**
 * Marco común de las pantallas de autenticación: panel rojo con el mensaje y el camello a la izquierda
 * (burbujas animadas de fondo) y, a la derecha, el contenido proyectado (el formulario de cada pantalla).
 */
@Component({
  selector: 'app-marco-autenticacion',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, TranslocoPipe, Logo],
  templateUrl: './marco-autenticacion.html',
  styleUrl: './marco-autenticacion.scss',
})
export class MarcoAutenticacion {
  /** Prefijo i18n de la pantalla (p. ej. `auth.login`): de ahí salen `panelTitulo`, `punto1`, `punto2` y `punto3`. */
  readonly prefijo = input.required<string>();
  protected readonly puntos = [1, 2, 3] as const;
}
