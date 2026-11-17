import { Vacante, VacanteApi } from '../../features/vacantes/vacante';

/** Contratos de Inicio. Solo tipos: aquí no hay datos. */

export interface Recomendada extends Vacante {
  /** Publicada hace menos de 2 días. */
  nueva: boolean;
}

/** Lo que la persona dijo que busca; las etiquetas las devuelve el backend. */
export interface Busqueda {
  texto: string;
  etiquetas: readonly string[];
}

/** Inicio con una página de recomendadas (de a 4; `pagina` empieza en 0). */
export interface ResumenInicio {
  vacantesNuevas: number;
  recomendadas: readonly Recomendada[];
  pagina: number;
  totalPaginas: number;
  total: number;
  busqueda: Busqueda | null;
}

export interface ResumenInicioApi {
  vacantesNuevas: number;
  recomendadas: readonly { vacante: VacanteApi; nueva: boolean }[];
  pagina: number;
  totalPaginas: number;
  total: number;
  busqueda: Busqueda | null;
}

export interface CupoAdaptaciones {
  usadas: number;
  limite: number;
}

export type EstadoBloque = 'cargando' | 'listo' | 'error';
