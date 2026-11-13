import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Marca: cuadrado rojo con la "M" del camello + nombre. `claro` es para fondos rojos. */
@Component({
  selector: 'app-logo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './logo.html',
  styleUrl: './logo.scss',
  host: { '[class.claro]': 'claro()' },
})
export class Logo {
  readonly claro = input(false);
}
