import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { EstiloPlantilla } from '../../../core/hoja-de-vida/hoja-de-vida.modelos';

/**
 * Miniatura decorativa de una plantilla: una hoja con líneas que sugieren su estilo.
 * No contiene datos ni texto; la plantilla real se ve en su vista previa.
 */
@Component({
  selector: 'app-miniatura-plantilla',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="hoja" [class]="'hoja hoja--' + estilo()" aria-hidden="true">
      <span class="linea titulo"></span>
      <span class="linea sub"></span>
      <span class="linea seccion"></span>
      <span class="linea texto"></span>
      <span class="linea texto corta"></span>
      <span class="linea seccion"></span>
      <span class="linea texto"></span>
      <span class="linea texto corta"></span>
      <span class="linea seccion"></span>
      <span class="linea texto"></span>
    </div>
  `,
  styleUrl: './miniatura-plantilla.scss',
})
export class MiniaturaPlantilla {
  readonly estilo = input.required<EstiloPlantilla>();
}
