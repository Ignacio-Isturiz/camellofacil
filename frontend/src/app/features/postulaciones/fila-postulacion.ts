import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';

import { Postulacion } from '../../core/postulaciones/postulaciones.modelos';
import { Icono } from '../../shared/icono/icono';

/** Una fila del historial: la vacante, la marca «Diste clic en postular», la hoja que llevó y sus acciones. */
@Component({
  selector: 'app-fila-postulacion',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoPipe, Icono],
  templateUrl: './fila-postulacion.html',
  styleUrl: './fila-postulacion.scss',
})
export class FilaPostulacion {
  private readonly i18n = inject(TranslocoService);

  readonly postulacion = input.required<Postulacion>();
  readonly ver = output<void>();
  readonly quitar = output<void>();

  protected readonly iniciales = computed(() =>
    this.postulacion()
      .empresa.split(' ')
      .filter((p) => p.length > 2)
      .slice(0, 2)
      .map((p) => p[0])
      .join('')
      .toUpperCase(),
  );

  /** Tres tonos fijos según la empresa, para distinguir filas sin depender de imágenes externas. */
  protected readonly tono = computed(() => {
    const e = this.postulacion().empresa;
    let h = 0;
    for (const c of e) h = (h * 31 + c.charCodeAt(0)) % 3;
    return (['rojo', 'oro', 'verde'] as const)[h];
  });

  /** «Hoy, 3:40 p. m.» / «Ayer, 10:15 a. m.» / «8 nov, 6:02 p. m.» en el idioma activo. */
  protected readonly fecha = computed(() => {
    const d = new Date(this.postulacion().ultimoClicEn);
    if (Number.isNaN(d.getTime())) return '';
    // es-CO da la hora en formato 12 horas («3:40 p. m.»), como en el diseño.
    const lang = this.i18n.getActiveLang() === 'es' ? 'es-CO' : 'en-US';
    const hora = new Intl.DateTimeFormat(lang, { hour: 'numeric', minute: '2-digit' }).format(d);
    const hoy = new Date();
    const inicio = (x: Date) => new Date(x.getFullYear(), x.getMonth(), x.getDate()).getTime();
    const dias = Math.round((inicio(hoy) - inicio(d)) / 86_400_000);
    if (dias === 0) return `${this.i18n.translate('postulaciones.hoy')}, ${hora}`;
    if (dias === 1) return `${this.i18n.translate('postulaciones.ayer')}, ${hora}`;
    const dia = new Intl.DateTimeFormat(lang, { day: 'numeric', month: 'short' }).format(d).replace(/\./g, '');
    return `${dia}, ${hora}`;
  });
}
