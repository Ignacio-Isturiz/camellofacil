import { ChangeDetectionStrategy, Component, computed, inject, signal, viewChild } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { Observable } from 'rxjs';

import { API_HOJA_DE_VIDA } from '../../../core/hoja-de-vida/hoja-de-vida.api';
import { DocumentoHoja, EstadoCarga, Version } from '../../../core/hoja-de-vida/hoja-de-vida.modelos';
import { HojaDeVidaServicio } from '../../../core/hoja-de-vida/hoja-de-vida.servicio';
import { FechaCortaPipe } from '../../../shared/fecha-corta/fecha-corta.pipe';
import { Icono } from '../../../shared/icono/icono';
import { AvisoError } from '../componentes/aviso-error';
import { Dialogo } from '../componentes/dialogo';
import { DocumentoHojaComponente } from '../componentes/documento-hoja';
import { MigasHoja } from '../componentes/migas-hoja';

type Filtro = 'todas' | 'base' | 'adaptadas';
type Accion = 'menu' | 'renombrar' | 'eliminar';

/** /hoja-de-vida/versiones — lista y gestión de las versiones de la hoja de vida. */
@Component({
  selector: 'app-versiones',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink, TranslocoPipe, FechaCortaPipe, Icono, AvisoError, Dialogo, DocumentoHojaComponente, MigasHoja],
  templateUrl: './versiones.html',
  styleUrl: './versiones.scss',
})
export class Versiones {
  protected readonly servicio = inject(HojaDeVidaServicio);

  protected readonly filtros: readonly Filtro[] = ['todas', 'base', 'adaptadas'];
  protected readonly filtro = signal<Filtro>('todas');

  protected readonly visibles = computed(() => {
    const f = this.filtro();
    return this.servicio.versiones().filter((v) =>
      f === 'todas' ? true : f === 'adaptadas' ? v.tipo === 'adaptada' : v.tipo !== 'adaptada',
    );
  });
  protected readonly conteos = computed(() => {
    const todas = this.servicio.versiones();
    return {
      todas: todas.length,
      base: todas.filter((v) => v.tipo !== 'adaptada').length,
      adaptadas: todas.filter((v) => v.tipo === 'adaptada').length,
    } satisfies Record<Filtro, number>;
  });

  // Ver una versión
  private readonly dialogoVer = viewChild<Dialogo>('dialogoVer');
  protected readonly documento = signal<DocumentoHoja | null>(null);
  protected readonly estadoDocumento = signal<EstadoCarga>('cargando');

  // Acciones sobre una versión
  private readonly dialogoAcciones = viewChild<Dialogo>('dialogoAcciones');
  protected readonly seleccionada = signal<Version | null>(null);
  protected readonly accion = signal<Accion>('menu');
  protected readonly trabajando = signal(false);
  protected readonly falloAccion = signal(false);
  protected readonly nombre = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.maxLength(120)],
  });

  constructor() {
    this.servicio.cargarVersiones();
  }

  protected reintentar(): void {
    this.servicio.cargarVersiones();
  }

  protected urlDescarga(id: string, formato: 'pdf' | 'docx'): string {
    return API_HOJA_DE_VIDA.descargarVersion(id, formato);
  }

  // ----- Ver -----
  protected ver(v: Version): void {
    this.seleccionada.set(v);
    this.documento.set(null);
    this.estadoDocumento.set('cargando');
    this.dialogoVer()?.abrir();
    this.servicio.documentoVersion(v.id).subscribe({
      next: (d) => {
        if (this.seleccionada()?.id !== v.id) return;
        this.documento.set(d);
        this.estadoDocumento.set('listo');
      },
      error: () => this.estadoDocumento.set('error'),
    });
  }

  // ----- Acciones -----
  protected abrirAcciones(v: Version): void {
    this.seleccionada.set(v);
    this.accion.set('menu');
    this.falloAccion.set(false);
    this.dialogoAcciones()?.abrir();
  }

  protected irARenombrar(): void {
    this.nombre.setValue(this.seleccionada()?.nombre ?? '');
    this.falloAccion.set(false);
    this.accion.set('renombrar');
  }

  protected irAEliminar(): void {
    this.falloAccion.set(false);
    this.accion.set('eliminar');
  }

  protected volverAlMenu(): void {
    this.falloAccion.set(false);
    this.accion.set('menu');
  }

  protected renombrar(): void {
    const v = this.seleccionada();
    if (!v || this.nombre.invalid) return;
    this.ejecutar(this.servicio.renombrarVersion(v.id, this.nombre.value.trim()));
  }

  protected duplicar(): void {
    const v = this.seleccionada();
    if (v) this.ejecutar(this.servicio.duplicarVersion(v.id));
  }

  protected usarComoBase(): void {
    const v = this.seleccionada();
    if (!v) return;
    this.ejecutar(this.servicio.usarVersionComoBase(v.id), () => this.servicio.cargarResumen());
  }

  protected eliminar(): void {
    const v = this.seleccionada();
    if (v?.eliminable) this.ejecutar(this.servicio.eliminarVersion(v.id));
  }

  /** Corre una acción del servidor; si sale bien cierra el diálogo y recarga la lista. */
  private ejecutar(llamada: Observable<unknown>, despues?: () => void): void {
    this.trabajando.set(true);
    this.falloAccion.set(false);
    llamada.subscribe({
      next: () => {
        this.trabajando.set(false);
        this.dialogoAcciones()?.cerrar();
        this.servicio.cargarVersiones();
        despues?.();
      },
      error: () => {
        this.trabajando.set(false);
        this.falloAccion.set(true);
      },
    });
  }
}
