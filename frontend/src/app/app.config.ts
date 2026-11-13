import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, isDevMode, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, withInMemoryScrolling } from '@angular/router';
import { provideTransloco } from '@jsverse/transloco';

import { routes } from './app.routes';
import { provideApiConfiguration } from './core/api/api-configuration';
import { ultimoErrorInterceptor } from './core/http/ultimo-error';
import { credencialesInterceptor } from './core/http/credenciales.interceptor';
import { TranslocoHttpLoader } from './core/i18n/transloco-loader';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    // anchorScrolling: los enlaces del menú (#como-funciona, #vacantes…) bajan a su sección.
    provideRouter(
      routes,
      withInMemoryScrolling({ anchorScrolling: 'enabled', scrollPositionRestoration: 'enabled' }),
    ),
    // HttpClient ya reenvía la cookie XSRF-TOKEN como cabecera X-XSRF-TOKEN (CSRF del backend).
    provideHttpClient(withFetch(), withInterceptors([credencialesInterceptor, ultimoErrorInterceptor])),
    // Cliente generado desde OpenAPI. Ruta relativa: en desarrollo pasa por el proxy (proxy.conf.json).
    provideApiConfiguration(''),
    provideTransloco({
      config: {
        availableLangs: ['es', 'en'],
        defaultLang: 'es',
        fallbackLang: 'es',
        reRenderOnLangChange: true,
        prodMode: !isDevMode(),
      },
      loader: TranslocoHttpLoader,
    }),
  ],
};
