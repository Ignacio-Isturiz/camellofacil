import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { TranslocoPipe } from '@jsverse/transloco';

import { CampoExtraido } from '../../../core/hoja-de-vida/hoja-de-vida.modelos';
import { Icono } from '../../../shared/icono/icono';

/**
 * Campo de un dato extraído de la hoja de vida. Según cómo lo leyó el backend se ve normal («leído»),
 * resaltado en dorado («revisa») o vacío con ayuda («no encontrado»). Al editarlo deja de pedir revisión.
 */
@Component({
  selector: 'app-campo-extraido',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, TranslocoPipe, Icono],
  template: `
    <div class="campo" [class.campo--revisa]="pendiente()">
      <label class="campo__etiqueta" [for]="idCampo()">
        {{ etiqueta() }}
        @if (campo().estado === 'no_encontrado' && !editado()) {
          <span class="campo__nota">{{ 'hoja.revisar.noEncontradaEtiqueta' | transloco }}</span>
        }
      </label>

      @if (tipo() === 'textarea') {
        <textarea
          class="entrada entrada--area"
          rows="3"
          [id]="idCampo()"
          [formControl]="control()"
          [placeholder]="placeholder()"
          (input)="alEditar()"
        ></textarea>
      } @else {
        <input
          class="entrada"
          [id]="idCampo()"
          [type]="tipo()"
          [formControl]="control()"
          [placeholder]="placeholder()"
          [readonly]="soloLectura()"
          [attr.autocomplete]="autocompletar()"
          (input)="alEditar()"
          (change)="alEditar()"
        />
      }

      @if (ayudaVisible(); as ayuda) {
        <p class="campo__ayuda">
          <app-icono nombre="info" style="--tam: 14px" />
          {{ ayuda }}
        </p>
      }
    </div>
  `,
  styleUrl: './campo-extraido.scss',
})
export class CampoExtraidoComponente {
  readonly control = input.required<FormControl<string>>();
  readonly campo = input.required<CampoExtraido>();
  readonly etiqueta = input.required<string>();
  readonly idCampo = input.required<string>();
  readonly tipo = input<'text' | 'tel' | 'email' | 'month' | 'textarea'>('text');
  readonly soloLectura = input(false);
  readonly placeholder = input('');
  readonly autocompletar = input<string | null>(null);
  /** Texto de ayuda para un dato no encontrado (ya traducido). */
  readonly ayudaNoEncontrado = input('');

  /** Se emite la primera vez que la persona toca el campo. */
  readonly resuelto = output<void>();

  protected readonly editado = signal(false);

  protected readonly pendiente = computed(() => this.campo().estado === 'revisa' && !this.editado());

  protected readonly ayudaVisible = computed(() => {
    if (this.editado()) return null;
    const c = this.campo();
    if (c.estado === 'revisa') return c.ayuda;
    if (c.estado === 'no_encontrado') return c.ayuda ?? this.ayudaNoEncontrado();
    return null;
  });

  protected alEditar(): void {
    if (this.editado()) return;
    this.editado.set(true);
    this.resuelto.emit();
  }
}
