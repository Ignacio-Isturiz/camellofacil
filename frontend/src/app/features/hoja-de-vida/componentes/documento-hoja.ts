import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { diffWords } from 'diff';

import { DocumentoHoja, EstiloPlantilla } from '../../../core/hoja-de-vida/hoja-de-vida.modelos';

interface Trozo {
  texto: string;
  /** Texto nuevo respecto del original (redacción ajustada). */
  nuevo: boolean;
}

interface BloqueVista {
  trozos: readonly Trozo[];
}

interface SeccionVista {
  id: string;
  titulo: string;
  movida: boolean;
  bloques: readonly BloqueVista[];
}

/**
 * Dibuja un documento de hoja de vida estructurado (lo que arma el backend) con el estilo de una plantilla.
 * Si se le da el `original`, marca con jsdiff las palabras que cambiaron de redacción y las secciones que se movieron.
 */
@Component({
  selector: 'app-documento-hoja',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './documento-hoja.html',
  styleUrl: './documento-hoja.scss',
})
export class DocumentoHojaComponente {
  readonly documento = input.required<DocumentoHoja>();
  readonly estilo = input<EstiloPlantilla>('harvard');
  /** Documento de partida para comparar; si no se da, no se marca nada. */
  readonly original = input<DocumentoHoja | null>(null);
  /** Con `true`, solo se muestran las secciones que cambiaron (requiere `original`). */
  readonly soloCambios = input(false);

  protected readonly secciones = computed<readonly SeccionVista[]>(() => {
    const doc = this.documento();
    const base = this.original();
    const idsBase = base?.secciones.map((s) => s.id) ?? [];

    const vistas = doc.secciones.map((sec, i): SeccionVista => {
      const previa = base?.secciones.find((s) => s.id === sec.id);
      const movida = !!base && idsBase.indexOf(sec.id) !== i;
      const bloques = sec.bloques.map((b, j): BloqueVista => {
        const antes = previa?.bloques[j];
        if (!base) return { trozos: [{ texto: b, nuevo: false }] };
        if (antes === undefined) return { trozos: [{ texto: b, nuevo: true }] };
        // Se descartan las partes eliminadas: la vista muestra cómo queda, marcando lo agregado.
        return {
          trozos: diffWords(antes, b)
            .filter((p) => !p.removed)
            .map((p) => ({ texto: p.value, nuevo: !!p.added })),
        };
      });
      return { id: sec.id, titulo: sec.titulo, movida, bloques };
    });

    if (!this.soloCambios() || !base) return vistas;
    return vistas.filter((s) => s.movida || s.bloques.some((b) => b.trozos.some((t) => t.nuevo)));
  });
}
