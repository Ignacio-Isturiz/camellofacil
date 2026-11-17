import { ChangeDetectionStrategy, Component, computed, effect, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { map } from 'rxjs';

import { Icono } from '../../../shared/icono/icono';
import { EsqueletoDetalle } from '../esqueleto/esqueleto-detalle';
import { PanelAccion } from './accion/panel-accion';
import { VacantesServicio } from '../../../core/vacantes/vacantes.servicio';

/** /vacantes/:id — detalle de una vacante, por qué se recomienda y la acción de postular. */
@Component({
  selector: 'app-detalle-vacante',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, TranslocoPipe, Icono, EsqueletoDetalle, PanelAccion],
  templateUrl: './detalle-vacante.html',
  styleUrl: './detalle-vacante.scss',
})
export class DetalleVacante {
  private readonly servicio = inject(VacantesServicio);

  protected readonly id = toSignal(inject(ActivatedRoute).paramMap.pipe(map((p) => p.get('id') ?? '')), {
    initialValue: '',
  });

  protected readonly estado = this.servicio.estadoDetalle;
  protected readonly vacante = computed(() => {
    const d = this.servicio.detalle();
    return d && d.id === this.id() ? d : undefined;
  });
  protected readonly guardada = computed(() => this.servicio.guardadas().has(this.id()));

  protected readonly iniciales = computed(() =>
    (this.vacante()?.empresa ?? '')
      .split(' ')
      .filter((p) => p.length > 2)
      .slice(0, 2)
      .map((p) => p[0])
      .join('')
      .toUpperCase(),
  );

  constructor() {
    effect(() => {
      const id = this.id();
      if (id) this.servicio.cargarDetalle(id);
    });
  }

  protected reintentar(): void {
    this.servicio.cargarDetalle(this.id(), true);
  }

  protected guardar(): void {
    this.servicio.alternarGuardada(this.id());
  }
}
