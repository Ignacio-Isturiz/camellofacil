import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';

/** Ruta de navegación de las pantallas internas: «Mi hoja de vida / <pantalla actual>». */
@Component({
  selector: 'app-migas-hoja',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, TranslocoPipe],
  template: `
    <nav class="migas" [attr.aria-label]="'vacantes.detalle.migasAria' | transloco">
      <a routerLink="/hoja-de-vida">{{ 'hoja.principal.titulo' | transloco }}</a>
      <span aria-hidden="true">/</span>
      <span aria-current="page">{{ actual() }}</span>
    </nav>
  `,
  styles: `
    .migas {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
      color: var(--gris-600);
      font-size: 12px;
      line-height: 18px;
    }
    a {
      color: var(--rojo);
      text-underline-offset: 2px;
    }
  `,
})
export class MigasHoja {
  /** Nombre ya traducido de la pantalla actual. */
  readonly actual = input.required<string>();
}
