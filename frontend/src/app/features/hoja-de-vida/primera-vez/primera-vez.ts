import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';

import { Icono, NombreIcono } from '../../../shared/icono/icono';
import { MiniaturaPlantilla } from '../componentes/miniatura-plantilla';

interface Bloqueo {
  clave: 'harvard' | 'ats' | 'mejorar' | 'vacantes';
}

/** «Primera vez»: aún no hay hoja base (o está subida sin confirmar). Dos caminos: subir o crear con plantilla. */
@Component({
  selector: 'app-primera-vez',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, TranslocoPipe, Icono, MiniaturaPlantilla],
  templateUrl: './primera-vez.html',
  styleUrl: './primera-vez.scss',
})
export class PrimeraVez {
  private readonly router = inject(Router);

  /** La hoja ya se subió pero falta confirmar los datos leídos. */
  readonly sinConfirmar = input(false);
  readonly pendientes = input<number | null>(null);

  /** RF-93: sin esta autorización la zona de carga queda desactivada. */
  protected readonly autorizado = signal(false);

  protected readonly pasos: readonly { clave: 'sube' | 'confirma' | 'recibe'; icono: NombreIcono }[] = [
    { clave: 'sube', icono: 'subir' },
    { clave: 'confirma', icono: 'check' },
    { clave: 'recibe', icono: 'estrella' },
  ];
  protected readonly bloqueos: readonly Bloqueo[] = [
    { clave: 'harvard' },
    { clave: 'ats' },
    { clave: 'mejorar' },
    { clave: 'vacantes' },
  ];

  protected cambiarAutorizacion(evento: Event): void {
    this.autorizado.set((evento.target as HTMLInputElement).checked);
  }

  /** La subida del archivo se hace en /hoja-de-vida/subir; la autorización viaja en el estado de la navegación. */
  protected elegirArchivo(): void {
    if (!this.autorizado()) return;
    void this.router.navigate(['/hoja-de-vida/subir'], { state: { autorizado: true } });
  }
}
