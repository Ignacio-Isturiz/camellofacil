---
name: camellofacil-desarrollo
description: Estándares de desarrollo para camellofacil.com (Spring Boot, Angular, PostgreSQL): arquitectura, buenas prácticas, patrones justificados, seguridad, amenazas y Ley 1581. Úsala al escribir, modificar o revisar código.
---

# Desarrollo de camellofacil.com

## Arquitectura (no negociable)
- **Estilo:** monolito modular en el backend y una aplicación Angular con vistas organizadas por función en el frontend. Dos personas trabajan en el proyecto, así que la organización debe permitir trabajar en módulos distintos sin pisarse.
- **Backend:** una sola aplicación Spring Boot dividida en módulos: auth, perfil, cv, plantillas, vacantes, recomendaciones, postulaciones, admin y legal. Cada módulo tiene tres capas:
  - web: recibe la petición HTTP, valida y responde con records. No tiene lógica de negocio.
  - app: servicios que aplican las reglas (límites diarios, bloqueo sin hoja de vida, RN-02).
  - persistence: repositorios y consultas.
- **Reglas de dependencia entre módulos:**
  - Un módulo no lee ni escribe las tablas de otro.
  - Se comunica con otro solo a través de su servicio público.
  - Las dependencias van en un solo sentido. No se permiten ciclos entre módulos.
  - Estas reglas se verifican con pruebas automáticas (ArchUnit), no solo con revisión manual.
- **Frontend:** una carpeta `features/` por módulo, con los mismos nombres del backend. Lo compartido vive en `core/` (sesión, interceptores, guardas) y `shared/` (componentes reutilizables). El panel de administrador se carga de forma diferida.
- **Sin estado local en el servidor:** el backend no guarda nada en memoria ni en el disco. Las sesiones van en Postgres (Spring Session JDBC), los archivos en R2 y la configuración en variables de entorno. Así la aplicación puede correr en Render hoy, en un servidor propio mañana, o en dos copias al mismo tiempo.
- **Comunicación frontend-backend:** solo por la API versionada en `/api/v1/`. Cambiar el interior de un lado no debe romper el otro mientras el contrato se mantenga.
- **Despliegue:** frontend en Cloudflare Pages en `camellofacil.com`; backend en un contenedor Docker en `api.camellofacil.com`. El contenedor es desechable: todo lo que necesita viene de la base de datos, de R2 y de las variables de entorno.
- **Tareas automáticas:** se registran en la tabla de tareas de Postgres. Nunca dependen de que el servidor esté despierto ni de una tarea programada en memoria.

## Antes de escribir código
1. Lee el requisito que implementa el cambio (RF-xx o RN-xx en el documento de requisitos del proyecto). Si no existe, pregunta antes de inventarlo.
2. Ubica el módulo responsable según la arquitectura de arriba.
3. Si el cambio toca datos personales, autenticación, archivos o IA, aplica las secciones de seguridad y privacidad con más rigor.
4. Si el cambio es grande o toca varios módulos, propone primero un plan breve: archivos afectados y qué cambia en cada uno.

## Principios de diseño
- KISS: la solución más simple que cumpla el requisito.
- YAGNI: no construyas para necesidades futuras sin requisito. Las etapas de escalamiento se activan con disparadores medidos, no por anticipación.
- DRY con criterio: repite una vez antes de abstraer; abstrae en el tercer caso real.
- Responsabilidad única: el controlador recibe y responde, el servicio aplica reglas, el repositorio accede a datos.
- Ley de Demeter: un módulo no navega por las entidades internas de otro; usa su servicio público.
- Inversión de dependencias: depende de interfaces para IA, correo, archivos y fuentes de vacantes.
- Principio de menor sorpresa: nombres en español consistentes con el dominio (Vacante, HojaDeVida, Postulacion).
- Ley de Hyrum: todo comportamiento visible acabará siendo usado; los cambios de API se versionan.
- Entradas estrictas: valida todo lo que entra al backend. Nunca confíes solo en el frontend.

## Patrones: solo cuando hay dolor
Antes de aplicar un patrón, di en una frase qué problema resuelve y qué costo agrega. Si no puedes, no lo apliques.

| Problema | Patrón candidato | Señal para usarlo |
|---|---|---|
| Proveedor externo (IA, correo, archivos) | Adaptador detrás de una interfaz | Siempre, desde el inicio |
| Criterios ATS distintos por opción | Estrategia | Dos o más criterios con lógica diferente |
| Transiciones de estado de postulación | Máquina de estados, o enum con validación | Las reglas de transición crecen |
| Notificaciones por varios canales | Observador o evento interno | Cuando entren las notificaciones |
| Objetos con muchos parámetros opcionales | Builder | Constructores con demasiados argumentos |
| Operaciones que deben reintentarse | Comando guardado en la tabla de tareas | Procesos automáticos |

Ya incluidos en Spring y Angular: inyección de dependencias, repositorios y servicios.

