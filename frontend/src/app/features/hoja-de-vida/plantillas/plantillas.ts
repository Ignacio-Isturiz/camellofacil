import { ChangeDetectionStrategy, Component, computed, effect, inject, signal, viewChild } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';

import {
  CategoriaPlantilla,
  DocumentoHoja,
  EstadoCarga,
  IdiomaHoja,
  Plantilla,
} from '../../../core/hoja-de-vida/hoja-de-vida.modelos';
import { HojaDeVidaServicio } from '../../../core/hoja-de-vida/hoja-de-vida.servicio';
import { Icono } from '../../../shared/icono/icono';
import { AvisoError } from '../componentes/aviso-error';
import { Dialogo } from '../componentes/dialogo';
import { DocumentoHojaComponente } from '../componentes/documento-hoja';
import { MigasHoja } from '../componentes/migas-hoja';
import { MiniaturaPlantilla } from '../componentes/miniatura-plantilla';

type Filtro = 'todas' | CategoriaPlantilla;

/** /hoja-de-vida/plantillas — galería con filtros y vista previa con los datos de la persona. */
@Component({
  selector: 'app-plantillas',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, TranslocoPipe, Icono, AvisoError, Dialogo, DocumentoHojaComponente, MigasHoja, MiniaturaPlantilla],
  templateUrl: './plantillas.html',
  styleUrl: './plantillas.scss',
})
export class Plantillas {
  protected readonly servicio = inject(HojaDeVidaServicio);
  private readonly ruta = inject(ActivatedRoute);

  protected readonly filtros: readonly Filtro[] = ['todas', 'harvard', 'ats'];
  protected readonly filtro = signal<Filtro>('todas');
  protected readonly idiomas: readonly IdiomaHoja[] = ['es', 'en'];
  protected readonly idioma = signal<IdiomaHoja>('es');

  protected readonly visibles = computed(() =>
    this.servicio.plantillas().filter(
      (p) => (this.filtro() === 'todas' || p.categoria === this.filtro()) && p.idiomas.includes(this.idioma()),
    ),
  );

  // Vista previa
  private readonly dialogo = viewChild(Dialogo);
  protected readonly elegida = signal<Plantilla | null>(null);
  protected readonly documento = signal<DocumentoHoja | null>(null);
  protected readonly estadoPrevia = signal<EstadoCarga>('cargando');

  constructor() {
    this.servicio.cargarPlantillas();
    // Si se llega con ?vista=<id> (desde la pantalla principal), se abre esa vista previa.
    const pedida = this.ruta.snapshot.queryParamMap.get('vista');
    if (pedida) {
      effect(() => {
        const p = this.servicio.plantillas().find((x) => x.id === pedida);
        if (p && !this.elegida() && this.dialogo()) this.verPrevia(p);
      });
    }
  }

  protected reintentar(): void {
    this.servicio.cargarPlantillas();
  }

  protected verPrevia(p: Plantilla): void {
    this.elegida.set(p);
    this.cargarPrevia(p);
    this.dialogo()?.abrir();
  }

  protected cambiarIdioma(i: IdiomaHoja): void {
    this.idioma.set(i);
    const p = this.elegida();
    if (p && p.idiomas.includes(i)) this.cargarPrevia(p);
  }

  protected reintentarPrevia(): void {
    const p = this.elegida();
    if (p) this.cargarPrevia(p);
  }

  protected alCerrar(): void {
    this.elegida.set(null);
    this.documento.set(null);
  }

  private cargarPrevia(p: Plantilla): void {
    this.estadoPrevia.set('cargando');
    this.documento.set(null);
    this.servicio.vistaPreviaPlantilla(p.id, this.idioma()).subscribe({
      next: (d) => {
        // Se ignora una respuesta tardía de otra plantilla.
        if (this.elegida()?.id !== p.id) return;
        this.documento.set(d);
        this.estadoPrevia.set('listo');
      },
      error: () => this.estadoPrevia.set('error'),
    });
  }
}
