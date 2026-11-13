import { ChangeDetectionStrategy, Component, input, output, signal, viewChild } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';

import { HojaUsada } from '../../../../core/postulaciones/postulaciones.modelos';
import { Dialogo } from '../../../hoja-de-vida/componentes/dialogo';

/** Pregunta con qué hoja ir: la adaptada (si existe) o la hoja base. */
@Component({
  selector: 'app-dialogo-postular',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoPipe, Dialogo],
  template: `
    <app-dialogo #dialogo [titulo]="'vacantes.postular.titulo' | transloco">
      <fieldset class="opciones">
        <legend>{{ 'vacantes.postular.pregunta' | transloco }}</legend>
        @if (tieneAdaptada()) {
          <label class="opcion" [class.opcion--activa]="elegida() === 'adaptada'">
            <input type="radio" name="hoja" value="adaptada" [checked]="elegida() === 'adaptada'" (change)="elegida.set('adaptada')" />
            <span>
              <strong>{{ 'vacantes.postular.adaptada' | transloco }}</strong>
              <small>{{ 'vacantes.postular.adaptadaDetalle' | transloco }}</small>
            </span>
          </label>
        }
        <label class="opcion" [class.opcion--activa]="elegida() === 'base'">
          <input type="radio" name="hoja" value="base" [checked]="elegida() === 'base'" (change)="elegida.set('base')" />
          <span>
            <strong>{{ 'vacantes.postular.base' | transloco }}</strong>
            <small>{{ 'vacantes.postular.baseDetalle' | transloco }}</small>
          </span>
        </label>
      </fieldset>
      <p class="nota">{{ 'vacantes.postular.nota' | transloco }}</p>

      <ng-container pie>
        <button type="button" class="boton boton--borde boton--pequeno" (click)="cerrar()">{{ 'hoja.versiones.cancelar' | transloco }}</button>
        <button type="button" class="boton boton--primario boton--pequeno" (click)="confirmar()">
          {{ 'vacantes.postular.confirmar' | transloco }}
        </button>
      </ng-container>
    </app-dialogo>
  `,
  styleUrl: './dialogo-postular.scss',
})
export class DialogoPostular {
  readonly tieneAdaptada = input(false);
  /** Se emite dentro del clic de «Postular», para que el padre pueda abrir la pestaña sin que la bloqueen. */
  readonly elegir = output<HojaUsada>();

  private readonly dialogo = viewChild.required<Dialogo>('dialogo');
  protected readonly elegida = signal<HojaUsada>('base');

  abrir(): void {
    this.elegida.set(this.tieneAdaptada() ? 'adaptada' : 'base');
    this.dialogo().abrir();
  }

  cerrar(): void {
    this.dialogo().cerrar();
  }

  protected confirmar(): void {
    this.elegir.emit(this.elegida());
    this.cerrar();
  }
}
