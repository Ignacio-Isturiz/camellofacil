import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink, RouterLinkActive } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';

import { Encabezado } from '../../shared/encabezado/encabezado';
import { Pie } from '../../shared/pie/pie';

type Documento = 'terminos' | 'privacidad';

/** Secciones de cada documento (sus textos viven en i18n: `legal.<documento>.<seccion>`). */
const SECCIONES: Record<Documento, readonly string[]> = {
  terminos: ['quien', 'que', 'vacantes', 'hoja', 'cuenta'],
  privacidad: ['responsable', 'datos', 'finalidades', 'ia', 'derechos', 'conservacion'],
};

/**
 * /terminos y /privacidad. BORRADOR versión 1.0, pendiente de revisión legal: la versión debe coincidir con
 * `ConsentimientoService.VERSION_TERMINOS` del backend, que es la que acepta cada persona.
 */
@Component({
  selector: 'app-documento-legal',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, RouterLinkActive, TranslocoPipe, Encabezado, Pie],
  template: `
    <app-encabezado />
    <main class="contenedor documento">
      <nav class="pestanas" [attr.aria-label]="'legal.documentos' | transloco">
        <a routerLink="/terminos" routerLinkActive="pestana--activa" class="pestana">{{ 'legal.terminos.corto' | transloco }}</a>
        <a routerLink="/privacidad" routerLinkActive="pestana--activa" class="pestana">{{ 'legal.privacidad.corto' | transloco }}</a>
      </nav>

      <header class="cabecera">
        <p class="meta">
          {{ 'legal.version' | transloco }}
          <span class="etiqueta">{{ 'legal.borrador' | transloco }}</span>
        </p>
        <h1>{{ 'legal.' + documento() + '.titulo' | transloco }}</h1>
      </header>

      @for (s of secciones(); track s; let i = $index) {
        <section class="seccion">
          <h2>{{ i + 1 }}. {{ 'legal.' + documento() + '.' + s + '.titulo' | transloco }}</h2>
          <p>{{ 'legal.' + documento() + '.' + s + '.texto' | transloco }}</p>
        </section>
      }
    </main>
    <app-pie />
  `,
  styles: `
    :host {
      display: block;
    }
    .documento {
      display: grid;
      gap: 20px;
      max-width: 760px;
      padding-block: 32px 48px;
    }
    .pestanas {
      display: flex;
      gap: 8px;
    }
    .pestana {
      padding: 8px 18px;
      border: 1.5px solid var(--gris-300);
      border-radius: 999px;
      font: 600 14px/20px var(--fuente-texto);
      text-decoration: none;
    }
    .pestana--activa {
      border-color: var(--rojo);
      background: var(--rojo-50);
      color: var(--rojo);
    }
    .cabecera {
      display: grid;
      gap: 8px;
    }
    .meta {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 8px;
      color: var(--gris-600);
      font-size: 13px;
    }
    h1 {
      font-size: 30px;
      line-height: 36px;
    }
    .seccion {
      display: grid;
      gap: 6px;
    }
    h2 {
      font-size: 18px;
      line-height: 24px;
    }
    .seccion p {
      color: var(--gris-600);
      line-height: 24px;
    }
  `,
})
export class DocumentoLegal {
  /** Cada ruta (/terminos, /privacidad) indica su documento en `data`. */
  private readonly doc: Documento = inject(ActivatedRoute).snapshot.data['documento'] ?? 'terminos';
  protected readonly documento = () => this.doc;
  protected readonly secciones = () => SECCIONES[this.doc];
}
