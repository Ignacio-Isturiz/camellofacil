export type TipoVacante = 'practica' | 'profesional';
export type Modalidad = 'presencial' | 'hibrido' | 'remoto';

export const MODALIDADES: readonly Modalidad[] = ['presencial', 'hibrido', 'remoto'];
/** Claves de carrera: las mismas de `landing.carreras.nombres.*`. */
export const CARRERAS = ['derecho', 'contaduria', 'administracion', 'economia', 'informatica', 'industrial', 'datos'] as const;

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
  /** Porcentaje de coincidencia con la hoja de vida (0–100). */
  coincidencia: number;
  ciudad: string;
  modalidad: Modalidad;
  /** Días desde la publicación (0 = hoy). */
  dias: number;
  fuente: string;
  carreras: readonly string[];
  razones: readonly Razon[];
  descripcion: string;
  responsabilidades: readonly string[];
  requisitos: readonly string[];
}
