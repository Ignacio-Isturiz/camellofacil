import { ChangeDetectionStrategy, Component } from '@angular/core';

/** Marcador de una tarjeta de vacante mientras cargan los datos. */
@Component({
  selector: 'app-esqueleto-vacante',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="tarjeta vac" aria-hidden="true">
      <div class="vac__texto">
        <div class="fila">
          <span class="bloque etiqueta"></span>
          <span class="bloque etiqueta etiqueta--larga"></span>
        </div>
        <span class="bloque cargo"></span>
        <span class="bloque empresa"></span>
        <div class="fila">
          <span class="bloque dato"></span>
          <span class="bloque dato"></span>
          <span class="bloque dato"></span>
        </div>
      </div>
      <div class="fila fila--acciones">
        <span class="bloque icono"></span>
        <span class="bloque icono"></span>
        <span class="bloque boton-falso"></span>
      </div>
    </div>
  `,
  styleUrl: './esqueleto-vacante.scss',
})
export class EsqueletoVacante {}
