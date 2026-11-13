import { UpperCasePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';

import {
  CambioPropuesto,
  EstadoCarga,
  EstiloPlantilla,
  FormatoDestino,
  IdiomaHoja,
  PropuestaReorganizacion,
} from '../../../core/hoja-de-vida/hoja-de-vida.modelos';
import { HojaDeVidaServicio } from '../../../core/hoja-de-vida/hoja-de-vida.servicio';
import { Icono, NombreIcono } from '../../../shared/icono/icono';
import { AvisoError } from '../componentes/aviso-error';
import { DocumentoHojaComponente } from '../componentes/documento-hoja';
import { MigasHoja } from '../componentes/migas-hoja';

type Vista = 'lado' | 'cambios';

const ICONOS: Record<CambioPropuesto['tipo'], NombreIcono> = {
  movido: 'flecha',
  redaccion: 'lapiz',
  separado: 'lista',
};

/** /hoja-de-vida/reorganizar — compara el original con la propuesta y deja aplicar o revertir cada cambio. */
@Component({
  selector: 'app-reorganizar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoPipe, UpperCasePipe, Icono, AvisoError, DocumentoHojaComponente, MigasHoja],
  templateUrl: './reorganizar.html',
  styleUrl: './reorganizar.scss',
})
export class Reorganizar {
  private readonly servicio = inject(HojaDeVidaServicio);
  private readonly router = inject(Router);

  protected readonly formatos: readonly FormatoDestino[] = ['harvard', 'ats'];
  protected readonly idiomas: readonly IdiomaHoja[] = ['es', 'en'];
  protected readonly vistas: readonly Vista[] = ['lado', 'cambios'];

  protected readonly formato = signal<FormatoDestino>('harvard');
  protected readonly idioma = signal<IdiomaHoja>('es');
  protected readonly vista = signal<Vista>('lado');

  protected readonly propuesta = signal<PropuestaReorganizacion | null>(null);
  protected readonly estado = signal<EstadoCarga>('cargando');
  /** Id del cambio que se está aplicando o revirtiendo (para apagar su botón). */
  protected readonly cambiando = signal<string | null>(null);
  protected readonly guardando = signal(false);
  protected readonly falloGuardar = signal(false);

  protected readonly estiloDocumento = computed<EstiloPlantilla>(() =>
    this.formato() === 'harvard' ? 'harvard' : 'ats-clasico',
  );

  constructor() {
    this.proponer();
  }

  protected icono(c: CambioPropuesto): NombreIcono {
    return ICONOS[c.tipo];
  }

  protected elegirFormato(evento: Event): void {
    this.formato.set((evento.target as HTMLSelectElement).value as FormatoDestino);
    this.proponer();
  }

  protected elegirIdioma(i: IdiomaHoja): void {
    if (this.idioma() === i) return;
    this.idioma.set(i);
    this.proponer();
  }

  /** Pide (o vuelve a pedir) la propuesta. El backend solo reordena y redacta mejor lo que ya está. */
  protected proponer(): void {
    this.estado.set('cargando');
    this.falloGuardar.set(false);
    this.servicio.proponerReorganizacion({ formato: this.formato(), idioma: this.idioma() }).subscribe({
      next: (p) => {
        this.propuesta.set(p);
        this.estado.set('listo');
      },
      error: () => this.estado.set('error'),
    });
  }

  protected alternar(c: CambioPropuesto): void {
    const p = this.propuesta();
    if (!p || this.cambiando()) return;
    this.cambiando.set(c.id);
    this.servicio.cambiarCambio(p.id, c, !c.aplicado).subscribe({
      next: (nueva) => {
        this.propuesta.set(nueva);
        this.cambiando.set(null);
      },
      error: () => this.cambiando.set(null),
    });
  }

  protected descartar(): void {
    const p = this.propuesta();
    const salir = () => void this.router.navigate(['/hoja-de-vida']);
    if (!p) return salir();
    this.servicio.descartarReorganizacion(p.id).subscribe({ next: salir, error: salir });
  }

  /** Guarda la propuesta como versión; con `editar` abre después el editor sobre esa versión. */
  protected aceptar(editar: boolean): void {
    const p = this.propuesta();
    if (!p || this.guardando()) return;
    this.guardando.set(true);
    this.falloGuardar.set(false);
    this.servicio.aceptarReorganizacion(p.id).subscribe({
      next: (version) => {
        this.servicio.cargarVersiones();
        if (editar) void this.router.navigate(['/hoja-de-vida/editor'], { queryParams: { version: version.id } });
        else void this.router.navigate(['/hoja-de-vida/versiones']);
      },
      error: () => {
        this.guardando.set(false);
        this.falloGuardar.set(true);
      },
    });
  }
}
