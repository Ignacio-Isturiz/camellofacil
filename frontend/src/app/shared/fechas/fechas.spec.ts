import { TestBed } from '@angular/core/testing';
import { TranslocoService, provideTransloco } from '@jsverse/transloco';

import { FechaCortaPipe } from '../fecha-corta/fecha-corta.pipe';
import { formatoFecha, mesCorto, rangoMeses } from './fechas';
import { urlSegura } from '../../core/postulaciones/pestana-externa';

describe('fechas', () => {
  it('mesCorto convierte AAAA-MM al mes corto sin puntos', () => {
    expect(mesCorto('2024-01', 'es')).toBe('ene 2024');
    expect(mesCorto('', 'es')).toBe('');
    expect(mesCorto('enero', 'es')).toBe('');
  });

  it('rangoMeses une los dos extremos o deja el que exista', () => {
    expect(rangoMeses('2024-01', '2024-06', 'es')).toBe('ene 2024 – jun 2024');
    expect(rangoMeses('2024-01', '', 'es')).toBe('ene 2024');
    expect(rangoMeses('', '', 'es')).toBe('');
  });

  it('formatoFecha reutiliza el mismo formateador por idioma y opciones', () => {
    const a = formatoFecha('es', { month: 'long' });
    expect(formatoFecha('es', { month: 'long' })).toBe(a);
    expect(formatoFecha('en', { month: 'long' })).not.toBe(a);
  });
});

describe('FechaCortaPipe', () => {
  beforeEach(() =>
    TestBed.configureTestingModule({
      providers: [
        FechaCortaPipe,
        provideTransloco({
          config: { availableLangs: ['es', 'en'], defaultLang: 'es' },
          loader: class {
            getTranslation() {
              return Promise.resolve({});
            }
          } as never,
        }),
      ],
    }),
  );

  it('formatea en el idioma activo y cambia con él', () => {
    const pipe = TestBed.inject(FechaCortaPipe);
    expect(pipe.transform('2026-11-03T15:00:00Z')).toBe('3 nov 2026');
    TestBed.inject(TranslocoService).setActiveLang('en');
    expect(pipe.transform('2026-11-03T15:00:00Z')).toBe('Nov 3, 2026');
  });

  it('devuelve vacío con fechas ausentes o inválidas', () => {
    const pipe = TestBed.inject(FechaCortaPipe);
    expect(pipe.transform(null)).toBe('');
    expect(pipe.transform('no es fecha')).toBe('');
  });
});

describe('urlSegura', () => {
  it('solo deja pasar http y https', () => {
    expect(urlSegura('https://empresa.ejemplo/oferta')).toBe('https://empresa.ejemplo/oferta');
    expect(urlSegura('javascript:alert(1)')).toBeNull();
    expect(urlSegura('data:text/html,hola')).toBeNull();
    expect(urlSegura('no es una url')).toBeNull();
  });
});
