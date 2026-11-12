import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideTransloco } from '@jsverse/transloco';

import { provideApiConfiguration } from '../../core/api/api-configuration';
import { Conexion } from './conexion';

/** El componente usa promesas (Api.invoke): hay que dejar que se resuelvan antes de comprobar la vista. */
const esperarPromesas = () => new Promise<void>((resolver) => setTimeout(resolver));

describe('Conexion', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Conexion],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideApiConfiguration(''),
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

  it('muestra el mensaje que devuelve el backend', async () => {
    const fixture = TestBed.createComponent(Conexion);
    const http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    http.expectOne('/api/v1/conexion').flush({ mensaje: 'Conexión con el backend establecida' });
    await esperarPromesas();
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).querySelector('.ok')?.textContent).toContain(
      'Conexión con el backend establecida',
    );
  });

  it('muestra un error claro si el backend no responde', async () => {
    const fixture = TestBed.createComponent(Conexion);
    const http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    http.expectOne('/api/v1/conexion').flush(null, { status: 500, statusText: 'Error' });
    await esperarPromesas();
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).querySelector('.error')).not.toBeNull();
  });
});
