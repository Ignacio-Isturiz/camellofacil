import {
  ChangeDetectionStrategy,
  Component,
  Injector,
  afterNextRender,
  computed,
  effect,
  inject,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { EMPTY, catchError, filter, switchMap, tap } from 'rxjs';

import { Adaptacion } from '../../../../core/adaptacion/adaptacion.modelos';
import { AdaptacionServicio } from '../../../../core/adaptacion/adaptacion.servicio';
import { PestanaPendiente, abrirEnBlanco } from '../../../../core/postulaciones/pestana-externa';
import { HojaUsada } from '../../../../core/postulaciones/postulaciones.modelos';
import { PostulacionesServicio } from '../../../../core/postulaciones/postulaciones.servicio';
import { Icono } from '../../../../shared/icono/icono';
import { ToastServicio } from '../../../../shared/toast/toast.servicio';
import { DialogoAdaptar } from './dialogo-adaptar';
import { DialogoPostular } from './dialogo-postular';

/**
 * Panel lateral del detalle de la vacante: Postular, Adaptar hoja de vida (o «tienes tu hoja adaptada»)
 * y las acciones secundarias. En móvil, los dos botones principales van en una barra fija abajo.
 * No se pregunta si la persona terminó de postularse: el clic queda registrado en «Mis postulaciones».
 */
@Component({
  selector: 'app-panel-accion',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoPipe, Icono, DialogoAdaptar, DialogoPostular],
  templateUrl: './panel-accion.html',
  styleUrl: './panel-accion.scss',
})
export class PanelAccion {
  private readonly adaptaciones = inject(AdaptacionServicio);
  private readonly postulaciones = inject(PostulacionesServicio);
  private readonly toast = inject(ToastServicio);
  private readonly i18n = inject(TranslocoService);
  private readonly injector = inject(Injector);

  readonly vacanteId = input.required<string>();
  /** Verdadero cuando la vacante ya cargó; antes, los botones están apagados. */
  readonly lista = input(false);
  readonly guardada = input(false);
  readonly guardar = output<void>();

  /** El diálogo de adaptar se carga diferido (@defer): no existe hasta que se pide por primera vez. */
  private readonly dialogoAdaptar = viewChild<DialogoAdaptar>('dlgAdaptar');
  private readonly dialogoPostular = viewChild.required<DialogoPostular>('dlgPostular');
  /** Lo que se pidió abrir mientras el diálogo terminaba de cargar. */
  private readonly aperturaPendiente = signal<'nueva' | 'ver' | null>(null);
  protected readonly adaptarPedido = computed(() => this.aperturaPendiente() !== null || !!this.dialogoAdaptar());

  protected readonly cargando = signal(true);
  protected readonly fallo = signal(false);
  protected readonly adaptacion = signal<Adaptacion | null>(null);
  /** Adaptaciones que quedan hoy; null mientras no se sabe. */
  protected readonly restantes = signal<number | null>(null);

  constructor() {
    // Cuando la vacante carga, se consulta si ya hay una hoja adaptada y cuántas adaptaciones quedan hoy.
    // switchMap: si se cambia de vacante antes de que responda, la consulta anterior se cancela.
    toObservable(computed(() => (this.lista() ? this.vacanteId() : null)))
      .pipe(
        filter((id): id is string => id !== null),
        tap(() => {
          this.cargando.set(true);
          this.fallo.set(false);
        }),
        switchMap((id) =>
          this.adaptaciones.estado(id).pipe(
            catchError(() => {
              this.fallo.set(true);
              this.cargando.set(false);
              return EMPTY;
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((e) => {
        this.adaptacion.set(e.adaptacion);
        this.restantes.set(e.restantesHoy);
        this.cargando.set(false);
      });

    // En cuanto el diálogo diferido existe, se abre con lo que se había pedido. Se espera a que termine su
    // primer pintado: antes de eso todavía no tiene sus entradas (vacanteId, existente…).
    effect(() => {
      const modo = this.aperturaPendiente();
      const dialogo = this.dialogoAdaptar();
      if (!modo || !dialogo) return;
      this.aperturaPendiente.set(null);
      afterNextRender(() => dialogo.abrir(modo), { injector: this.injector });
    });
  }

  protected urlDescarga(formato: 'pdf' | 'docx'): string {
    const a = this.adaptacion();
    return a ? this.adaptaciones.urlDescarga(a.id, formato) : '#';
  }

  // ----- Adaptar -----
  protected adaptar(): void {
    this.abrirAdaptar('nueva');
  }

  protected verAdaptada(): void {
    this.abrirAdaptar('ver');
  }

  private abrirAdaptar(modo: 'nueva' | 'ver'): void {
    const dialogo = this.dialogoAdaptar();
    if (dialogo) dialogo.abrir(modo);
    else this.aperturaPendiente.set(modo);
  }

  protected alGuardarAdaptacion(a: Adaptacion): void {
    this.adaptacion.set(a);
    this.toast.mostrar(this.i18n.translate('vacantes.adaptar.guardadaAviso'));
  }

  protected alDescartar(): void {
    this.toast.mostrar(this.i18n.translate('vacantes.adaptar.descartadaAviso'));
  }

  // ----- Postular -----
  /**
   * Sin hoja adaptada no hay nada que elegir: se va directo a la oferta original. Con hoja adaptada se
   * pregunta con cuál ir. En ambos casos la pestaña se abre dentro del clic para que el navegador no la bloquee.
   */
  protected preguntarHoja(): void {
    if (this.adaptacion()) this.dialogoPostular().abrir();
    else this.postularCon('base');
  }

  protected postularCon(hoja: HojaUsada): void {
    this.postulaciones.postularEnVacante(this.vacanteId(), hoja, abrirEnBlanco());
  }

  protected postularConAdaptada(pestana: PestanaPendiente): void {
    this.postulaciones.postularEnVacante(this.vacanteId(), 'adaptada', pestana);
  }
}
