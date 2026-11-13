import { DocumentoHoja } from '../hoja-de-vida/hoja-de-vida.modelos';

/** Contratos de la adaptación. Regla: nunca agrega información que no esté en la hoja base. */

export type TipoCambioAdaptacion = 'redaccion' | 'movido' | 'destacado';

export interface CambioAdaptacion {
  id: string;
  tipo: TipoCambioAdaptacion;
  titulo: string;
  detalle: string;
}

export interface Adaptacion {
  id: string;
  vacanteId: string;
  /** La hoja base. */
  original: DocumentoHoja;
  /** La hoja adaptada a la vacante. */
  adaptada: DocumentoHoja;
  cambios: readonly CambioAdaptacion[];
  /** Lo que la vacante pide y la persona no tiene: se muestra como aviso, nunca se agrega. */
  avisos: readonly string[];
  /** Adaptaciones que le quedan hoy (máximo 3 por día). */
  restantesHoy: number;
}

export interface EstadoAdaptacion {
  /** La adaptación ya guardada para esta vacante, si existe. */
  adaptacion: Adaptacion | null;
  restantesHoy: number;
}
