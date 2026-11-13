import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, input, output, signal, viewChild } from '@angular/core';
import { Router } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';

import { Adaptacion, CambioAdaptacion } from '../../../../core/adaptacion/adaptacion.modelos';
import { AdaptacionServicio } from '../../../../core/adaptacion/adaptacion.servicio';
import { PestanaPendiente, abrirEnBlanco } from '../../../../core/postulaciones/pestana-externa';
import { Icono, NombreIcono } from '../../../../shared/icono/icono';
import { Dialogo } from '../../../hoja-de-vida/componentes/dialogo';
import { DocumentoHojaComponente } from '../../../hoja-de-vida/componentes/documento-hoja';

type EstadoDialogo = 'adaptando' | 'listo' | 'error' | 'limite';

const ICONOS: Record<CambioAdaptacion['tipo'], NombreIcono> = {
  redaccion: 'lapiz',
  movido: 'flecha',
  destacado: 'estrella',
};

/**
 * Adaptar la hoja de vida a una vacante: «Adaptando tu hoja…» → comparación base vs adaptada con los cambios
 * resaltados → Editar / Regenerar / Descartar / PDF / Word → Guardar o Guardar y postular.
 * La adaptación nunca agrega información: lo que la vacante pide y falta se muestra como aviso.
 */
@Component({
  selector: 'app-dialogo-adaptar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoPipe, Icono, Dialogo, DocumentoHojaComponente],
  templateUrl: './dialogo-adaptar.html',
  styleUrl: './dialogo-adaptar.scss',
})
export class DialogoAdaptar {
  private readonly servicio = inject(AdaptacionServicio);
  private readonly router = inject(Router);

  readonly vacanteId = input.required<string>();
  /** La adaptación ya guardada para esta vacante, si hay (modo «ver»). */
  readonly existente = input<Adaptacion | null>(null);
  readonly restantes = input<number | null>(null);

  /** La adaptación quedó guardada (el panel la usa para mostrar «Tienes tu hoja adaptada…»). */
  readonly guardada = output<Adaptacion>();
  /** Cuántas adaptaciones quedan hoy, para actualizar el panel. */
  readonly cupo = output<number>();
  /** «Guardar y postular»: la pestaña se abrió dentro del clic para que el navegador no la bloquee. */
  readonly postular = output<PestanaPendiente>();
  /** La adaptación se descartó sin guardar. */
  readonly descartada = output<void>();

  private readonly dialogo = viewChild.required<Dialogo>('dialogo');

  protected readonly estado = signal<EstadoDialogo>('adaptando');
  protected readonly adaptacion = signal<Adaptacion | null>(null);
  protected readonly yaGuardada = signal(false);
  protected readonly trabajando = signal(false);
  protected readonly falloAccion = signal(false);

  protected icono(c: CambioAdaptacion): NombreIcono {
    return ICONOS[c.tipo];
  }

  /** `nueva` genera una adaptación (gasta un cupo); `ver` muestra la ya guardada. */
  abrir(modo: 'nueva' | 'ver'): void {
    this.falloAccion.set(false);
    this.dialogo().abrir();
    if (modo === 'ver' && this.existente()) {
      this.adaptacion.set(this.existente());
      this.yaGuardada.set(true);
      this.estado.set('listo');
    } else {
      this.yaGuardada.set(false);
      this.generar();
    }
  }

  protected generar(): void {
    this.estado.set('adaptando');
    this.adaptacion.set(null);
    this.servicio.crear(this.vacanteId()).subscribe({
      next: (a) => {
        this.adaptacion.set(a);
        this.cupo.emit(a.restantesHoy);
        this.estado.set('listo');
      },
      // 429: se acabaron las adaptaciones de hoy.
      error: (e: HttpErrorResponse) => {
        if (e.status === 429) this.cupo.emit(0);
        this.estado.set(e.status === 429 ? 'limite' : 'error');
      },
    });
  }

  protected cerrar(): void {
    this.dialogo().cerrar();
  }

  protected urlDescarga(formato: 'pdf' | 'docx'): string {
    const a = this.adaptacion();
    return a ? this.servicio.urlDescarga(a.id, formato) : '#';
  }

  protected editar(): void {
    const a = this.adaptacion();
    if (!a) return;
    this.cerrar();
    void this.router.navigate(['/hoja-de-vida/editor'], { queryParams: { adaptacion: a.id } });
  }

  protected descartar(): void {
    const a = this.adaptacion();
    if (!a || this.trabajando()) return;
    this.trabajando.set(true);
    this.servicio.descartar(a.id).subscribe({
      next: () => {
        this.trabajando.set(false);
        this.adaptacion.set(null);
        this.descartada.emit();
        this.cerrar();
      },
      error: () => {
        this.trabajando.set(false);
        this.falloAccion.set(true);
      },
    });
  }

  protected guardar(): void {
    const a = this.adaptacion();
    if (!a || this.trabajando()) return;
    this.trabajando.set(true);
    this.falloAccion.set(false);
    this.servicio.aceptar(a.id, false).subscribe({
      next: () => {
        this.trabajando.set(false);
        this.guardada.emit(a);
        this.cerrar();
      },
      error: () => {
        this.trabajando.set(false);
        this.falloAccion.set(true);
      },
    });
  }

  protected guardarYPostular(): void {
    const a = this.adaptacion();
    if (!a || this.trabajando()) return;
    const pestana = abrirEnBlanco(); // dentro del clic, antes de esperar al servidor
    if (this.yaGuardada()) {
      this.cerrar();
      this.postular.emit(pestana);
      return;
    }
    this.trabajando.set(true);
    this.falloAccion.set(false);
    this.servicio.aceptar(a.id, true).subscribe({
      next: () => {
        this.trabajando.set(false);
        this.guardada.emit(a);
        this.cerrar();
        this.postular.emit(pestana);
      },
      error: () => {
        pestana.cancelar();
        this.trabajando.set(false);
        this.falloAccion.set(true);
      },
    });
  }
}
