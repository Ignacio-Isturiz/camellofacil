import { ChangeDetectionStrategy, Component } from '@angular/core';

import { Encabezado } from '../../shared/encabezado/encabezado';
import { Pie } from '../../shared/pie/pie';
import { CarruselCarreras } from './secciones/carrusel-carreras/carrusel-carreras';
import { Pasos } from './secciones/pasos/pasos';
import { Portada } from './secciones/portada/portada';
import { Preguntas } from './secciones/preguntas/preguntas';
import { VacantesDestacadas } from './secciones/vacantes-destacadas/vacantes-destacadas';

/** Página pública de inicio (camello-web.pdf). */
@Component({
  selector: 'app-landing',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Encabezado, Portada, CarruselCarreras, Pasos, VacantesDestacadas, Preguntas, Pie],
  template: `
    <app-encabezado />
    <main>
      <app-portada />
      <app-carrusel-carreras />
      <app-pasos />
      <app-vacantes-destacadas />
      <app-preguntas />
    </main>
    <app-pie />
  `,
  styles: `
    :host {
      display: block;
      overflow-x: clip;
    }
  `,
})
export class Landing {}
