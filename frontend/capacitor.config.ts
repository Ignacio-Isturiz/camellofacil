import type { CapacitorConfig } from '@capacitor/cli';

/**
 * App móvil (Android) de camelloFácil. Es un envoltorio nativo que abre el sitio publicado: así la sesión (cookie de
 * Spring Session), el CSRF y el inicio con Microsoft funcionan igual que en el navegador, sin duplicar la API ni
 * tocar el backend. La app y el sitio se actualizan a la vez: no hay que volver a publicar la app por cada cambio.
 *
 * Para probar contra otro servidor (por ejemplo el de Docker en el emulador de Android, donde el computador es
 * 10.0.2.2) se define CAP_SERVER_URL antes de sincronizar:
 *   CAP_SERVER_URL=http://10.0.2.2:4200 npx cap sync android
 */
const servidorDePrueba = process.env['CAP_SERVER_URL'];
const urlPublica = 'https://camellofacil.com';

const config: CapacitorConfig = {
  appId: 'com.camellofacil.app',
  appName: 'camelloFácil',
  // Se empaqueta también la build de Angular (npm run build), pero la app abre `server.url`.
  webDir: 'dist/frontend/browser',
  server: {
    url: servidorDePrueba ?? urlPublica,
    // Solo se permite http en las pruebas locales; en producción todo va por https.
    cleartext: servidorDePrueba?.startsWith('http://') ?? false,
    androidScheme: 'https',
    // Dentro de la app solo se navega por el sitio y por el inicio de sesión de Microsoft (Entra ID). Cualquier otra
    // dirección (las ofertas de empleo, la política de privacidad externa…) se abre en el navegador del teléfono.
    allowNavigation: ['camellofacil.com', '*.camellofacil.com', 'login.microsoftonline.com', 'login.live.com'],
  },
  android: {
    backgroundColor: '#FAF6EE',
    allowMixedContent: false,
  },
};

export default config;
