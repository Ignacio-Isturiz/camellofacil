import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { provideTransloco } from '@jsverse/transloco';
import { vi } from 'vitest';

import { Plantillas } from './plantillas';

const PLANTILLA = {
  id: 'harvard-clasica',
  nombre: 'Harvard clásica',
  descripcion: 'Formato sobrio',
  categoria: 'harvard',
  estilo: 'harvard',
  idiomas: ['es', 'en'],
  etiqueta: null,
};

/** jsdom no trae el <dialog> modal: se simula lo justo (abrir, cerrar y avisar del cierre). */
function simularDialogo() {
  const abrir = vi.fn(function (this: HTMLDialogElement) {
    this.setAttribute('open', '');
  });
  HTMLDialogElement.prototype.showModal = abrir;
  HTMLDialogElement.prototype.close = function (this: HTMLDialogElement) {
    this.removeAttribute('open');
    this.dispatchEvent(new Event('close'));
  };
  return abrir;
}

describe('Plantillas · /hoja-de-vida/plantillas?vista=<id>', () => {
  async function montar(vista: string | null) {
    const showModal = simularDialogo();
    await TestBed.configureTestingModule({
      imports: [Plantillas],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap(vista ? { vista } : {}) } } },
        provideTransloco({
          config: { availableLangs: ['es'], defaultLang: 'es' },
          loader: class {
            getTranslation() {
              return Promise.resolve({});
            }
          } as never,
        }),
      ],
    }).compileComponents();
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(Plantillas);
    const http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    http.expectOne('/api/v1/plantillas').flush([PLANTILLA]);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return { fixture, http, showModal, navegar };
  }

  it('abre la vista previa pedida, quita el parámetro de la dirección y no la reabre al cerrarla', async () => {
    const { fixture, http, showModal, navegar } = await montar('harvard-clasica');

    expect(showModal).toHaveBeenCalledTimes(1);
    // Recargar la página no debe volver a abrirla sola: el parámetro se elimina de la dirección.
    expect(navegar).toHaveBeenCalledWith([], expect.objectContaining({ queryParams: { vista: null }, replaceUrl: true }));
    http.expectOne('/api/v1/plantillas/harvard-clasica/vista-previa?idioma=es').flush({ nombre: 'Ana', contacto: [], secciones: [] });

    // Cerrar (la X o Esc) dispara el evento `close` del <dialog>.
    fixture.nativeElement.querySelector('dialog').dispatchEvent(new Event('close'));
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(showModal).toHaveBeenCalledTimes(1);
  });

  it('sin parámetro no se abre nada hasta que la persona pulsa «Vista previa»', async () => {
    const { showModal, navegar } = await montar(null);

    expect(showModal).not.toHaveBeenCalled();
    expect(navegar).not.toHaveBeenCalled();
  });
});
