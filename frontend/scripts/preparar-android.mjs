// Crea (o actualiza) el proyecto de Android de la app con Capacitor y le aplica la configuración de seguridad.
//
// El proyecto frontend/android/ NO se guarda en Git: es código generado por Capacitor, y así no hay que mantener
// (ni revisar) un proyecto nativo mientras nadie lo compile. Este script lo vuelve a crear igual cada vez:
//   npm run cap:preparar
//
// Es idempotente: se puede ejecutar de nuevo sin dañar nada.
import { execSync } from 'node:child_process';
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';

const ejecutar = (comando) => execSync(comando, { stdio: 'inherit' });

const MANIFIESTO = 'android/app/src/main/AndroidManifest.xml';
const RED = 'android/app/src/main/res/xml/network_security_config.xml';

const CONFIG_DE_RED = `<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <!-- El tráfico va siempre cifrado (https). -->
    <base-config cleartextTrafficPermitted="false" />
    <!-- Excepción solo para probar en el emulador contra el servidor del computador (CAP_SERVER_URL=http://10.0.2.2:4200). -->
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="false">10.0.2.2</domain>
    </domain-config>
</network-security-config>
`;

// 1) La web compilada (Capacitor la copia al proyecto) y el proyecto de Android
if (!existsSync('dist/frontend/browser')) ejecutar('npm run build');
if (!existsSync('android')) ejecutar('npx cap add android');

// 2) Seguridad: sin copias de seguridad de la app (guardaría la sesión y datos de la hoja) y sin tráfico http
//    salvo hacia el emulador. Si la plantilla de Capacitor cambia y esto no se puede aplicar, se detiene: mejor
//    fallar que dejar el valor por defecto, que es menos seguro.
let manifiesto = readFileSync(MANIFIESTO, 'utf8');
if (!manifiesto.includes('android:networkSecurityConfig')) {
  manifiesto = manifiesto.replace(
    'android:allowBackup="true"',
    'android:allowBackup="false"\n        android:usesCleartextTraffic="false"\n        android:networkSecurityConfig="@xml/network_security_config"',
  );
  writeFileSync(MANIFIESTO, manifiesto);
}
manifiesto = readFileSync(MANIFIESTO, 'utf8');
for (const requerido of ['android:allowBackup="false"', 'android:usesCleartextTraffic="false"', 'android:networkSecurityConfig']) {
  if (!manifiesto.includes(requerido)) {
    throw new Error(`No se pudo aplicar ${requerido} en ${MANIFIESTO}: revisa la plantilla de Capacitor.`);
  }
}
mkdirSync('android/app/src/main/res/xml', { recursive: true });
writeFileSync(RED, CONFIG_DE_RED);

// 3) Íconos y pantalla de arranque desde frontend/resources/, y sincronizar la web
ejecutar('npm run cap:iconos');
ejecutar('npx cap sync android');
console.log('\nListo: abre frontend/android/ en Android Studio, o compila con Gradle.');
