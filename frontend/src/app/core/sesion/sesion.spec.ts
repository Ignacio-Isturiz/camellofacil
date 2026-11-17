import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { exigirSesion } from './exigir-sesion';
import { irAInicioConSesion } from './ir-a-inicio-con-sesion';
import { RUTA_YO, Sesion } from './sesion';
import { sesionVencidaInterceptor } from './sesion-vencida.interceptor';

/** Datos ficticios: nunca datos reales de estudiantes en las pruebas (Ley 1581). */
const YO = { correo: 'ana.prueba@ejemplo.edu.co', nombre: 'Ana Prueba Ficticia' };
const esperar = () => new Promise<void>((r) => setTimeout(r));

describe('Sesion', () => {
  let sesion: Sesion;
  let ctrl: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(withInterceptors([sesionVencidaInterceptor])), provideHttpClientTesting()],
    });
    sesion = TestBed.inject(Sesion);
    ctrl = TestBed.inject(HttpTestingController);
  });

  afterEach(() => ctrl.verify());

  it('restaurar arma el usuario con sus iniciales', async () => {
    const r = sesion.restaurar();
    ctrl.expectOne(RUTA_YO).flush(YO);
    expect(await r).toBe(true);
    expect(sesion.usuario()).toEqual({ correo: YO.correo, nombre: YO.nombre, iniciales: 'AP' });
  });

  it('sin cookie (401) no hay sesión', async () => {
    const r = sesion.restaurar();
    ctrl.expectOne(RUTA_YO).flush(null, { status: 401, statusText: 'No autorizado' });
    expect(await r).toBe(false);
    expect(sesion.activa()).toBe(false);
  });

  it('comprobar reutiliza la respuesta ya recibida (una sola petición a /me)', async () => {
    const a = sesion.comprobar();
    ctrl.expectOne(RUTA_YO).flush(null, { status: 401, statusText: 'No autorizado' });
    expect(await a).toBe(false);
    expect(await sesion.comprobar()).toBe(false);
    ctrl.expectNone(RUTA_YO);
  });

  it('cerrar olvida a la persona y avisa al backend', async () => {
    const r = sesion.restaurar();
    ctrl.expectOne(RUTA_YO).flush(YO);
    await r;
    const c = sesion.cerrar();
    expect(sesion.activa()).toBe(false);
    ctrl.expectOne('/api/v1/auth/logout').flush(null);
    await c;
    expect(await sesion.comprobar()).toBe(false);
  });

  it('un 401 con sesión activa la da por vencida y lleva a /login', async () => {
    const r = sesion.restaurar();
    ctrl.expectOne(RUTA_YO).flush(YO);
    await r;
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    TestBed.inject(HttpClient).get('/api/v1/perfil').subscribe({ error: () => undefined });
    ctrl.expectOne('/api/v1/perfil').flush(null, { status: 401, statusText: 'No autorizado' });
    expect(sesion.activa()).toBe(false);
    expect(navegar).toHaveBeenCalledWith(['/login'], { queryParams: { error: 'expirada' } });
  });

  it('exigirSesion manda a /login si el backend dice que no hay sesión', async () => {
    const resultado = TestBed.runInInjectionContext(() => exigirSesion({} as never, {} as never)) as Promise<unknown>;
    await esperar();
    ctrl.expectOne(RUTA_YO).flush(null, { status: 401, statusText: 'No autorizado' });
    expect(String(await resultado)).toBe('/login');
  });

  it('irAInicioConSesion lleva a /inicio a quien ya entró', async () => {
    const resultado = TestBed.runInInjectionContext(() => irAInicioConSesion({} as never, {} as never)) as Promise<unknown>;
    await esperar();
    ctrl.expectOne(RUTA_YO).flush(YO);
    expect(String(await resultado)).toBe('/inicio');
  });
});
