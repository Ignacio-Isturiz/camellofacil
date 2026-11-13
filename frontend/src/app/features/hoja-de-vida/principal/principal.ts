import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';

import { API_HOJA_DE_VIDA } from '../../../core/hoja-de-vida/hoja-de-vida.api';
import { TipoObservacion } from '../../../core/hoja-de-vida/hoja-de-vida.modelos';
import { HojaDeVidaServicio } from '../../../core/hoja-de-vida/hoja-de-vida.servicio';
import { FechaCortaPipe } from '../../../shared/fecha-corta/fecha-corta.pipe';
import { Icono } from '../../../shared/icono/icono';
import { AvisoError } from '../componentes/aviso-error';
import { MiniaturaPlantilla } from '../componentes/miniatura-plantilla';
import { TarjetaPuntaje } from '../componentes/tarjeta-puntaje';

type Vista = 'analisis' | 'plantillas' | 'versiones';

/** Pantalla principal de «Hoja de vida» con la hoja base ya confirmada. */
@Component({
  selector: 'app-principal',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, TranslocoPipe, FechaCortaPipe, Icono, AvisoError, MiniaturaPlantilla, TarjetaPuntaje],
  templateUrl: './principal.html',
  styleUrl: './principal.scss',
})
export class Principal {
  protected readonly servicio = inject(HojaDeVidaServicio);

  protected readonly hoja = computed(() => this.servicio.resumen()?.hoja ?? null);
  protected readonly archivo = computed(() => this.hoja()?.archivo ?? null);

  /** En tableta y móvil las secciones van en pestañas; en escritorio se ven todas. */
  protected readonly vista = signal<Vista>('analisis');
  protected readonly vistas: readonly Vista[] = ['analisis', 'plantillas', 'versiones'];

  protected readonly tipos: readonly TipoObservacion[] = ['falta', 'mejorar', 'bien'];
  protected readonly tipoActivo = signal<TipoObservacion>('falta');
  protected readonly observaciones = computed(() =>
    (this.hoja()?.observaciones ?? []).filter((o) => o.tipo === this.tipoActivo()),
  );
  protected readonly conteos = computed(() => {
    const todas = this.hoja()?.observaciones ?? [];
    return {
      falta: todas.filter((o) => o.tipo === 'falta').length,
      mejorar: todas.filter((o) => o.tipo === 'mejorar').length,
      bien: todas.filter((o) => o.tipo === 'bien').length,
    } satisfies Record<TipoObservacion, number>;
  });

  protected readonly plantillasVisibles = computed(() => this.servicio.plantillas().slice(0, 4));
  protected readonly versionesVisibles = computed(() => this.servicio.versiones().slice(0, 3));
  protected readonly versionBase = computed(() => this.servicio.versiones().find((v) => v.tipo === 'base') ?? null);

  constructor() {
    this.servicio.cargarPlantillas();
    this.servicio.cargarVersiones();
  }

  protected urlDescarga(id: string, formato: 'pdf' | 'docx'): string {
    return API_HOJA_DE_VIDA.descargarVersion(id, formato);
  }

  protected cambiarOpcionAts(evento: Event): void {
    this.servicio.evaluarAts((evento.target as HTMLSelectElement).value);
  }

  protected reintentarPlantillas(): void {
    this.servicio.cargarPlantillas();
  }

  protected reintentarVersiones(): void {
    this.servicio.cargarVersiones();
  }
}
