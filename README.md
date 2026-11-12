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
