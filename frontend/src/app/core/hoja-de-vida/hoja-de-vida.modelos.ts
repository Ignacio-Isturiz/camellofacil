/**
 * Contratos del módulo «Hoja de vida» tal como la interfaz espera recibirlos del API.
 * Solo tipos: aquí no hay datos. TODO: confirmar con el backend / reemplazar por los modelos generados desde OpenAPI.
 */

export type IdiomaHoja = 'es' | 'en';

/** Punto del recorrido en el que está la persona. */
export type EstadoHoja = 'sin_hoja' | 'sin_confirmar' | 'confirmada';

export interface ArchivoHoja {
  nombre: string;
  /** Fecha ISO 8601 de la última actualización. */
  actualizadaEn: string;
  /** Idioma detectado en el archivo. */
  idioma: IdiomaHoja;
  paginas: number;
}

export type NivelCumplimiento = 'bajo' | 'medio' | 'alto';

export interface Puntaje {
  /** De 0 a 100. */
  valor: number;
  nivel: NivelCumplimiento;
  /** Explicación corta del puntaje, redactada por el backend. */
  detalle: string;
}

export interface OpcionAts {
  id: string;
  nombre: string;
}

export interface PuntajeAts extends Puntaje {
  /** Id de la opción ATS con la que se evaluó. */
  opcionId: string;
}

export type TipoObservacion = 'falta' | 'mejorar' | 'bien';

export interface Observacion {
  id: string;
  tipo: TipoObservacion;
  titulo: string;
  detalle: string;
}

export interface HojaBase {
  archivo: ArchivoHoja;
  harvard: Puntaje;
  ats: PuntajeAts;
  opcionesAts: readonly OpcionAts[];
  observaciones: readonly Observacion[];
}

export interface ResumenHojaDeVida {
  estado: EstadoHoja;
  /** Presente cuando `estado` no es `sin_hoja`; los puntajes solo si está `confirmada`. */
  hoja: HojaBase | null;
  /** Datos extraídos que aún hay que revisar (solo con `sin_confirmar`). */
  pendientesRevision: number | null;
}

export type CategoriaPlantilla = 'harvard' | 'ats';
/** Define cómo se dibuja la miniatura; los textos vienen del API. */
export type EstiloPlantilla = 'harvard' | 'ats-clasico' | 'ats-moderno' | 'ats-compacto';

export interface Plantilla {
  id: string;
  nombre: string;
  descripcion: string;
  categoria: CategoriaPlantilla;
  estilo: EstiloPlantilla;
  idiomas: readonly IdiomaHoja[];
  /** Texto opcional destacado (p. ej. «Recomendada para prácticas»). */
  etiqueta: string | null;
}

export type TipoVersion = 'base' | 'reorganizada' | 'adaptada';

export interface Version {
  id: string;
  nombre: string;
  tipo: TipoVersion;
  creadaEn: string;
  idioma: IdiomaHoja;
  puntajeHarvard: number | null;
  puntajeAts: number | null;
  /** Detalle opcional (empresa de la vacante, uso en postulaciones…), redactado por el backend. */
  nota: string | null;
  /** Una versión usada en una postulación (o la base) no se puede eliminar. */
  eliminable: boolean;
  motivoNoEliminable: string | null;
}

/** Estado de cada petición que alimenta una pantalla. */
export type EstadoCarga = 'cargando' | 'listo' | 'error';

// ---------- Subida y análisis ----------

export type EtapaAnalisis = 'analizando' | 'listo' | 'ilegible';

export interface AvanceAnalisis {
  estado: EtapaAnalisis;
}

/** Por qué falló una subida (los cuatro primeros se detectan en el cliente o los devuelve el servidor). */
export type ErrorSubida = 'formato' | 'peso' | 'ilegible' | 'conexion' | 'servidor';

// ---------- Datos extraídos del archivo ----------

export type EstadoCampo = 'leido' | 'revisa' | 'no_encontrado';

