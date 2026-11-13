import { ChangeDetectionStrategy, Component, OnDestroy, effect, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';

import { HojaDeVidaSubidaServicio } from '../../../core/hoja-de-vida/hoja-de-vida-subida.servicio';
import { HojaDeVidaServicio } from '../../../core/hoja-de-vida/hoja-de-vida.servicio';
import { Icono } from '../../../shared/icono/icono';
import { MigasHoja } from '../componentes/migas-hoja';

/** /hoja-de-vida/subir — subir o reemplazar la hoja base. */
@Component({
  selector: 'app-subir',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, TranslocoPipe, Icono, MigasHoja],
  templateUrl: './subir.html',
  styleUrl: './subir.scss',
})
export class Subir implements OnDestroy {
  private readonly router = inject(Router);
  private readonly resumen = inject(HojaDeVidaServicio);
  protected readonly subida = inject(HojaDeVidaSubidaServicio);

  /** RF-93: sin esta autorización no se puede subir. Puede venir ya marcada desde «Primera vez». */
  protected readonly autorizado = signal(
    this.router.currentNavigation()?.extras.state?.['autorizado'] === true,
  );
  protected readonly arrastrando = signal(false);

  protected readonly erroresPasos = [1, 2, 3] as const;

  constructor() {
    // Empieza limpio cada vez que se entra a la pantalla.
    this.subida.reiniciar();
    // Cuando el análisis termina, se pasa a revisar lo que se leyó.
    effect(() => {
      if (this.subida.estado() === 'listo') {
        this.resumen.cargarResumen();
        void this.router.navigate(['/hoja-de-vida/revisar']);
      }
    });
  }

  ngOnDestroy(): void {
    this.subida.cancelar();
  }

  protected cambiarAutorizacion(evento: Event): void {
    this.autorizado.set((evento.target as HTMLInputElement).checked);
  }

  protected elegido(evento: Event): void {
    const entrada = evento.target as HTMLInputElement;
    const archivo = entrada.files?.[0];
    if (archivo) this.subida.subir(archivo, this.autorizado());
    entrada.value = ''; // permite volver a elegir el mismo archivo
  }

  protected sobre(evento: DragEvent, dentro: boolean): void {
    evento.preventDefault();
    if (this.autorizado()) this.arrastrando.set(dentro);
  }

  protected soltado(evento: DragEvent): void {
    evento.preventDefault();
    this.arrastrando.set(false);
    const archivo = evento.dataTransfer?.files?.[0];
    if (archivo) this.subida.subir(archivo, this.autorizado());
  }

  protected otroArchivo(): void {
    this.subida.reiniciar();
  }
}
