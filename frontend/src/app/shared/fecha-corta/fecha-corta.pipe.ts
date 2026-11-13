import { Pipe, PipeTransform, inject } from '@angular/core';
import { TranslocoService } from '@jsverse/transloco';

/** Fecha ISO → «3 nov 2026» en el idioma activo. Impura a propósito: debe cambiar al cambiar de idioma. */
@Pipe({ name: 'fechaCorta', pure: false })
export class FechaCortaPipe implements PipeTransform {
  private readonly i18n = inject(TranslocoService);

  transform(iso: string | null | undefined): string {
    if (!iso) return '';
    const fecha = new Date(iso);
    if (Number.isNaN(fecha.getTime())) return '';
    return new Intl.DateTimeFormat(this.i18n.getActiveLang(), { day: 'numeric', month: 'short', year: 'numeric' })
      .format(fecha)
      .replace(/\./g, '');
  }
}