export interface CampoExtraido {
  valor: string;
  estado: EstadoCampo;
  /** Ayuda corta redactada por el backend (p. ej. «Parece incompleto…»). */
  ayuda: string | null;
}

export interface ContactoExtraido {
  nombre: CampoExtraido;
  /** El correo viene de la cuenta y no se edita. */
  correo: CampoExtraido;
  telefono: CampoExtraido;
  ciudad: CampoExtraido;
}

export interface ExperienciaExtraida {
  id: string;
  cargo: CampoExtraido;
  organizacion: CampoExtraido;
  /** Formato `AAAA-MM` (o vacío si no se encontró). */
  inicio: CampoExtraido;
  fin: CampoExtraido;
  descripcion: CampoExtraido;
}

export interface EducacionExtraida {
  id: string;
  titulo: CampoExtraido;
  institucion: CampoExtraido;
  inicio: CampoExtraido;
  fin: CampoExtraido;
}

export interface DatosExtraidos {
  archivo: { nombre: string; idioma: IdiomaHoja };
  contacto: ContactoExtraido;
  perfil: CampoExtraido;
  experiencia: readonly ExperienciaExtraida[];
  educacion: readonly EducacionExtraida[];
  habilidades: CampoExtraido;
}

/** Lo que la persona dejó después de revisar: se envía al servidor (solo texto, sin estados). */
export interface ValoresHoja {
  nombre: string;
  telefono: string;
  ciudad: string;
  perfil: string;
  habilidades: string;
  experiencia: readonly { id: string; cargo: string; organizacion: string; inicio: string; fin: string; descripcion: string }[];
  educacion: readonly { id: string; titulo: string; institucion: string; inicio: string; fin: string }[];
}

// ---------- Documento estructurado (vista previa, versiones, reorganización) ----------

export interface SeccionDocumento {
  id: string;
  titulo: string;
  bloques: readonly string[];
}

export interface DocumentoHoja {
  nombre: string;
  contacto: readonly string[];
  secciones: readonly SeccionDocumento[];
}

// ---------- Reorganizar ----------

export type FormatoDestino = 'harvard' | 'ats';
export type TipoCambio = 'movido' | 'redaccion' | 'separado';

export interface CambioPropuesto {
  id: string;
  tipo: TipoCambio;
  titulo: string;
  detalle: string;
  aplicado: boolean;
  /** Sección del documento a la que afecta (para resaltarla). */
  seccionId: string | null;
}

export interface SolicitudReorganizacion {
  formato: FormatoDestino;
  idioma: IdiomaHoja;
}

export interface PropuestaReorganizacion {
  id: string;
  formato: FormatoDestino;
  idioma: IdiomaHoja;
  original: DocumentoHoja;
  propuesta: DocumentoHoja;
  cambios: readonly CambioPropuesto[];
  /** Cosas que faltan y NO se inventaron (p. ej. «faltan las fechas de tu monitoria»). */
  avisos: readonly string[];
}

// ---------- Plantillas y editor ----------

export interface GuiaSeccion {
  seccion: 'contacto' | 'perfil' | 'experiencia' | 'educacion' | 'habilidades';
  /** Qué va en la sección. */
  que: string;
  /** Un ejemplo de cómo escribirlo (texto genérico, no datos de la persona). */
  ejemplo: string;
}

export interface PlantillaDetalle extends Plantilla {
  guias: readonly GuiaSeccion[];
}

export interface SolicitudBorrador {
  plantillaId?: string;
  versionId?: string;
  /** Abre el editor sobre una hoja adaptada a una vacante. */
  adaptacionId?: string;
  idioma?: IdiomaHoja;
}

export interface Borrador {
  id: string;
  plantillaId: string;
  idioma: IdiomaHoja;
  /** Correo de la cuenta (solo se muestra; no se edita). */
  correo: string;
  valores: ValoresHoja;
  /** ISO 8601 del último guardado, o null si aún no se guardó. */
  guardadoEn: string | null;
}
