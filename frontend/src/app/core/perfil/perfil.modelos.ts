/** Contrato de /api/v1/perfil (módulo perfil del backend). */

export type TipoPersona = 'ESTUDIANTE' | 'EGRESADO';
export type ModalidadPerfil = 'PRESENCIAL' | 'REMOTO' | 'HIBRIDO';
export type Disponibilidad = 'TIEMPO_COMPLETO' | 'MEDIO_TIEMPO' | 'FLEXIBLE';

export const MODALIDADES_PERFIL: readonly ModalidadPerfil[] = ['PRESENCIAL', 'REMOTO', 'HIBRIDO'];
export const DISPONIBILIDADES: readonly Disponibilidad[] = ['TIEMPO_COMPLETO', 'MEDIO_TIEMPO', 'FLEXIBLE'];
export const SEMESTRES: readonly number[] = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];

export interface Carrera {
  codigo: string;
  nombre: string;
}

export interface Consentimiento {
  version: string;
  /** ISO 8601. */
  otorgadoEn: string;
}

export interface DatosPerfil {
  tipo: TipoPersona;
  carrera: Carrera;
  semestre: number | null;
  modalidades: ModalidadPerfil[];
  disponibilidad: Disponibilidad | null;
}

/** Con `completo: false` se muestra el modal obligatorio del primer ingreso. */
export interface EstadoPerfil {
  completo: boolean;
  nombre: string;
  correo: string;
  versionTerminosVigente: string;
  datos: DatosPerfil | null;
  terminos: Consentimiento | null;
  analisisIa: Consentimiento | null;
}

export interface CompletarPerfil {
  tipo: TipoPersona;
  carrera: string;
  semestre: number | null;
  versionTerminos: string;
  aceptaTerminos: boolean;
  autorizaAnalisisIa: boolean;
}

export interface ActualizarPerfil {
  tipo: TipoPersona;
  carrera: string;
  semestre: number | null;
  modalidades: ModalidadPerfil[];
  disponibilidad: Disponibilidad | null;
}
