import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideTransloco } from '@jsverse/transloco';

import { HojaDeVidaServicio } from './hoja-de-vida/hoja-de-vida.servicio';
import { InicioServicio } from './inicio/inicio.servicio';
import { PerfilServicio } from './perfil/perfil.servicio';
import { PostulacionesServicio } from './postulaciones/postulaciones.servicio';
import { RUTA_YO, Sesion } from './sesion/sesion';
import { VacantesServicio } from './vacantes/vacantes.servicio';
import { VacanteApi } from './vacantes/vacantes.modelos';

/** Datos ficticios (Ley 1581). */
const vacante = (id: string): VacanteApi => ({
  id,
  cargo: 'Practicante ' + id,
  empresa: 'Empresa de prueba',
  tipo: 'practica',
  ciudad: null,
  modalidad: 'remoto',
  publicadaEn: new Date().toISOString(),
  fuente: null,
  carreras: [],
});
const ERROR = { status: 500, statusText: 'Error' };
const esperar = () => new Promise<void>((r) => setTimeout(r));

let ctrl: HttpTestingController;
beforeEach(() => {
  TestBed.configureTestingModule({
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
  });
  ctrl = TestBed.inject(HttpTestingController);
});
afterEach(() => ctrl.verify());

describe('VacantesServicio', () => {
  it('pide la lista una sola vez aunque Inicio y la lista la pidan a la vez', () => {
    const s = TestBed.inject(VacantesServicio);
    s.cargar();
    s.cargar();
    ctrl.expectOne('/api/v1/vacantes').flush({ vacantes: [vacante('a')], guardadas: ['a'] });
    expect(s.todas().length).toBe(1);
    expect(s.guardadas().has('a')).toBe(true);
    s.cargar(); // todavía vigente: no vuelve a pedir
    ctrl.expectNone('/api/v1/vacantes');
  });

  it('guardar se ve al instante y se deshace si el servidor falla', () => {
    const s = TestBed.inject(VacantesServicio);
    s.alternarGuardada('a');
    expect(s.guardadas().has('a')).toBe(true);
    ctrl.expectOne({ method: 'PUT', url: '/api/v1/vacantes/a/guardada' }).flush(null, ERROR);
    expect(s.guardadas().has('a')).toBe(false);
  });

  it('al abrir otra vacante se cancela el detalle anterior', () => {
    const s = TestBed.inject(VacantesServicio);
    s.cargarDetalle('a');
    const primera = ctrl.expectOne('/api/v1/vacantes/a');
    s.cargarDetalle('b');
    expect(primera.cancelled).toBe(true);
    ctrl.expectOne('/api/v1/vacantes/b').flush({ vacante: vacante('b'), descripcion: null, responsabilidades: [], requisitos: [] });
    expect(s.detalle()?.id).toBe('b');
  });

  it('una vacante que no existe queda como «no encontrada»', () => {
    const s = TestBed.inject(VacantesServicio);
    s.cargarDetalle('x');
    ctrl.expectOne('/api/v1/vacantes/x').flush(null, { status: 404, statusText: 'No existe' });
    expect(s.estadoDetalle()).toBe('no-encontrada');
  });
});

describe('PostulacionesServicio', () => {
  it('una búsqueda nueva cancela la anterior (no la pisa una respuesta vieja)', () => {
    const s = TestBed.inject(PostulacionesServicio);
    s.cargar('dat', 'recientes');
    const vieja = ctrl.expectOne((r) => r.url === '/api/v1/postulaciones' && r.params.get('q') === 'dat');
    s.cargar('datos', 'recientes');
    expect(vieja.cancelled).toBe(true);
    ctrl.expectOne((r) => r.params.get('q') === 'datos').flush({ items: [], clicsUltimos90Dias: 0 });
    expect(s.estado()).toBe('listo');
  });

  it('quitar saca la fila solo cuando el servidor lo acepta', () => {
    const s = TestBed.inject(PostulacionesServicio);
    s.cargar('', 'recientes');
    const item = { id: 'p1', vacanteId: 'a', cargo: 'X', empresa: 'Y', ciudad: null, modalidad: 'remoto', tipo: 'practica', ultimoClicEn: '', clics: 1, hojaUsada: 'base' };
    ctrl.expectOne((r) => r.url === '/api/v1/postulaciones').flush({ items: [item], clicsUltimos90Dias: 1 });
    s.quitar('p1').subscribe();
    expect(s.items().length).toBe(1);
    ctrl.expectOne({ method: 'DELETE', url: '/api/v1/postulaciones/p1' }).flush(null);
    expect(s.items().length).toBe(0);
  });
});

describe('InicioServicio', () => {
  it('al cambiar de página rápido solo cuenta la última', () => {
    const s = TestBed.inject(InicioServicio);
    s.cargarResumen(0);
    ctrl.expectOne((r) => r.params.get('pagina') === '0').flush({ vacantesNuevas: 0, recomendadas: [], pagina: 0, totalPaginas: 3, total: 9, busqueda: null });
    s.cargarResumen(1);
    const p1 = ctrl.expectOne((r) => r.params.get('pagina') === '1');
    s.cargarResumen(2);
    expect(p1.cancelled).toBe(true);
    expect(s.cambiandoPagina()).toBe(true);
    ctrl.expectOne((r) => r.params.get('pagina') === '2').flush({ vacantesNuevas: 0, recomendadas: [{ vacante: vacante('c'), nueva: true }], pagina: 2, totalPaginas: 3, total: 9, busqueda: null });
    expect(s.resumen()?.pagina).toBe(2);
    expect(s.cambiandoPagina()).toBe(false);
  });
});

