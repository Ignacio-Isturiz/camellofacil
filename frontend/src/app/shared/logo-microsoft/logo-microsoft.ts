import { ChangeDetectionStrategy, Component } from '@angular/core';

/** Logo de Microsoft (cuatro cuadros de colores). Decorativo: el botón ya dice "Microsoft". */
@Component({
  selector: 'app-logo-microsoft',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <svg viewBox="0 0 21 21" aria-hidden="true" focusable="false">
      <rect x="1" y="1" width="9" height="9" fill="#f25022" />
      <rect x="11" y="1" width="9" height="9" fill="#7fba00" />
      <rect x="1" y="11" width="9" height="9" fill="#00a4ef" />
      <rect x="11" y="11" width="9" height="9" fill="#ffb900" />
    </svg>
  `,
  styles: `
    :host {
      display: inline-flex;
      flex: none;
    }
    svg {
      width: 18px;
      height: 18px;
    }
  `,
})
export class LogoMicrosoft {}
