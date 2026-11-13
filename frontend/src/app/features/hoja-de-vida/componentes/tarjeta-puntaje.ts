import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';

import { Puntaje } from '../../../core/hoja-de-vida/hoja-de-vida.modelos';

/** Tarjeta con un puntaje de 0 a 100, su barra y su nivel. El contenido proyectado va bajo la barra. */
@Component({
  selector: 'app-tarjeta-puntaje',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoPipe],
  template: `
    <section class="tarjeta puntaje" [attr.aria-label]="titulo()">
      <div class="puntaje__cabecera">
        <h2>{{ titulo() }}</h2>
        @if (puntaje(); as p) {
          <span class="insignia" [class]="'insignia insignia--' + p.nivel">
            {{ 'hoja.nivel.' + p.nivel | transloco }}
          </span>
        }
      </div>

      @if (puntaje(); as p) {
        <p class="puntaje__valor" [class.puntaje__valor--cargando]="cargando()">
          <strong>{{ p.valor }}</strong><span>/100</span>
        </p>
        <div class="barra" role="meter" aria-valuemin="0" aria-valuemax="100" [attr.aria-valuenow]="p.valor" [attr.aria-label]="titulo()">
          <span class="barra__relleno" [class]="'barra__relleno barra__relleno--' + p.nivel" [style.width.%]="p.valor"></span>
        </div>
        <p class="puntaje__detalle">{{ p.detalle }}</p>
      }
      <ng-content />
    </section>
  `,
  styleUrl: './tarjeta-puntaje.scss',
})
export class TarjetaPuntaje {
  readonly titulo = input.required<string>();
  readonly puntaje = input.required<Puntaje | null>();
  readonly cargando = input(false);
}
