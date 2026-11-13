import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';

import { ValoresHoja } from '../../../core/hoja-de-vida/hoja-de-vida.modelos';

/**
 * Vista previa de la hoja con lo que hay en el formulario (sin agregar nada). Lo que aún hay que revisar
 * se marca en dorado. Es solo una representación para comparar; el documento real lo genera el backend.
 */
@Component({
  selector: 'app-vista-previa-hoja',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoPipe],
  templateUrl: './vista-previa-hoja.html',
  styleUrl: './vista-previa-hoja.scss',
})
export class VistaPreviaHoja {
  private readonly i18n = inject(TranslocoService);

  readonly valores = input.required<ValoresHoja>();
  readonly correo = input('');
  /** Claves de los datos pendientes de revisión (p. ej. `telefono`, `exp.<id>.inicio`). */
  readonly marcas = input<ReadonlySet<string>>(new Set());

  protected marcado(clave: string): boolean {
    return this.marcas().has(clave);
  }

  /** `AAAA-MM` → «ene 2024» en el idioma activo. Vacío si no hay fecha. */
  protected mes(valor: string): string {
    const m = /^(\d{4})-(\d{2})$/.exec(valor);
    if (!m) return '';
    const fecha = new Date(Number(m[1]), Number(m[2]) - 1, 1);
    return new Intl.DateTimeFormat(this.i18n.getActiveLang(), { month: 'short', year: 'numeric' })
      .format(fecha)
      .replace(/\./g, '');
  }

  protected rango(inicio: string, fin: string): string {
    const a = this.mes(inicio);
    const b = this.mes(fin);
    return a && b ? `${a} – ${b}` : a || b;
  }
}
