import { ChangeDetectionStrategy, Component, input, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { TranslocoPipe } from '@jsverse/transloco';

import { Icono } from '../../../shared/icono/icono';

/** Campo de contraseña con botón para mostrarla u ocultarla. Admite un enlace al lado de la etiqueta. */
@Component({
  selector: 'app-campo-contrasena',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, TranslocoPipe, Icono],
  template: `
    <div class="campo">
      <div class="campo__fila">
        <label class="campo__etiqueta" [for]="idCampo()">{{ 'auth.comun.contrasena' | transloco }}</label>
        <ng-content />
      </div>
      <div class="campo__caja" [class.campo__caja--error]="hayError()">
        <input
          class="campo__entrada"
          [id]="idCampo()"
          [type]="visible() ? 'text' : 'password'"
          [autocomplete]="autocompletar()"
          autocapitalize="none"
          spellcheck="false"
          [formControl]="control()"
          (input)="cambios.set(cambios() + 1)"
          (blur)="tocado.set(true)"
          [attr.aria-invalid]="hayError()"
          [attr.aria-describedby]="hayError() ? idCampo() + '-error' : null"
        />
        <button
          type="button"
          class="campo__ojo"
          [attr.aria-label]="(visible() ? 'auth.comun.ocultarContrasena' : 'auth.comun.mostrarContrasena') | transloco"
          [attr.aria-pressed]="visible()"
          (click)="visible.set(!visible())"
        >
          <app-icono [nombre]="visible() ? 'ojo-cerrado' : 'ojo'" style="--tam: 18px" />
        </button>
      </div>
      @if (hayError()) {
        <p class="campo__error" [id]="idCampo() + '-error'" role="alert">
          {{ mensaje() | transloco }}
        </p>
      }
    </div>
  `,
  styles: `
    :host {
      display: block;
    }
  `,
})
export class CampoContrasena {
  readonly control = input.required<FormControl<string>>();
  readonly idCampo = input('contrasena');
  /** `current-password` al ingresar, `new-password` al crear la cuenta (ayuda a los gestores de contraseñas). */
  readonly autocompletar = input<'current-password' | 'new-password'>('current-password');
  readonly mostrarErrores = input(false);

  protected readonly visible = signal(false);
  protected readonly tocado = signal(false);
  protected readonly cambios = signal(0);

  protected hayError(): boolean {
    this.cambios();
    return this.control().invalid && (this.tocado() || this.mostrarErrores());
  }

  protected mensaje(): string {
    return this.control().errors?.['contrasenaDebil']
      ? 'auth.comun.errores.contrasenaDebil'
      : 'auth.comun.errores.contrasenaObligatoria';
  }
}
