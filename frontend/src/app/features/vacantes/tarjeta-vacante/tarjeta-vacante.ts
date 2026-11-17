import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';

import { abrirEnBlanco } from '../../../core/postulaciones/pestana-externa';
import { PostulacionesServicio } from '../../../core/postulaciones/postulaciones.servicio';
import { Icono } from '../../../shared/icono/icono';
import { Vacante } from '../vacante';

/**
 * Una vacante en la lista: etiquetas, cargo, empresa, datos clave y acciones. El cargo lleva al detalle;
 * «Ver y postular» lleva directo a la oferta original (y queda en «Mis postulaciones»).
 */
@Component({
  selector: 'app-tarjeta-vacante',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, TranslocoPipe, Icono],
  templateUrl: './tarjeta-vacante.html',
  styleUrl: './tarjeta-vacante.scss',
})
export class TarjetaVacante {
  private readonly postulaciones = inject(PostulacionesServicio);

  readonly vacante = input.required<Vacante>();
  readonly guardada = input(false);
  readonly guardar = output<void>();

  /** La pestaña se abre dentro del clic (si no, el navegador la bloquea) y se completa con la URL del backend. */
  protected postular(): void {
    this.postulaciones.postularEnVacante(this.vacante().id, 'base', abrirEnBlanco());
  }
}
