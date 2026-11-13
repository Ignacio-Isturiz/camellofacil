import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';

import { Icono } from '../../../../shared/icono/icono';

/** Portada: propuesta de valor, y llamada a la acción. */
@Component({
  selector: 'app-portada',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, TranslocoPipe, Icono],
  templateUrl: './portada.html',
  styleUrl: './portada.scss',
})
export class Portada {}