Evita: singletons manuales, herencias profundas, clases llamadas Manager, Helper o Util sin responsabilidad clara, y patrones decorativos.

## Backend (Spring Boot 4, Java 21)
- Usa record para los DTO de entrada y salida. Nunca expongas entidades JPA en la API.
- Inyección por constructor, no @Autowired en campos.
- @Transactional en la capa de servicio. Los contadores de límites (adaptaciones diarias, consultas mensuales) usan bloqueo pesimista (SELECT FOR UPDATE).
- Identificadores UUID. Cambios de base solo con migraciones Flyway, nunca a mano.
- API versionada en /api/v1/. Errores con un único formato (ProblemDetail de Spring).
- Configuración por variables de entorno. Ningún secreto en el código ni en application.yml versionado.
- Logs sin datos personales (nombre, correo, documento, contenido de hojas de vida). Usa IDs.
- Pruebas: unitarias en servicios; integración con base real (Testcontainers) en los flujos críticos; reglas de arquitectura con ArchUnit.

## Frontend (Angular 22)
- Componentes standalone, signals para estado local, Reactive Forms tipados.
- Carga diferida por función, sobre todo el panel de administrador.
- Cliente de API generado desde OpenAPI. No escribas llamadas HTTP duplicadas a mano.
- Guardas de ruta para sesión y rol. La autorización real se valida siempre en el backend.
- No guardes datos personales en localStorage ni sessionStorage.
- Angular escapa el HTML por defecto. Evita bypassSecurityTrust salvo casos justificados y revisados.
- Textos en español e inglés con Transloco.

## Seguridad (OWASP Top 10 aplicado)
Revisa cada cambio contra estas amenazas:
- Control de acceso: cada endpoint verifica que el recurso pertenece al usuario autenticado, para evitar ver hojas de vida ajenas (IDOR). El rol de administrador se valida explícitamente.
- Inyección: consultas parametrizadas siempre. Nunca concatenes texto de usuario en SQL.
- Autenticación: BCrypt; límite de intentos de inicio de sesión; tokens de verificación y recuperación de un solo uso con vencimiento; mensajes de error genéricos.
- CSRF y CORS: CORS solo para el dominio del frontend; protección CSRF activa.
- Archivos: validar el tipo real del contenido (no solo la extensión), tamaño máximo, nombre generado por el sistema, y acceso mediante URL firmada de corta duración.
- Contenido no confiable: el texto de una hoja de vida o de una vacante puede contener instrucciones maliciosas dirigidas a la IA (inyección de prompt). La respuesta de la IA es solo datos estructurados que se validan antes de guardarse.
- Adaptación de la hoja de vida (RN-03): que no se inventen datos se verifica en el código comparando contra la hoja base, no solo con instrucciones al modelo.
- SSRF: no hagas peticiones a URLs que vengan del usuario o de fuentes sin una lista blanca de dominios.
- Límites de uso: rate limiting en inicio de sesión, registro, recuperación y postulación.
- Dependencias: actualizadas, con alertas de vulnerabilidades activas (Dependabot u OWASP Dependency-Check).
- Errores: nunca se exponen trazas de error ni detalles de la base de datos al usuario.
- Secretos: nunca en el repositorio. Si se filtra uno, se rota de inmediato.

## Privacidad (Ley 1581 de 2012)
- Todo dato personal tiene una finalidad clara y un consentimiento registrado con versión y fecha (RF-92, RF-93).
- Minimiza: no pidas ni guardes lo que no usas.
- Al eliminar una cuenta se borran todos los datos, incluidos los archivos en R2 (RF-11, RF-95).
- Los datos solo los ven su dueño o el administrador (RF-97). Las acciones sensibles quedan en auditoría, sin datos personales en el texto.
- No uses datos reales de estudiantes en pruebas ni en datos de ejemplo. Usa datos ficticios.
- Si el cambio agrega un proveedor o modifica dónde se guardan datos, márcalo para actualizar la política de privacidad.

## Análisis de amenazas rápido
Antes de una funcionalidad nueva, responde en cuatro líneas: qué activo protege, quién podría atacarlo, por dónde entraría, y qué control lo impide. Si no hay control, la tarea no está terminada.

## Revisión antes de entregar
- El proyecto compila y pasan las pruebas, incluidas las de reglas de arquitectura.
- Los cambios respetan los límites entre módulos y no introducen estado local en el servidor.
- No hay secretos, datos personales ni registros sensibles en el código ni en los logs.
- Las validaciones existen en el backend, no solo en el frontend.
- Los mensajes de error para el usuario están en español claro.
- Si cambió la API, la versión y la documentación están actualizadas.

## Formato de respuesta
Primero el código. Luego una explicación breve con tres puntos: la decisión tomada, la alternativa descartada y por qué, y el riesgo residual. Si el requisito es ambiguo, pregunta antes de implementar.