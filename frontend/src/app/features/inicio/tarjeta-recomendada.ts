import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';

import { Recomendada } from '../../core/inicio/inicio.modelos';
import { abrirEnBlanco } from '../../core/postulaciones/pestana-externa';
import { PostulacionesServicio } from '../../core/postulaciones/postulaciones.servicio';
import { Icono } from '../../shared/icono/icono';

/**
 * Una recomendación de hoy: datos clave y «Ver y postular». El cargo lleva al detalle; el botón lleva
 * directo a la oferta original (y queda en «Mis postulaciones»), igual que en la lista de vacantes.
 */
@Component({
  selector: 'app-tarjeta-recomendada',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, TranslocoPipe, Icono],
  template: `
    <article class="tarjeta rec">
      <div class="rec__tope">
        <div class="rec__etiquetas">
          <span class="etiqueta">{{ 'vacantes.tarjeta.' + vacante().tipo | transloco }}</span>
          @if (vacante().coincidencia !== null) {
            <span class="etiqueta etiqueta--exito">
              <app-icono nombre="check" style="--tam: 13px" />
              {{ 'vacantes.tarjeta.coincidencia' | transloco: { n: vacante().coincidencia } }}
            </span>
          }
        </div>
        @if (vacante().nueva) {
          <span class="rec__nueva">{{ 'inicio.nueva' | transloco }}</span>
        }
        <button
          type="button"
          class="rec__guardar"
          [class.rec__guardar--activo]="guardada()"
          [attr.aria-pressed]="guardada()"
          [attr.aria-label]="(guardada() ? 'vacantes.tarjeta.quitarGuardada' : 'vacantes.tarjeta.guardar') | transloco"
          (click)="guardar.emit()"
        >
          <app-icono nombre="guardar" style="--tam: 18px" />
        </button>
      </div>

      <h3 class="rec__cargo">
        <a [routerLink]="['/vacantes', vacante().id]">{{ vacante().cargo }}</a>
      </h3>
      <p class="rec__empresa">{{ vacante().empresa }}</p>

      <ul class="rec__datos">
        <li>
          <app-icono nombre="pin" style="--tam: 14px" />
          @if (vacante().ciudad) {
            {{ vacante().ciudad }} ·
          }
          {{ 'vacantes.modalidad.' + vacante().modalidad | transloco }}
        </li>
        <li>
          <app-icono nombre="reloj" style="--tam: 14px" />
          @switch (vacante().dias) {
            @case (0) { {{ 'vacantes.tarjeta.hoy' | transloco }} }
            @case (1) { {{ 'vacantes.tarjeta.ayer' | transloco }} }
            @default { {{ 'vacantes.tarjeta.hace' | transloco: { n: vacante().dias } }} }
          }
        </li>
      </ul>

      <div class="rec__pie">
        <button type="button" class="boton boton--primario boton--pequeno rec__postular" (click)="postular()">
          {{ 'vacantes.tarjeta.verYPostular' | transloco }}
          <app-icono nombre="externo" style="--tam: 16px" />
          <span class="solo-lectores">{{ 'postulaciones.nuevaPestana' | transloco }}</span>
        </button>
      </div>
    </article>
  `,
  styleUrl: './tarjeta-recomendada.scss',
})
export class TarjetaRecomendada {
  private readonly postulaciones = inject(PostulacionesServicio);

  readonly vacante = input.required<Recomendada>();
  readonly guardada = input(false);
  readonly guardar = output<void>();

  /** La pestaña se abre dentro del clic (si no, el navegador la bloquea) y se completa con la URL del backend. */
  protected postular(): void {
    this.postulaciones.postularEnVacante(this.vacante().id, 'base', abrirEnBlanco());
  }
}
