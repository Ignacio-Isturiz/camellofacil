export type TipoVacante = 'practica' | 'profesional';
export type Modalidad = 'presencial' | 'hibrido' | 'remoto';

export const MODALIDADES: readonly Modalidad[] = ['presencial', 'hibrido', 'remoto'];
/** Claves de carrera: las mismas de `landing.carreras.nombres.*` y del backend. */
export const CARRERAS = ['derecho', 'contaduria', 'administracion', 'economia', 'informatica', 'industrial', 'datos', 'sociales'] as const;

export interface Razon {
  /** `ok`: coincide con tu hoja; `aviso`: falta algo (no se inventa, se sugiere). */
  tipo: 'ok' | 'aviso';
  texto: string;
}

export interface Vacante {
  id: string;
  cargo: string;
  empresa: string;
  tipo: TipoVacante;
  /** Porcentaje de coincidencia con la hoja de vida (0–100). Null hasta que exista el módulo de recomendaciones. */
  coincidencia: number | null;
  ciudad: string | null;
  modalidad: Modalidad;
  /** Días desde la publicación (0 = hoy). */
  dias: number;
  /** Portal donde se publicó (p. ej. «El Empleo»). */
  fuente: string | null;
  carreras: readonly string[];
  razones: readonly Razon[];
}

export interface VacanteDetalle extends Vacante {
  descripcion: string | null;
  responsabilidades: readonly string[];
  requisitos: readonly string[];
}

// ----- Formato del API (/api/v1/vacantes) -----

export interface VacanteApi {
  id: string;
  cargo: string;
  empresa: string;
  tipo: TipoVacante;
  ciudad: string | null;
  modalidad: Modalidad;
  /** ISO 8601. */
  publicadaEn: string;
  fuente: string | null;
  carreras: string[];
}

export interface ListaVacantesApi {
  vacantes: VacanteApi[];
  guardadas: string[];
}

export interface DetalleVacanteApi {
  vacante: VacanteApi;
  descripcion: string | null;
  responsabilidades: string[];
  requisitos: string[];
}

const DIA_MS = 24 * 60 * 60 * 1000;

export function desdeApi(v: VacanteApi, ahora = Date.now()): Vacante {
  return {
    id: v.id,
    cargo: v.cargo,
    empresa: v.empresa,
    tipo: v.tipo,
    coincidencia: null,
    ciudad: v.ciudad,
    modalidad: v.modalidad,
    dias: Math.max(0, Math.floor((ahora - Date.parse(v.publicadaEn)) / DIA_MS)),
    fuente: v.fuente,
    carreras: v.carreras,
    razones: [],
  };
}
