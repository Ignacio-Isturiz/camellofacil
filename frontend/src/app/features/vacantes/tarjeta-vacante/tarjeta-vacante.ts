import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';

import { Icono } from '../../../shared/icono/icono';
import { Vacante } from '../vacante';

/** Una vacante en la lista: etiquetas, cargo, empresa, datos clave y acciones. */
@Component({
  selector: 'app-tarjeta-vacante',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, TranslocoPipe, Icono],
  templateUrl: './tarjeta-vacante.html',
  styleUrl: './tarjeta-vacante.scss',
})
export class TarjetaVacante {
  readonly vacante = input.required<Vacante>();
  readonly guardada = input(false);
  readonly guardar = output<void>();
  readonly ocultar = output<void>();
}
