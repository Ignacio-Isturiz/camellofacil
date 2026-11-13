import { ChangeDetectionStrategy, Component, input } from '@angular/core';

export type NombreIcono =
  | 'flecha'
  | 'chevron-abajo'
  | 'chevron-izq'
  | 'chevron-der'
  | 'check'
  | 'escudo'
  | 'rayo'
  | 'birrete'
  | 'documento'
  | 'usuario-mas'
  | 'subir'
  | 'campana'
  | 'pin'
  | 'guardar'
  | 'mas'
  | 'menu'
  | 'cerrar'
  | 'sobre'
  | 'edificio'
  | 'estrella'
  | 'comillas'
  | 'balanza'
  | 'calculadora'
  | 'maletin'
  | 'grafico'
  | 'codigo'
  | 'engranaje'
  | 'datos'
  | 'ojo'
  | 'ojo-cerrado'
  | 'candado'
  | 'info'
  | 'reenviar'
  | 'lupa'
  | 'filtros'
  | 'reloj'
  | 'externo'
  | 'bandera'
  | 'casa'
  | 'lista'
  | 'usuario'
  | 'lapiz'
  | 'descargar'
  | 'alerta'
  | 'papelera';

/** Iconos de trazo (24x24) que heredan el color del texto. Decorativos: no los lee el lector de pantalla. */
@Component({
  selector: 'app-icono',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './icono.html',
  styles: `
    :host {
      display: inline-flex;
      flex: none;
    }
    svg {
      width: var(--tam, 20px);
      height: var(--tam, 20px);
      fill: none;
      stroke: currentColor;
      stroke-width: 2;
      stroke-linecap: round;
      stroke-linejoin: round;
    }
  `,
})
export class Icono {
  readonly nombre = input.required<NombreIcono>();
}
