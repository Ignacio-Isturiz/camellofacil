import { ChangeDetectionStrategy, Component, computed, inject, signal, viewChild } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';

import { InicioServicio } from '../../core/inicio/inicio.servicio';
import { Sesion } from '../../core/sesion/sesion';
import { Icono } from '../../shared/icono/icono';
import { ToastServicio } from '../../shared/toast/toast.servicio';
import { AvisoError } from '../hoja-de-vida/componentes/aviso-error';
import { Dialogo } from '../hoja-de-vida/componentes/dialogo';
import { VacantesServicio } from '../vacantes/vacantes-servicio';
import { TarjetaRecomendada } from './tarjeta-recomendada';

const MAX_ETIQUETAS = 8;

/** Inicio de quien ya inició sesión: saludo, recomendadas de hoy y los paneles laterales. */
@Component({
  selector: 'app-inicio',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, TranslocoPipe, Icono, AvisoError, Dialogo, TarjetaRecomendada],
  templateUrl: './inicio.html',
  styleUrl: './inicio.scss',
})
export class Inicio {
  protected readonly servicio = inject(InicioServicio);
  private readonly vacantes = inject(VacantesServicio);
  private readonly sesion = inject(Sesion);
  private readonly toast = inject(ToastServicio);
  private readonly i18n = inject(TranslocoService);

  private readonly idioma = toSignal(this.i18n.langChanges$, { initialValue: this.i18n.getActiveLang() });
  protected readonly guardadas = this.vacantes.guardadas;
  private readonly dialogo = viewChild.required<Dialogo>('dlgBusqueda');

  protected readonly nombre = computed(() => this.sesion.usuario()?.nombre.split(' ')[0] ?? '');

  /** «Martes 11 de noviembre», en el idioma activo. */
  protected readonly hoy = computed(() => {
    const lang = this.idioma() === 'es' ? 'es-CO' : 'en-US';
    const texto = new Intl.DateTimeFormat(lang, { weekday: 'long', day: 'numeric', month: 'long' }).format(new Date());
    return texto.charAt(0).toUpperCase() + texto.slice(1);
  });

  /** Una barra por adaptación del día: llena = disponible, vacía = ya usada. */
  protected readonly barras = computed(() => {
    const c = this.servicio.cupo();
    if (!c) return [];
    const restantes = Math.max(0, c.limite - c.usadas);
    return Array.from({ length: c.limite }, (_, i) => i < restantes);
  });
  protected readonly restantes = computed(() => {
    const c = this.servicio.cupo();
    return c ? Math.max(0, c.limite - c.usadas) : 0;
  });

  // ----- Editar «¿Qué estás buscando?» -----
  protected readonly textoBusqueda = signal('');
  protected readonly etiquetasBusqueda = signal('');
  protected readonly guardando = signal(false);
  protected readonly falloGuardar = signal(false);

  constructor() {
    this.servicio.cargarTodo();
  }

  protected alternarGuardada(id: string): void {
    this.vacantes.alternarGuardada(id);
  }

  protected editarBusqueda(): void {
    const b = this.servicio.resumen()?.busqueda;
    this.textoBusqueda.set(b?.texto ?? '');
    this.etiquetasBusqueda.set(b?.etiquetas.join(', ') ?? '');
    this.falloGuardar.set(false);
    this.dialogo().abrir();
  }

  protected cancelar(): void {
    this.dialogo().cerrar();
  }

  protected guardarBusqueda(): void {
    const texto = this.textoBusqueda().trim();
    if (!texto || this.guardando()) return;
    const etiquetas = this.etiquetasBusqueda()
      .split(',')
      .map((e) => e.trim())
      .filter((e, i, todas) => e && todas.indexOf(e) === i)
      .slice(0, MAX_ETIQUETAS);
    this.guardando.set(true);
    this.falloGuardar.set(false);
    this.servicio.guardarBusqueda({ texto, etiquetas }).subscribe({
      next: (guardada) => {
        this.servicio.aplicarBusqueda(guardada);
        this.guardando.set(false);
        this.dialogo().cerrar();
        this.toast.mostrar(this.i18n.translate('inicio.busqueda.guardada'));
      },
      error: () => {
        this.guardando.set(false);
        this.falloGuardar.set(true);
      },
    });
  }
}
