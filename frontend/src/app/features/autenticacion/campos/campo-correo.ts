import { ChangeDetectionStrategy, Component, input, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { TranslocoPipe } from '@jsverse/transloco';

import { Icono } from '../../../shared/icono/icono';

/** Campo de correo institucional con su etiqueta, icono y mensaje de error. */
@Component({
  selector: 'app-campo-correo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, TranslocoPipe, Icono],
  template: `
    <div class="campo">
      <label class="campo__etiqueta" [for]="idCampo()">{{ 'auth.comun.correo' | transloco }}</label>
      <div class="campo__caja" [class.campo__caja--error]="hayError()">
        <app-icono nombre="sobre" style="--tam: 18px" />
        <input
          class="campo__entrada"
          [id]="idCampo()"
          type="email"
          inputmode="email"
          autocomplete="email"
          autocapitalize="none"
          spellcheck="false"
          [placeholder]="'auth.comun.correoPlaceholder' | transloco"
          [formControl]="control()"
          (input)="cambios.set(cambios() + 1)"
          (blur)="tocado.set(true)"
          [attr.aria-invalid]="hayError()"
          [attr.aria-describedby]="hayError() ? idCampo() + '-error' : null"
        />
      </div>
      @if (hayError()) {
        <p class="campo__error" [id]="idCampo() + '-error'" role="alert">
          {{ mensaje() | transloco }}
        </p>
      } @else if (ayuda()) {
        <p class="campo__ayuda">{{ ayuda()! | transloco }}</p>
      }
    </div>
  `,
  styles: `
    :host {
      display: block;
    }
  `,
})
export class CampoCorreo {
  readonly control = input.required<FormControl<string>>();
  readonly idCampo = input('correo');
  /** Clave i18n de un texto de ayuda bajo el campo (opcional). */
  readonly ayuda = input<string | null>(null);
  /** Fuerza a mostrar errores aunque el campo no se haya tocado (al intentar enviar). */
  readonly mostrarErrores = input(false);

  // Con detección sin zone, estas señales son las que vuelven a pintar el campo al escribir o salir de él.
  protected readonly tocado = signal(false);
  protected readonly cambios = signal(0);

  protected hayError(): boolean {
    this.cambios();
    return this.control().invalid && (this.tocado() || this.mostrarErrores());
  }

  protected mensaje(): string {
    return this.control().errors?.['correoDominio']
      ? 'auth.comun.errores.correoDominio'
      : 'auth.comun.errores.correoObligatorio';
  }
}
