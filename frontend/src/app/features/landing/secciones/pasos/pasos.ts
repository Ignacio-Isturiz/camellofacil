import { ChangeDetectionStrategy, Component } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';

import { Icono, NombreIcono } from '../../../../shared/icono/icono';

interface Paso {
  icono: NombreIcono;
  clave: string;
}

/** Sección «¿Cómo funciona?»: los cuatro pasos para llegar a una postulación. */
@Component({
  selector: 'app-pasos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoPipe, Icono],
  templateUrl: './pasos.html',
  styleUrl: './pasos.scss',
})
export class Pasos {
  protected readonly pasos: readonly Paso[] = [
    { icono: 'usuario-mas', clave: 'cuenta' },
    { icono: 'subir', clave: 'hoja' },
    { icono: 'campana', clave: 'vacantes' },
    { icono: 'flecha', clave: 'postula' },
  ];
}
