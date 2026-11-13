/** Contratos de postulaciones. Solo tipos: aquí no hay datos. */

export type TipoVacantePostulada = 'practica' | 'profesional';
export type HojaUsada = 'base' | 'adaptada';
export type OrdenPostulaciones = 'recientes' | 'antiguas' | 'cargoAZ' | 'cargoZA';

/** Una vacante en la que la persona dio clic en «Postular». No hay estados: la postulación termina en la página de la empresa. */
export interface Postulacion {
  id: string;
  vacanteId: string;
  cargo: string;
  empresa: string;
  lugar: string;
  tipo: TipoVacantePostulada;
  /** ISO 8601 del último clic. */
  ultimoClicEn: string;
  clics: number;
  hojaUsada: HojaUsada;
}

export interface ListaPostulaciones {
  items: readonly Postulacion[];
  /** Clics en «Postular» de los últimos 90 días. */
  clicsUltimos90Dias: number;
}

/** Respuesta de registrar un clic: la página original de la vacante (externa). */
export interface ClicPostular {
  urlOriginal: string;
  postulacionId: string;
}
