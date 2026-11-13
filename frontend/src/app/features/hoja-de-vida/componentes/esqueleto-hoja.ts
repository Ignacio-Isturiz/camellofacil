import { ChangeDetectionStrategy, Component } from '@angular/core';

/** Marcador de la pantalla principal de la hoja de vida mientras cargan los datos. */
@Component({
  selector: 'app-esqueleto-hoja',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="esq" aria-hidden="true">
      <div class="tarjeta caja fila">
        <span class="bloque icono"></span>
        <div class="col">
          <span class="bloque linea media"></span>
          <span class="bloque linea corta"></span>
        </div>
      </div>
      <div class="dos">
        <div class="tarjeta caja">
          <span class="bloque linea media"></span>
          <span class="bloque numero"></span>
          <span class="bloque linea"></span>
        </div>
        <div class="tarjeta caja">
          <span class="bloque linea media"></span>
          <span class="bloque numero"></span>
          <span class="bloque linea"></span>
        </div>
      </div>
      <div class="tarjeta caja">
        <span class="bloque linea media"></span>
        <span class="bloque bloque-alto"></span>
        <span class="bloque bloque-alto"></span>
      </div>
    </div>
  `,
  styleUrl: './esqueleto-hoja.scss',
})
export class EsqueletoHoja {}
