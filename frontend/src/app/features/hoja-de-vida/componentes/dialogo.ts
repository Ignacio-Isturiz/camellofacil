import { ChangeDetectionStrategy, Component, ElementRef, input, output, viewChild } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';

import { Icono } from '../../../shared/icono/icono';

/**
 * Ventana de diálogo sobre el `<dialog>` nativo (foco atrapado, Esc para cerrar y fondo inerte sin código extra).
 * En móvil se muestra como hoja que sube desde abajo. Se abre con `abrir()` desde el componente padre.
 */
@Component({
  selector: 'app-dialogo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoPipe, Icono],
  template: `
    <dialog #caja class="caja" [class.caja--ancha]="ancho()" [attr.aria-labelledby]="idTitulo" (close)="cerrado.emit()" (click)="alFondo($event)">
      <div class="contenido">
        <header class="cabecera">
          <h2 [id]="idTitulo">{{ titulo() }}</h2>
          <button type="button" class="cerrar" [attr.aria-label]="'hoja.dialogo.cerrar' | transloco" (click)="cerrar()">
            <app-icono nombre="cerrar" style="--tam: 18px" />
          </button>
        </header>
        <div class="cuerpo"><ng-content /></div>
        <footer class="pie"><ng-content select="[pie]" /></footer>
      </div>
    </dialog>
  `,
  styleUrl: './dialogo.scss',
})
export class Dialogo {
  readonly titulo = input.required<string>();
  /** Versión ancha para comparaciones lado a lado. */
  readonly ancho = input(false);
  readonly cerrado = output<void>();

  private readonly caja = viewChild.required<ElementRef<HTMLDialogElement>>('caja');
  private static contador = 0;
  protected readonly idTitulo = `dialogo-titulo-${++Dialogo.contador}`;

  abrir(): void {
    const d = this.caja().nativeElement;
    if (!d.open) d.showModal();
  }

  cerrar(): void {
    const d = this.caja().nativeElement;
    if (d.open) d.close();
  }

  /** Un clic en el fondo oscuro (fuera del contenido) cierra el diálogo. */
  protected alFondo(evento: MouseEvent): void {
    if (evento.target === this.caja().nativeElement) this.cerrar();
  }
}
