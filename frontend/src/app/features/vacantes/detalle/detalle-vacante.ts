import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { map } from 'rxjs';

import { Icono } from '../../../shared/icono/icono';
import { EsqueletoDetalle } from '../esqueleto/esqueleto-detalle';
import { PanelAccion } from './accion/panel-accion';
import { VacantesServicio } from '../vacantes-servicio';

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
  private readonly router = inject(Router);

  protected readonly id = toSignal(inject(ActivatedRoute).paramMap.pipe(map((p) => p.get('id') ?? '')), {
    initialValue: '',
  });

  protected readonly estado = this.servicio.estado;
  protected readonly vacante = computed(() => this.servicio.porId(this.id()));
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

  protected guardar(): void {
    this.servicio.alternarGuardada(this.id());
  }

  protected ocultar(): void {
    this.servicio.ocultar(this.id());
    void this.router.navigate(['/vacantes']);
  }
}
