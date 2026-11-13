import { ChangeDetectionStrategy, Component, effect, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { debounceTime, distinctUntilChanged } from 'rxjs';

import { abrirEnBlanco } from '../../core/postulaciones/pestana-externa';
import { OrdenPostulaciones, Postulacion } from '../../core/postulaciones/postulaciones.modelos';
import { PostulacionesServicio } from '../../core/postulaciones/postulaciones.servicio';
import { Icono } from '../../shared/icono/icono';
import { ToastServicio } from '../../shared/toast/toast.servicio';
import { AvisoError } from '../hoja-de-vida/componentes/aviso-error';
import { FilaPostulacion } from './fila-postulacion';

/** /postulaciones — historial de las vacantes en las que la persona dio clic en «Postular». */
@Component({
  selector: 'app-postulaciones',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, TranslocoPipe, Icono, AvisoError, FilaPostulacion],
  templateUrl: './postulaciones.html',
  styleUrl: './postulaciones.scss',
})
export class Postulaciones {
  protected readonly servicio = inject(PostulacionesServicio);
  private readonly toast = inject(ToastServicio);
  private readonly i18n = inject(TranslocoService);

  protected readonly ordenes: readonly OrdenPostulaciones[] = ['recientes', 'antiguas', 'cargoAZ', 'cargoZA'];
  protected readonly consulta = signal('');
  protected readonly orden = signal<OrdenPostulaciones>('recientes');
  protected readonly esqueletos = [1, 2, 3, 4];

  /** La búsqueda espera un instante tras la última letra para no pedir al servidor en cada tecla. */
  private readonly textoBuscado = toSignal(
    toObservable(this.consulta).pipe(debounceTime(300), distinctUntilChanged()),
    { initialValue: '' },
  );

  constructor() {
    effect(() => {
      const texto = this.textoBuscado();
      const orden = this.orden();
      this.servicio.cargar(texto, orden);
    });
  }

  protected recargar(): void {
    this.servicio.cargar(this.textoBuscado(), this.orden());
  }

  protected cambiarOrden(evento: Event): void {
    this.orden.set((evento.target as HTMLSelectElement).value as OrdenPostulaciones);
  }

  /** Abre la página original en otra pestaña y registra un clic nuevo. */
  protected ver(p: Postulacion): void {
    const pestana = abrirEnBlanco(); // dentro del clic, para que el navegador no la bloquee
    this.servicio.clicDesdeHistorial(p.id).subscribe({
      next: (c) => {
        if (!pestana.completar(c.urlOriginal)) this.toast.mostrar(this.i18n.translate('postulaciones.errorAbrir'));
        this.recargar();
      },
      error: () => {
        pestana.cancelar();
        this.toast.mostrar(this.i18n.translate('postulaciones.errorAbrir'));
      },
    });
  }

  /** Quita la fila del historial y ofrece «Deshacer» durante unos segundos. */
  protected quitar(p: Postulacion): void {
    this.servicio.quitar(p.id).subscribe({
      next: () => {
        this.servicio.items.update((l) => l.filter((x) => x.id !== p.id));
        this.toast.mostrar(this.i18n.translate('postulaciones.quitada'), {
          etiqueta: this.i18n.translate('postulaciones.deshacer'),
          alHacer: () => this.deshacer(p),
        });
      },
      error: () => this.toast.mostrar(this.i18n.translate('postulaciones.errorQuitar')),
    });
  }

  private deshacer(p: Postulacion): void {
    this.servicio.restaurar(p.id).subscribe({
      next: () => {
        this.toast.mostrar(this.i18n.translate('postulaciones.restaurada'));
        this.recargar();
      },
      error: () => this.toast.mostrar(this.i18n.translate('postulaciones.errorRestaurar')),
    });
  }
}
