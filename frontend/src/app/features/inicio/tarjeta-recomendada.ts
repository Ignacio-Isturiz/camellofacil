import { ChangeDetectionStrategy, Component, inject, input, output, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';

import { InicioServicio } from '../../core/inicio/inicio.servicio';
import { Recomendada } from '../../core/inicio/inicio.modelos';
import { Icono } from '../../shared/icono/icono';
import { ToastServicio } from '../../shared/toast/toast.servicio';

/** Una recomendación de hoy: datos clave, «por qué» y «¿Te sirvió?». */
@Component({
  selector: 'app-tarjeta-recomendada',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, TranslocoPipe, Icono],
  template: `
    <article class="tarjeta rec">
      <div class="rec__tope">
        <div class="rec__etiquetas">
          <span class="etiqueta">{{ 'vacantes.tarjeta.' + vacante().tipo | transloco }}</span>
          <span class="etiqueta etiqueta--exito">
            <app-icono nombre="check" style="--tam: 13px" />
            {{ 'vacantes.tarjeta.coincidencia' | transloco: { n: vacante().coincidencia } }}
          </span>
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
        <li><app-icono nombre="pin" style="--tam: 14px" />{{ vacante().ciudad }} · {{ 'vacantes.modalidad.' + vacante().modalidad | transloco }}</li>
        <li>
          <app-icono nombre="reloj" style="--tam: 14px" />
          @switch (vacante().dias) {
            @case (0) { {{ 'vacantes.tarjeta.hoy' | transloco }} }
            @case (1) { {{ 'vacantes.tarjeta.ayer' | transloco }} }
            @default { {{ 'vacantes.tarjeta.hace' | transloco: { n: vacante().dias } }} }
          }
        </li>
      </ul>

      @if (vacante().razones[0]; as razon) {
        <p class="rec__porque"><strong>{{ 'inicio.porQue' | transloco }}</strong> {{ razon.texto }}</p>
      }

      <div class="rec__pie">
        @if (voto() === null) {
          <span class="rec__sirvio" id="sirvio-{{ vacante().id }}">{{ 'inicio.teSirvio' | transloco }}</span>
          <div class="rec__votos" role="group" [attr.aria-labelledby]="'sirvio-' + vacante().id">
            <button type="button" class="rec__voto" [disabled]="enviando()" (click)="votar(true)">{{ 'inicio.si' | transloco }}</button>
            <button type="button" class="rec__voto" [disabled]="enviando()" (click)="votar(false)">{{ 'inicio.no' | transloco }}</button>
          </div>
        } @else {
          <span class="rec__gracias" role="status">{{ 'inicio.gracias' | transloco }}</span>
        }
        <a
          class="rec__ir"
          [routerLink]="['/vacantes', vacante().id]"
          [attr.aria-label]="'vacantes.tarjeta.verYPostular' | transloco"
        >
          <app-icono nombre="flecha" style="--tam: 18px" />
        </a>
      </div>
    </article>
  `,
  styleUrl: './tarjeta-recomendada.scss',
})
export class TarjetaRecomendada {
  private readonly inicio = inject(InicioServicio);
  private readonly toast = inject(ToastServicio);
  private readonly i18n = inject(TranslocoService);

  readonly vacante = input.required<Recomendada>();
  readonly guardada = input(false);
  readonly guardar = output<void>();

  protected readonly voto = signal<boolean | null>(null);
  protected readonly enviando = signal(false);

  protected votar(sirvio: boolean): void {
    this.enviando.set(true);
    this.inicio.enviarUtilidad(this.vacante().id, sirvio).subscribe({
      next: () => {
        this.voto.set(sirvio);
        this.enviando.set(false);
      },
      error: () => {
        this.enviando.set(false);
        this.toast.mostrar(this.i18n.translate('inicio.errorVoto'));
      },
    });
  }
}