describe('HojaDeVidaServicio', () => {
  const resumen = { estado: 'sin_hoja', hoja: null, pendientesRevision: null };

  it('el resumen no se pide dos veces a la vez, salvo que se fuerce', () => {
    const s = TestBed.inject(HojaDeVidaServicio);
    s.cargarResumen();
    s.cargarResumen();
    const primera = ctrl.expectOne('/api/v1/hoja-de-vida');
    s.cargarResumen(true);
    expect(primera.cancelled).toBe(true);
    ctrl.expectOne('/api/v1/hoja-de-vida').flush(resumen);
    expect(s.estadoResumen()).toBe('listo');
  });

  it('las plantillas se piden una vez por visita, y otra vez solo tras un error', () => {
    const s = TestBed.inject(HojaDeVidaServicio);
    s.cargarPlantillas();
    ctrl.expectOne('/api/v1/plantillas').flush(null, ERROR);
    s.cargarPlantillas();
    ctrl.expectOne('/api/v1/plantillas').flush([]);
    s.cargarPlantillas();
    ctrl.expectNone('/api/v1/plantillas');
  });
});

describe('HojaDeVidaServicio · recomendaciones de la IA', () => {
  const resumenCon = (ia: string, extra: object[] = []) => ({
    estado: 'confirmada',
    hoja: { harvard: { valor: 80, nivel: 'alto', detalle: '' }, ats: null, opcionesAts: [], observaciones: extra, recomendacionesIa: ia },
    pendientesRevision: null,
  });

  it('las pide una vez, deja el resumen con ellas y no toca el puntaje ATS elegido', () => {
    const s = TestBed.inject(HojaDeVidaServicio);
    s.cargarResumen();
    ctrl.expectOne('/api/v1/hoja-de-vida').flush(resumenCon('pendiente'));
    s.evaluarAts('palabras-clave');
    ctrl.expectOne('/api/v1/hoja-de-vida/puntajes/ats/palabras-clave').flush({ valor: 61, nivel: 'medio', detalle: '', opcionId: 'palabras-clave' });

    s.generarRecomendacionesIa();
    s.generarRecomendacionesIa(); // ya hay una en vuelo: no se repite
    expect(s.estadoIa()).toBe('generando');
    ctrl
      .expectOne({ method: 'POST', url: '/api/v1/hoja-de-vida/recomendaciones-ia' })
      .flush(resumenCon('lista', [{ id: 'ia-1', tipo: 'mejorar', titulo: 'Agrega cifras', detalle: '...', origen: 'ia' }]));

    expect(s.estadoIa()).toBe('inactivo');
    expect(s.resumen()?.hoja?.observaciones[0].origen).toBe('ia');
    expect(s.puntajeAts()?.opcionId).toBe('palabras-clave');
  });

  it('si falla queda en «error» y si ya usó las de hoy, en «limite»', () => {
    const s = TestBed.inject(HojaDeVidaServicio);
    s.generarRecomendacionesIa();
    ctrl.expectOne('/api/v1/hoja-de-vida/recomendaciones-ia').flush({}, { status: 503, statusText: 'No disponible' });
    expect(s.estadoIa()).toBe('error');

    s.generarRecomendacionesIa();
    ctrl.expectOne('/api/v1/hoja-de-vida/recomendaciones-ia').flush({}, { status: 429, statusText: 'Demasiadas' });
    expect(s.estadoIa()).toBe('limite');
  });
});

describe('PerfilServicio', () => {
  it('las carreras se piden una vez; si falla, se puede reintentar', async () => {
    const s = TestBed.inject(PerfilServicio);
    const a = s.carreras();
    ctrl.expectOne('/api/v1/perfil/carreras').flush(null, ERROR);
    await expect(a).rejects.toBeTruthy();
    const b = s.carreras();
    ctrl.expectOne('/api/v1/perfil/carreras').flush([{ codigo: 'datos', nombre: 'Ciencia de Datos' }]);
    expect(await b).toHaveLength(1);
    await s.carreras();
    ctrl.expectNone('/api/v1/perfil/carreras');
  });

  it('al cerrar sesión olvida el perfil de la persona anterior', async () => {
    const sesion = TestBed.inject(Sesion);
    const perfil = TestBed.inject(PerfilServicio);
    const r = sesion.restaurar();
    ctrl.expectOne(RUTA_YO).flush({ correo: 'ana.prueba@ejemplo.edu.co', nombre: 'Ana Prueba' });
    await r;
    const c = perfil.cargar();
    ctrl.expectOne('/api/v1/perfil').flush({ completo: true, nombre: 'Ana Prueba', correo: 'ana.prueba@ejemplo.edu.co', versionTerminosVigente: '1.0', datos: null, terminos: null, analisisIa: null });
    await c;
    expect(perfil.estado()).not.toBeNull();
    void sesion.cerrar();
    ctrl.expectOne('/api/v1/auth/logout').flush(null);
    TestBed.tick();
    await esperar();
    expect(perfil.estado()).toBeNull();
  });
});
