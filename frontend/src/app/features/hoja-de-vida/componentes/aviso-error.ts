import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';

import { UltimoError } from '../../../core/http/ultimo-error';
import { Icono } from '../../../shared/icono/icono';

/** Mensaje de error de una carga, con el botón para reintentar. */
@Component({
  selector: 'app-aviso-error',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoPipe, Icono],
  template: `
    <div class="tarjeta aviso" role="alert">
      <app-icono nombre="alerta" style="--tam: 20px" />
      <div class="aviso__texto">
        <p class="aviso__titulo">{{ (titulo() ?? claves().titulo) | transloco }}</p>
        <p class="aviso__detalle">{{ (detalle() ?? claves().detalle) | transloco }}</p>
      </div>
      <button type="button" class="boton boton--borde boton--pequeno" (click)="reintentar.emit()">
        {{ 'hoja.error.reintentar' | transloco }}
      </button>
    </div>
  `,
  styles: `
    :host {
      display: block;
    }
    .aviso {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 12px 16px;
      padding: 16px 20px;
      color: var(--error);
    }
    .aviso__texto {
      flex: 1;
      min-width: 200px;
    }
    .aviso__titulo {
      color: var(--tinta);
      font: 700 15px/22px var(--fuente-titulo);
    }
    .aviso__detalle {
      color: var(--gris-600);
      font-size: 13px;
      line-height: 18px;
    }
  `,
})
export class AvisoError {
  /** Claves i18n opcionales; por defecto, el mensaje genérico. */
  readonly titulo = input<string | null>(null);
  readonly detalle = input<string | null>(null);
  readonly reintentar = output<void>();

  private readonly ultimo = inject(UltimoError);

  /** Sin claves propias, el mensaje depende de la causa real del fallo (no siempre es la conexión). */
  protected readonly claves = computed(() => {
    switch (this.ultimo.causa()) {
      case 'acceso':
        return { titulo: 'hoja.error.accesoTitulo', detalle: 'hoja.error.accesoDetalle' };
      case 'no-disponible':
        return { titulo: 'hoja.error.noDisponibleTitulo', detalle: 'hoja.error.noDisponibleDetalle' };
      case 'servidor':
        return { titulo: 'hoja.error.servidorTitulo', detalle: 'hoja.error.servidorDetalle' };
      default:
        return { titulo: 'hoja.error.titulo', detalle: 'hoja.error.detalle' };
    }
  });
}
