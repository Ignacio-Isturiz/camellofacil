import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';

import { Icono } from '../../../../shared/icono/icono';

/** Preguntas frecuentes en acordeón: solo una respuesta abierta a la vez. */
@Component({
  selector: 'app-preguntas',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, TranslocoPipe, Icono],
  templateUrl: './preguntas.html',
  styleUrl: './preguntas.scss',
})
export class Preguntas {
  protected readonly claves = ['registro', 'vacantes', 'inventa', 'datos', 'garantia'] as const;
  protected readonly abierta = signal<string | null>(null);

  protected alternar(clave: string): void {
    this.abierta.update((actual) => (actual === clave ? null : clave));
  }
}
