import { TestBed } from '@angular/core/testing';
import { FormControl } from '@angular/forms';
import { provideTransloco } from '@jsverse/transloco';

import { CampoExtraido } from '../../../core/hoja-de-vida/hoja-de-vida.modelos';
import { CampoExtraidoComponente } from './campo-extraido';

const leido = (valor: string): CampoExtraido => ({ valor, estado: 'leido', ayuda: null });
const noEncontrado: CampoExtraido = { valor: '', estado: 'no_encontrado', ayuda: null };

describe('CampoExtraidoComponente · casilla «En curso»', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CampoExtraidoComponente],
      providers: [
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

  function crear(control: FormControl<string>, campo: CampoExtraido, casilla: string | null) {
    const fixture = TestBed.createComponent(CampoExtraidoComponente);
    fixture.componentRef.setInput('control', control);
    fixture.componentRef.setInput('campo', campo);
    fixture.componentRef.setInput('etiqueta', 'Fin');
    fixture.componentRef.setInput('idCampo', 'fin');
    fixture.componentRef.setInput('tipo', 'month');
    fixture.componentRef.setInput('ayudaNoEncontrado', 'No encontramos la fecha.');
    fixture.componentRef.setInput('etiquetaEnCurso', casilla);
    fixture.detectChanges();
    const raiz: HTMLElement = fixture.nativeElement;
    return {
      fixture,
      raiz,
      casilla: () => raiz.querySelector<HTMLInputElement>('.campo__curso input'),
      entrada: () => raiz.querySelector<HTMLInputElement>('input.entrada'),
      actual: () => raiz.querySelector('.campo__curso-valor'),
    };
  }

  it('sin texto para la casilla no hay casilla (solo las fechas de fin la usan)', () => {
    const c = crear(new FormControl('2024-05', { nonNullable: true }), leido('2024-05'), null);
    expect(c.casilla()).toBeNull();
    expect(c.entrada()).not.toBeNull();
  });

  it('un fin vacío que la hoja leyó como tal empieza marcado como «en curso»', () => {
    const c = crear(new FormControl('', { nonNullable: true }), leido(''), 'En curso');
    expect(c.casilla()!.checked).toBe(true);
    expect(c.actual()).not.toBeNull();
    expect(c.entrada()).toBeNull();
  });

  it('si la fecha no se encontró, la casilla empieza sin marcar y se avisa; marcarla quita el aviso y vacía el fin', () => {
    const control = new FormControl('2024-05', { nonNullable: true });
    const c = crear(control, noEncontrado, 'En curso');
    let resueltos = 0;
    c.fixture.componentInstance.resuelto.subscribe(() => resueltos++);

    expect(c.casilla()!.checked).toBe(false);
    expect(c.raiz.querySelector('.campo__ayuda')).not.toBeNull();

    c.casilla()!.click();
    c.fixture.detectChanges();

    expect(control.value).toBe('');
    expect(c.actual()).not.toBeNull();
    expect(c.raiz.querySelector('.campo__ayuda')).toBeNull();
    expect(resueltos).toBe(1);
  });

  it('al desmarcarla vuelve a pedir la fecha', () => {
    const control = new FormControl('', { nonNullable: true });
    const c = crear(control, leido(''), 'En curso');

    c.casilla()!.click();
    c.fixture.detectChanges();

    expect(c.entrada()).not.toBeNull();
    expect(c.actual()).toBeNull();
  });
});
