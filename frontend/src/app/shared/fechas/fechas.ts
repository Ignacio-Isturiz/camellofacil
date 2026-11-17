/** Utilidades de fechas compartidas por las pantallas (formatos en el idioma activo). */

const formateadores = new Map<string, Intl.DateTimeFormat>();

/** Crear un Intl.DateTimeFormat es costoso: se reutiliza uno por idioma y formato. */
export function formatoFecha(idioma: string, opciones: Intl.DateTimeFormatOptions): Intl.DateTimeFormat {
  const clave = idioma + JSON.stringify(opciones);
  let f = formateadores.get(clave);
  if (!f) {
    f = new Intl.DateTimeFormat(idioma, opciones);
    formateadores.set(clave, f);
  }
  return f;
}

/** `AAAA-MM` → «ene 2024» en el idioma dado. Vacío si no hay fecha válida. */
export function mesCorto(valor: string, idioma: string): string {
  const m = /^(\d{4})-(\d{2})$/.exec(valor);
  if (!m) return '';
  const fecha = new Date(Number(m[1]), Number(m[2]) - 1, 1);
  return formatoFecha(idioma, { month: 'short', year: 'numeric' }).format(fecha).replace(/\./g, '');
}

/** Rango «ene 2024 – jun 2025», o solo el extremo que exista. */
export function rangoMeses(inicio: string, fin: string, idioma: string): string {
  const a = mesCorto(inicio, idioma);
  const b = mesCorto(fin, idioma);
  return a && b ? `${a} – ${b}` : a || b;
}
