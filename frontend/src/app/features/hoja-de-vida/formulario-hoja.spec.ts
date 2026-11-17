import { DatosExtraidos } from '../../core/hoja-de-vida/hoja-de-vida.modelos';
import { crearFormularioHoja, experienciaVacia, grupoExperiencia, valoresLeidos } from './formulario-hoja';

const campo = (valor: string) => ({ valor, estado: 'leido' as const, ayuda: null });

/** Datos ficticios (Ley 1581). */
const DATOS: DatosExtraidos = {
  archivo: { nombre: 'prueba.pdf', idioma: 'es' },
  contacto: { nombre: campo('Ana Prueba'), correo: campo('ana@ejemplo.edu.co'), telefono: campo('300'), ciudad: campo('Medellín') },
  perfil: campo('Perfil de prueba'),
  experiencia: [{ id: 'e1', cargo: campo('Monitor'), organizacion: campo('U. Ejemplo'), inicio: campo('2025-01'), fin: campo(''), descripcion: campo('Talleres') }],
  educacion: [{ id: 'd1', titulo: campo('Ingeniería'), institucion: campo('U. Ejemplo'), inicio: campo('2022-01'), fin: campo('') }],
  habilidades: campo('SQL'),
};

describe('formulario de la hoja de vida', () => {
  it('toma solo los valores de lo leído, sin el correo (que no se edita)', () => {
    const v = valoresLeidos(DATOS);
    expect(v.nombre).toBe('Ana Prueba');
    expect(v.experiencia[0]).toEqual({ id: 'e1', cargo: 'Monitor', organizacion: 'U. Ejemplo', inicio: '2025-01', fin: '', descripcion: 'Talleres' });
    expect('correo' in v).toBe(false);
  });

  it('el formulario devuelve exactamente lo que recibió', () => {
    const v = valoresLeidos(DATOS);
    expect(crearFormularioHoja(v).getRawValue()).toEqual(v);
  });

  it('aplica los mismos límites de largo que el backend', () => {
    const f = crearFormularioHoja(valoresLeidos(DATOS));
    f.controls.telefono.setValue('9'.repeat(31));
    expect(f.invalid).toBe(true);
    f.controls.telefono.setValue('300 000 0000');
    f.controls.experiencia.push(grupoExperiencia(experienciaVacia('nueva-1')));
    f.controls.experiencia.at(1).controls.inicio.setValue('2025-011');
    expect(f.invalid).toBe(true);
  });
});
