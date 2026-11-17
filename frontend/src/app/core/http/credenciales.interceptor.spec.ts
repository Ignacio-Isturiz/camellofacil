import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { credencialesInterceptor, esNuestroApi } from './credenciales.interceptor';

describe('esNuestroApi', () => {
  it('acepta las rutas relativas del API', () => {
    expect(esNuestroApi('/api/v1/perfil')).toBe(true);
  });

  it('acepta el dominio propio y sus subdominios por https', () => {
    expect(esNuestroApi('https://camellofacil.com/api/v1/perfil')).toBe(true);
    expect(esNuestroApi('https://api.camellofacil.com/api/v1/perfil')).toBe(true);
  });

  it('rechaza dominios que solo contienen el texto del propio', () => {
    expect(esNuestroApi('https://camellofacil.com.otro.io/api')).toBe(false);
    expect(esNuestroApi('https://otrocamellofacil.com/api')).toBe(false);
    expect(esNuestroApi('https://otro.io/?d=camellofacil.com')).toBe(false);
  });

  it('rechaza http sin cifrar y textos que no son URL', () => {
    expect(esNuestroApi('http://api.camellofacil.com/api')).toBe(false);
    expect(esNuestroApi('i18n/es.json')).toBe(false);
  });
});

describe('credencialesInterceptor', () => {
  beforeEach(() =>
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([credencialesInterceptor])), provideHttpClientTesting()],
    }),
  );

  it('envía la cookie solo a nuestro API', () => {
    const http = TestBed.inject(HttpClient);
    const ctrl = TestBed.inject(HttpTestingController);
    http.get('/api/v1/perfil').subscribe();
    http.get('https://camellofacil.com.otro.io/x').subscribe();
    expect(ctrl.expectOne('/api/v1/perfil').request.withCredentials).toBe(true);
    expect(ctrl.expectOne('https://camellofacil.com.otro.io/x').request.withCredentials).toBe(false);
    ctrl.verify();
  });
});
