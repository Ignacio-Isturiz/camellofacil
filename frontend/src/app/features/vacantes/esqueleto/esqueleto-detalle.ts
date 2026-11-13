import { ChangeDetectionStrategy, Component } from '@angular/core';

/** Marcador de la columna principal del detalle de una vacante mientras cargan los datos. */
@Component({
  selector: 'app-esqueleto-detalle',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="principal" aria-hidden="true">
      <div class="tarjeta caja">
        <div class="fila">
          <span class="bloque logo"></span>
          <div class="columna">
            <span class="bloque linea linea--media"></span>
            <span class="bloque linea linea--corta"></span>
          </div>
        </div>
        <span class="bloque titulo"></span>
        <div class="fila">
          <span class="bloque etiqueta"></span>
          <span class="bloque etiqueta"></span>
          <span class="bloque etiqueta"></span>
        </div>
      </div>
      <div class="tarjeta caja">
        <span class="bloque linea linea--media"></span>
        <span class="bloque razon"></span>
        <span class="bloque razon"></span>
      </div>
      <div class="tarjeta caja">
        <span class="bloque linea linea--media"></span>
        <span class="bloque linea"></span>
        <span class="bloque linea"></span>
        <span class="bloque linea linea--corta"></span>
      </div>
    </div>
  `,
  styleUrl: './esqueleto-detalle.scss',
})
export class EsqueletoDetalle {}
