# camellofacil.com — Montaje del proyecto

Guía para dejar el proyecto corriendo en tu computador. Hay dos formas de montarlo: **todo en Docker** (la más simple) o **desarrollo con recarga** (base de datos en Docker, backend y frontend en tu máquina).

## 1. Requisitos

| Herramienta | Versión | Comprobar con | Necesaria para |
|---|---|---|---|
| Git | cualquiera reciente | `git --version` | ambas |
| Docker Desktop | cualquiera reciente | `docker --version` | ambas |
| JDK | 21 o superior | `java -version` | solo desarrollo con recarga |
| Node.js | 24.14 o superior (LTS) | `node -v` | solo desarrollo con recarga |

Maven y Angular CLI **no** hay que instalarlos: el proyecto trae `mvnw` y usa `npx`.

## 2. Clonar el repositorio

```bash
git clone <URL-DEL-REPOSITORIO> camellofacil
cd camellofacil
```

## 3. Variables de entorno

Copia el archivo de ejemplo y ajusta la clave de la base de datos si quieres:

```bash
cp .env.example .env
```

En Windows (PowerShell): `Copy-Item .env.example .env`

El archivo `.env` está en `.gitignore` y no se sube a Git.

Abre Docker Desktop y espera a que diga **Engine running** antes de seguir.

## Opción A — Todo en Docker

Levanta la base de datos, el backend y el frontend con un solo comando, en la raíz del proyecto:

```bash
docker compose up -d --build
```

| Servicio | Contenedor | Dirección |
|---|---|---|
| Frontend (nginx) | `camellofacil-frontend` | http://localhost:4200 |
| Backend (Spring Boot) | `camellofacil-backend` | http://localhost:8080 |
| Base de datos (PostgreSQL 17) | `camellofacil-db` | `localhost:5433` |

La primera vez tarda unos minutos porque compila las imágenes. Después de cambiar código, vuelve a ejecutar el mismo comando para reconstruir.

Comandos útiles:

```bash
docker compose ps              # ver el estado de los contenedores
docker compose logs -f backend # ver los logs del backend (también: frontend, db)
docker compose down            # apagar todo (conserva los datos de la base)
docker compose down -v         # apagar y BORRAR los datos (base limpia)
```

## Opción B — Desarrollo con recarga automática

Solo la base de datos va en Docker; el backend y el frontend corren en tu máquina.

**1. Base de datos** (en la raíz del proyecto):

```bash
docker compose up -d db
```

**2. Backend** (terminal nueva):

```bash
cd backend
./mvnw spring-boot:run
```

En Windows (PowerShell): `.\mvnw.cmd spring-boot:run`. Queda en http://localhost:8080 y crea las tablas solo (migraciones Flyway). Lee las variables del `.env` de la raíz.

**3. Frontend** (otra terminal):

```bash
cd frontend
npm install
npm start
```

Queda en http://localhost:4200 y reenvía `/api` al backend. El navegador se recarga al guardar cambios.

Los puertos 8080 y 4200 son los mismos que usa la Opción A: no corras las dos a la vez.

## Comprobar que todo funciona

Abre http://localhost:4200. Debe aparecer **"Conexión con el backend establecida"**.

## Problemas comunes

| Síntoma | Causa y solución |
|---|---|
| `failed to connect to the docker API` | Docker Desktop está apagado. Ábrelo y espera a que esté en marcha. |
| `port is already allocated` / `Port 8080 was already in use` | Ya hay algo usando ese puerto (otro backend o contenedor). Ciérralo, o cambia el puerto con `BACKEND_PORT=18080` o `FRONTEND_PORT=14200` antes del comando. |
| `Port 4200 is already in use` | Ya hay un `npm start` corriendo. Ciérralo o usa ese. |
| Error de conexión a la base de datos | Revisa que el contenedor `camellofacil-db` esté `healthy` (`docker compose ps`) y que `.env` exista. |
| Los cambios de código no aparecen en Docker | Reconstruye con `docker compose up -d --build`. |
| `The Angular CLI requires a minimum Node.js version` | Actualiza Node.js a la versión indicada arriba (solo Opción B). |
| Puerto 5433 ocupado | Cambia el puerto en `docker-compose.yml` y en `DB_URL` del `.env`. |

## App móvil (Android, con Capacitor)

La app de Android es un envoltorio nativo (Capacitor) que abre **https://camellofacil.com**. Así la sesión, el CSRF y el inicio con Microsoft funcionan igual que en el navegador y el backend no cambia. La app se actualiza sola cuando se despliega el sitio; solo hay que volver a publicarla en Play Store si cambia el envoltorio (nombre, íconos, permisos o la versión de Capacitor).

El proyecto vive en `frontend/android/` y la configuración en `frontend/capacitor.config.ts`.

### Requisitos

| Herramienta | Para qué |
|---|---|
| [Android Studio](https://developer.android.com/studio) | Compilar, probar en emulador y generar el APK/AAB. Instala el SDK de Android. |
| JDK 21 | Lo usa Gradle (el mismo del backend). |

### Comandos (desde `frontend/`)

```bash
npm ci                 # dependencias
npm run cap:sync       # compila Angular y sincroniza el proyecto de Android
npm run cap:abrir      # abre el proyecto en Android Studio
npm run cap:iconos     # regenera íconos y pantalla de arranque desde frontend/resources/
```

En Android Studio: elige un emulador o un teléfono con depuración USB y pulsa **Run**.

### Probar contra tu Docker local

En el emulador de Android el computador es `10.0.2.2`. Con el entorno de Docker arriba (`docker compose up -d`):

```bash
CAP_SERVER_URL=http://10.0.2.2:4200 npx cap sync android
```

Antes de publicar, vuelve a sincronizar **sin** esa variable (`npm run cap:sync`) para que la app apunte a producción. Para el inicio con Microsoft contra `localhost` la redirect URI de Azure debe coincidir con lo que use el backend (`BACKEND_URL`).

### Publicar en Play Store

1. En Android Studio: **Build → Generate Signed Bundle / APK** → *Android App Bundle*. Crea y **guarda aparte** la llave de firma (keystore): sin ella no se puede actualizar la app.
2. Sube el `.aab` en [Play Console](https://play.google.com/console) (cuenta de desarrollador de pago único).
3. Identificador de la app: `com.camellofacil.app` (cámbialo en `capacitor.config.ts` **antes** de la primera publicación: después no se puede).
4. Play Store pide la política de privacidad (Ley 1581) con la URL pública del sitio.

### Límites conocidos

- **Necesita internet**: abre el sitio publicado; no funciona sin conexión.
- **Las ofertas de empleo** (enlaces a otros sitios) se abren en el navegador del teléfono, no dentro de la app (`allowNavigation` solo admite el sitio y el inicio de sesión de Microsoft).
- **iOS**: no está agregado. Requiere un Mac, Xcode y una cuenta de Apple Developer (`npm i @capacitor/ios && npx cap add ios`). Apple suele rechazar apps que solo envuelven un sitio web: habría que añadir funciones nativas.
