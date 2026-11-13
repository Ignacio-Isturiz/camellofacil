import { Injectable, signal } from '@angular/core';

export interface Toast {
  id: number;
  texto: string;
  accion: { etiqueta: string; alHacer: () => void } | null;
}

/** Avisos breves (toasts). Se anuncian a los lectores de pantalla desde `app-toasts` (aria-live). */
@Injectable({ providedIn: 'root' })
export class ToastServicio {
  private contador = 0;
  readonly toasts = signal<readonly Toast[]>([]);

  /** `duracionMs`: más largo cuando hay una acción (p. ej. «Deshacer») para dar tiempo a pulsarla. */
  mostrar(texto: string, accion: Toast['accion'] = null, duracionMs = accion ? 8000 : 5000): void {
    const id = ++this.contador;
    this.toasts.update((l) => [...l, { id, texto, accion }]);
    setTimeout(() => this.cerrar(id), duracionMs);
  }

  cerrar(id: number): void {
    this.toasts.update((l) => l.filter((t) => t.id !== id));
  }
}
