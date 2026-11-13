import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';

import { Icono } from '../../../../shared/icono/icono';

interface VacanteDemo {
  id: string;
  iniciales: string;
  cargo: string;
  empresa: string;
  lugar: string;
  hace: string;
  tono: 'rojo' | 'oro' | 'verde';
}

/**
 * Vacantes de muestra con datos ficticios (sin datos reales, Ley 1581).
 * Se reemplazan por la API de vacantes cuando exista ese módulo.
 */
const VACANTES_DEMO: readonly VacanteDemo[] = [
  {
    id: 'ga',
    iniciales: 'GA',
    cargo: 'Practicante de análisis de datos',
    empresa: 'Grupo Andino Analítica',
    lugar: 'Medellín · Híbrido',
    hace: 'Hace 2 h',
    tono: 'rojo',
  },
  {
    id: 'ld',
    iniciales: 'LD',
    cargo: 'Pasante de desarrollo de software',
    empresa: 'Laboratorio Digital Antioquia',
    lugar: 'Medellín · Remoto',
    hace: 'Hace 3 h',
    tono: 'oro',
  },
  {
    id: 'cv',
    iniciales: 'CV',
    cargo: 'Practicante Economía y Finanzas',
    empresa: 'Corporación Ventures',
    lugar: 'Envigado · Presencial',
    hace: 'Hace 5 h',
    tono: 'verde',
  },
];

/** Muestra de tres vacantes recientes y el botón para ver todas. */
@Component({
  selector: 'app-vacantes-destacadas',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, TranslocoPipe, Icono],
  templateUrl: './vacantes-destacadas.html',
  styleUrl: './vacantes-destacadas.scss',
})
export class VacantesDestacadas {
  protected readonly vacantes = VACANTES_DEMO;
}
