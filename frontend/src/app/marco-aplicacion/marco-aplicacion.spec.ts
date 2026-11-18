import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, output } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideTransloco } from '@jsverse/transloco';

import { PerfilServicio } from '../core/perfil/perfil.servicio';
import { CompletarPerfil } from '../features/perfil/completar-perfil/completar-perfil';
import { MarcoAplicacion } from './marco-aplicacion';

/** El modal real descarga formularios y pide carreras: aquí basta uno vacío con la misma etiqueta y salida. */
@Component({ selector: 'app-completar-perfil', template: '' })
class CompletarPerfilFalso {
  readonly salir = output<void>();
}

/**
 * Con el modal del primer ingreso abierto la API responde 403: la pantalla de fondo no debe montarse (si no, se
 * quedaría con errores). Al completar el perfil se monta y pide sus datos.
 */
describe('MarcoAplicacion', () => {
  beforeEach(async () => {
    TestBed.overrideComponent(MarcoAplicacion, {
      remove: { imports: [CompletarPerfil] },
      add: { imports: [CompletarPerfilFalso] },
    });
    await TestBed.configureTestingModule({
      imports: [MarcoAplicacion],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
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
  });

  const hayPantalla = (raiz: HTMLElement) => raiz.querySelector('#contenido-app')!.querySelector('router-outlet') !== null;
  const esperar = () => new Promise<void>((resolver) => setTimeout(resolver));

  it('no monta la pantalla mientras falta completar el perfil y la monta al completarlo', async () => {
    const fixture = TestBed.createComponent(MarcoAplicacion);
    const http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    // Mientras no se sabe el estado del perfil, la pantalla se muestra como siempre.
    expect(hayPantalla(fixture.nativeElement)).toBe(true);

    http.expectOne('/api/v1/perfil').flush({ completo: false });
    await esperar();
    fixture.detectChanges();
    expect(hayPantalla(fixture.nativeElement)).toBe(false);

    const completar = TestBed.inject(PerfilServicio).completar({} as never);
    http.expectOne({ method: 'POST', url: '/api/v1/perfil' }).flush({ completo: true });
    await completar;
    fixture.detectChanges();
    expect(hayPantalla(fixture.nativeElement)).toBe(true);
  });

  it('con el perfil ya completo muestra la pantalla de inmediato', async () => {
    const fixture = TestBed.createComponent(MarcoAplicacion);
    const http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    http.expectOne('/api/v1/perfil').flush({ completo: true });
    await esperar();
    fixture.detectChanges();
    expect(hayPantalla(fixture.nativeElement)).toBe(true);
  });
});
