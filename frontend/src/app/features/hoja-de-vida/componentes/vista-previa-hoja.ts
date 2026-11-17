import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';

import { ValoresHoja } from '../../../core/hoja-de-vida/hoja-de-vida.modelos';
import { rangoMeses } from '../../../shared/fechas/fechas';

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

  protected rango(inicio: string, fin: string): string {
    return rangoMeses(inicio, fin, this.i18n.getActiveLang());
  }
}
