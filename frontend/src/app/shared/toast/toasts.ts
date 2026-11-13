import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';

import { Icono } from '../icono/icono';
import { Toast, ToastServicio } from './toast.servicio';

/** Contenedor de toasts. Va una sola vez en el marco de la app; la región `aria-live` anuncia cada aviso nuevo. */
@Component({
  selector: 'app-toasts',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoPipe, Icono],
  template: `
    <div class="toasts" role="status" aria-live="polite" aria-atomic="false">
      @for (t of servicio.toasts(); track t.id) {
        <div class="toast">
          <p>{{ t.texto }}</p>
          @if (t.accion) {
            <button type="button" class="toast__accion" (click)="hacer(t)">{{ t.accion.etiqueta }}</button>
          }
          <button type="button" class="toast__cerrar" [attr.aria-label]="'hoja.dialogo.cerrar' | transloco" (click)="servicio.cerrar(t.id)">
            <app-icono nombre="cerrar" style="--tam: 16px" />
          </button>
        </div>
      }
    </div>
  `,
  styleUrl: './toasts.scss',
})
export class Toasts {
  protected readonly servicio = inject(ToastServicio);

  protected hacer(t: Toast): void {
    t.accion?.alHacer();
    this.servicio.cerrar(t.id);
  }
}
