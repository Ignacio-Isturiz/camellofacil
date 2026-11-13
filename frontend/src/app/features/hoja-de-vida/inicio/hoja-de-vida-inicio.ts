import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';

import { HojaDeVidaServicio } from '../../../core/hoja-de-vida/hoja-de-vida.servicio';
import { AvisoError } from '../componentes/aviso-error';
import { EsqueletoHoja } from '../componentes/esqueleto-hoja';
import { Principal } from '../principal/principal';
import { PrimeraVez } from '../primera-vez/primera-vez';

/**
 * /hoja-de-vida — decide qué mostrar según lo que responda el API:
 * sin hoja o sin confirmar → «Primera vez»; con hoja base confirmada → pantalla principal.
 */
@Component({
  selector: 'app-hoja-de-vida-inicio',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoPipe, AvisoError, EsqueletoHoja, Principal, PrimeraVez],
  templateUrl: './hoja-de-vida-inicio.html',
  styleUrl: './hoja-de-vida-inicio.scss',
})
export class HojaDeVidaInicio {
  protected readonly servicio = inject(HojaDeVidaServicio);

  protected readonly estado = this.servicio.estadoResumen;
  protected readonly resumen = this.servicio.resumen;
  protected readonly confirmada = computed(() => {
    const r = this.resumen();
    return r?.estado === 'confirmada' && r.hoja !== null;
  });

  constructor() {
    this.servicio.cargarResumen();
  }

  protected reintentar(): void {
    this.servicio.cargarResumen();
  }
}
