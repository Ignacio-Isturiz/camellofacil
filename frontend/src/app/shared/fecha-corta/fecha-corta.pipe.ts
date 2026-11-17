import { Pipe, PipeTransform, inject } from '@angular/core';
import { TranslocoService } from '@jsverse/transloco';

import { formatoFecha } from '../fechas/fechas';

/**
 * Fecha ISO → «3 nov 2026» en el idioma activo. Es impuro a propósito (debe cambiar al cambiar de idioma),
 * así que corre en cada revisión de la vista: por eso recuerda su último resultado y solo vuelve a formatear
 * si cambia la fecha o el idioma. El formateador de Intl se crea una vez por idioma (ver `formatoFecha`).
 */
@Pipe({ name: 'fechaCorta', pure: false })
export class FechaCortaPipe implements PipeTransform {
  private readonly i18n = inject(TranslocoService);
  private ultimo: { iso: string | null | undefined; idioma: string; texto: string } | null = null;

  transform(iso: string | null | undefined): string {
    const idioma = this.i18n.getActiveLang();
    if (this.ultimo && this.ultimo.iso === iso && this.ultimo.idioma === idioma) return this.ultimo.texto;
    const texto = fechaCorta(iso, idioma);
    this.ultimo = { iso, idioma, texto };
    return texto;
  }
}

export function fechaCorta(iso: string | null | undefined, idioma: string): string {
  if (!iso) return '';
  const fecha = new Date(iso);
  if (Number.isNaN(fecha.getTime())) return '';
  return formatoFecha(idioma, { day: 'numeric', month: 'short', year: 'numeric' }).format(fecha).replace(/\./g, '');
}
