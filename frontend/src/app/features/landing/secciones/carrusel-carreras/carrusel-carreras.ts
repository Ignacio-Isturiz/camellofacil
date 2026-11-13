import { ChangeDetectionStrategy, Component } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';

import { Icono, NombreIcono } from '../../../../shared/icono/icono';

interface Carrera {
  clave: string;
  icono: NombreIcono;
}

/**
 * Carreras mostradas en el carrusel, cada una con un icono que la representa.
 * Las configura el administrador; hasta que exista ese módulo se mantiene esta lista
 * (tomada del diseño y del documento de requisitos).
 */
const CARRERAS: readonly Carrera[] = [
  { clave: 'derecho', icono: 'balanza' },
  { clave: 'contaduria', icono: 'calculadora' },
  { clave: 'administracion', icono: 'maletin' },
  { clave: 'economia', icono: 'grafico' },
  { clave: 'informatica', icono: 'codigo' },
  { clave: 'industrial', icono: 'engranaje' },
  { clave: 'datos', icono: 'datos' },
];

/** Carrusel continuo con las carreras cubiertas. Nunca se detiene. */
@Component({
  selector: 'app-carrusel-carreras',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoPipe, Icono],
  templateUrl: './carrusel-carreras.html',
  styleUrl: './carrusel-carreras.scss',
})
export class CarruselCarreras {
  protected readonly carreras = CARRERAS;
}
