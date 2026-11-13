import { Vacante } from '../../features/vacantes/vacante';

/** Contratos de Inicio. Solo tipos: aquí no hay datos. */

export interface Recomendada extends Vacante {
  /** Publicada desde la última visita. */
  nueva: boolean;
}

/** Lo que la persona dijo que busca; las etiquetas las devuelve el backend. */
export interface Busqueda {
  texto: string;
  etiquetas: readonly string[];
}

export interface ResumenInicio {
  vacantesNuevas: number;
  recomendadas: readonly Recomendada[];
  busqueda: Busqueda | null;
}

export interface CupoAdaptaciones {
  usadas: number;
  limite: number;
}

export type EstadoBloque = 'cargando' | 'listo' | 'error';
