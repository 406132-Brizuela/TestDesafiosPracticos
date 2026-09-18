# G05: implementacion actual y pendientes de integracion

Este documento describe el estado real del microservicio de Desafios Practicos despues de implementar el primer flujo funcional de G05. El servicio permite crear, consultar y listar contenido practico con casos de prueba versionados. En el perfil local tambien permite iniciar y listar intentos minimos.

La implementacion actual no publica desafios, no ejecuta codigo y no registra entregas ni correcciones. Esas capacidades requieren contratos con otros microservicios y se detallan al final del documento.

## 1. Resumen ejecutivo

| Area | Estado actual |
|---|---|
| Creacion de contenido practico | Implementada |
| Persistencia de desafios y tests | Implementada en H2 |
| Version inicial de los tests | Implementada como version `1` |
| Listado y detalle de desafios | Implementados |
| Inicio y listado de intentos | Implementados solo como prueba local |
| Pantalla de resolución y guardado de borrador del código | Implementado en perfil local, sobre almacenamiento temporal (`ATTEMPT_DRAFTS`) |
| Identidad y roles | Preparados para identidad validada por Gateway fuera de local |
| Publicacion en el Motor | Pendiente |
| Asociacion a cursos | Pendiente |
| Ejecucion de codigo en Sandbox | Pendiente |
| Entregas, correccion y puntaje | Pendientes |
| Progreso, vidas, XP y monedas | Fuera del alcance actual |

## 2. Alcance implementado

Se implemento el sector acordado del modelo de G05:

- `LANGUAGES`
- `PROFILES`
- `CHALLENGE_TYPES`
- `PRACTICAL_CHALLENGES`
- `CHALLENGE_FILES`
- `TESTS`
- `CHALLENGE_VERSIONS`
- `ATTEMPTS`

El flujo principal permite que un profesor o administrador:

1. Complete titulo, consigna, dificultad y codigo inicial.
2. Agregue uno o mas casos de prueba.
3. Defina nombre, entrada, salida esperada y visibilidad de cada caso.
4. Guarde el desafio y todos sus tests en una unica transaccion.
5. Consulte el detalle recuperado desde el backend.
6. Vea el desafio dentro del listado de actividad.

En local tambien se puede iniciar un intento sobre un desafio. Ese intento solo registra su identificador, desafio, fecha de inicio y un documento de codigo inicialmente vacio.

## 3. Decisiones de alcance

### Contenido no equivale a publicacion

Guardar un `PRACTICAL_CHALLENGE` crea contenido reutilizable dentro de G05. No crea automaticamente una actividad visible para estudiantes, no lo incorpora a un curso y no lo publica en el Motor.

`PRACTICAL_CHALLENGES.practical_challenge_id` es directamente el `desafioId` que genera Motor (Tema 03): G05 nunca genera un id propio. Motor crea su registro y redirige al profesor a G05 pasando `desafioId`. El backend usa ese id para consultar por HTTP los metadatos de solo lectura (`title`/`difficulty`); esa consulta no crea ni modifica nada en Motor. En desarrollo, `HttpMotorDesafioClient` apunta al microservicio `motor-mock`, que expone un catalogo fijo de ejemplos.

### Intento no equivale a entrega

Un registro en `ATTEMPTS` representa un inicio minimo. Actualmente:

- `attempt_id` es el `intentoId` recibido como dato de entrada (mismo patron que `desafioId`), no generado por G05.
- `repo_url` y `ref` quedan en `null` (todavia no existe la integracion real con GitHub).
- `submission_datetime` queda en `null`.
- La API lo informa con estado `INICIADO`.
- No se envia codigo a ejecutar.
- No se calculan resultados, puntajes ni progreso.

El estado `ENTREGADO` solo puede inferirse si en el futuro se completa `submission_datetime`; el flujo para hacerlo todavia no existe.

### Tests simplificados

Los casos de prueba se guardan como pares de texto:

- Entrada.
- Salida esperada.
- Visibilidad publica o privada.

Esta representacion permite probar el flujo completo de creacion y persistencia, pero no pretende definir el contrato final con Sandbox. Todavia falta acordar formatos, comparacion de salidas, limites y resultados de ejecucion.

## 4. Modelo de datos

### `LANGUAGES`

Catalogo de lenguajes soportados.

| Campo principal | Uso actual |
|---|---|
| `language_id` | Identificador estable del lenguaje |
| `name` | Nombre unico |

El catalogo actual crea `java` con nombre `JAVA` cuando se guarda el primer desafio.

### `PROFILES`

Representa un perfil de ejecucion asociado a un lenguaje.

| Campo principal | Uso actual |
|---|---|
| `profile_id` | Identificador del perfil |
| `name` | Nombre del perfil |
| `sandbox_image_id` | Reservado para la imagen definida por Sandbox |
| `language_id` | Lenguaje asociado |
| `version` | Version del perfil |
| `is_active` | Disponibilidad del perfil |

El perfil inicial es `java-io-v1`. `sandbox_image_id` queda vacio porque todavia no existe integracion con Sandbox.

### `CHALLENGE_TYPES`

Asocia el tipo de desafio con un perfil de ejecucion.

| Campo principal | Uso actual |
|---|---|
| `challenge_type_id` | Identificador del tipo |
| `profile_id` | Perfil tecnico asociado |
| `modification_datetime` | Fecha de modificacion del catalogo |
| `version` | Version del tipo |
| `is_active` | Disponibilidad |
| `allow_multiple` | Indica si admite multiples instancias o respuestas |

El unico tipo habilitado actualmente es `algorithms-auto-tests-v1`, expuesto en la API como `ALGORITMOS_CON_PRUEBAS_AUTOMATICAS`.

### `PRACTICAL_CHALLENGES`

Contiene la definicion principal del contenido practico.

| Campo principal | Uso actual |
|---|---|
| `practical_challenge_id` | `desafioId` recibido desde el redirect de Motor; no es un UUID propio de G05 |
| `challenge_type_id` | Tipo y perfil tecnico |
| `statement` | Consigna |
| `user_creator_id` | Identidad recibida desde seguridad, si existe |
| `creation_datetime` | Fecha de creacion |

`title` y `difficulty` son de Motor, no de G05: no se duplican en ninguna tabla local. Se consultan por HTTP para formulario, listado y detalle. El codigo inicial vive en `CHALLENGE_FILES`.

### `CHALLENGE_FILES`

Guarda el codigo inicial del desafio con forma de archivo, en vez de como un `String` suelto.

| Campo principal | Uso actual |
|---|---|
| `challenge_file_id` | UUID del archivo |
| `practical_challenge_id` | Desafio al que pertenece |
| `path` | Ruta del archivo dentro del desafio |
| `content` | Contenido del archivo |
| `file_order` | Posicion estable del archivo (mismo patron que `test_order` en `CHALLENGE_VERSIONS`) |

Hoy el profesor carga un unico archivo desde un textarea (no hay UI de multi-archivo), asi que al crear un desafio se genera una unica fila con `path = "Main.java"` y `file_order = 0` — mismo nombre fijo que ya asume `LocalSandboxClient` al escribir el archivo que compila. `PracticalChallengeRequest` y `PracticalChallengeResponse` no cambiaron: `starterCode` se sigue exponiendo como `String` plano hacia afuera; el archivo es un detalle de almacenamiento interno.

Esta tabla no tiene concepto de version de contenido (a diferencia de `CHALLENGE_VERSIONS`, que versiona que tests aplican a cada version): hoy no existe ningun versionado del codigo inicial en el sistema y esta tabla no lo agrega.

Preparacion para trabajo futuro fuera de este sprint: cuando se implemente subir el contenido del desafio a un repo de GitHub, ese trabajo puede iterar sobre `CHALLENGE_FILES` (una lista de archivos con `path`) y comitearlos, sin tener que rediseñar el modelo de contenido en ese momento.

### Metadatos de Motor

No existe una tabla local para los metadatos de Motor. El paquete `motor`
define el puerto `MotorDesafioClient`, el adaptador HTTP y el resolver que
combina los datos remotos con las entidades de G05.

### `TESTS`

Guarda cada caso de prueba de forma independiente.

| Campo principal | Uso actual |
|---|---|
| `test_id` | Identificador autogenerado |
| `code` | Reservado; actualmente no se utiliza |
| `is_active` | Estado del test |
| `user_creator_id` | Creador, cuando hay identidad autenticada |
| `creation_datetime` | Fecha de creacion |
| `name` | Nombre descriptivo |
| `input` | Entrada textual |
| `expected_output` | Salida textual esperada |
| `visibility` | `PUBLICO` o `PRIVADO` |

`name`, `input`, `expected_output` y `visibility` son extensiones incorporadas para representar los casos del formulario actual.

### `CHALLENGE_VERSIONS`

Relaciona un desafio con los tests que pertenecen a una version.

| Campo principal | Uso actual |
|---|---|
| `challenge_version_id` | UUID de la relacion versionada |
| `practical_challenge_id` | Desafio relacionado |
| `test_id` | Test relacionado |
| `version` | Numero de version |
| `test_order` | Posicion estable del test dentro de la version |

Al crear un desafio se genera una fila por cada test, todas con version `1`. Al consultar el detalle se toma la version numericamente mas alta y se ordenan sus tests por `test_order`.

Todavia no existe un endpoint para editar un desafio ni para crear una version posterior.

### `ATTEMPTS`

Registra el inicio minimo de una resolucion.

| Campo principal | Uso actual |
|---|---|
| `attempt_id` | `intentoId` recibido como dato de entrada (Motor), no generado por nosotros — mismo patron que `desafioId` |
| `practical_challenge_id` | Contenido que se intenta resolver |
| `repo_url` | Referencia al repo evaluado (DT-08); nullable, hoy siempre `null` |
| `ref` | Referencia al commit/rama evaluado (DT-08); nullable, hoy siempre `null` |
| `creation_datetime` | Fecha de inicio |
| `submission_datetime` | Fecha futura de entrega |
| `llm_conversation_id` | Referencia futura a una conversacion asistida |
| `user_id` | Usuario que inicio el intento |

`user_id` se agrego para poder filtrar actividad por usuario cuando la aplicacion recibe identidad del Gateway.

### `ATTEMPT_DRAFTS`

Tabla temporal del paquete `attemptdraft`. No forma parte del modelo real del intento: `AttemptEntity.repoUrl`/`ref` (DT-08) siguen siendo el destino final del codigo del alumno una vez exista integracion con GitHub y no se tocan aca. Es un paquete aparte, explicitamente temporal, que se borra entero el dia que el codigo del alumno viva en Git.

| Campo principal | Uso actual |
|---|---|
| `attempt_id` | Mismo id que `ATTEMPTS.attempt_id`, sin generar nada propio |
| `content` | Ultimo codigo guardado por el alumno para ese intento |
| `updated_at` | Fecha del ultimo guardado |

Se elimina junto con el resto del paquete `attemptdraft` cuando el codigo del alumno pase a vivir en Git.

## 5. Flujo transaccional de creacion

La operacion `POST /api/desafiospracticos/desafios` realiza estos pasos:

1. Valida el request, incluido `desafioId` (llegado por el redirect de Motor).
2. Consulta `MotorDesafioClient.findById(desafioId)` por HTTP para validar el id y recuperar `title`/`difficulty` desde su fuente de verdad.
3. Obtiene o crea el catalogo minimo de Java, perfil Java I/O y tipo de algoritmo.
4. Persiste en `PRACTICAL_CHALLENGES` la consigna, creador y fecha, usando `desafioId` como `practical_challenge_id`.
5. Persiste el codigo inicial como una fila en `CHALLENGE_FILES` (`path = "Main.java"`, `file_order = 0`).
6. Crea una entidad `TESTS` por cada caso recibido.
7. Crea una relacion `CHALLENGE_VERSIONS` version `1` por cada test.
8. Conserva el orden original mediante `test_order`.
9. Publica el evento `ContenidoPracticoPersistido` via `DesafioContenidoEventPublisher` (hoy solo lo loguea `LoggingDesafioContenidoEventPublisher`; Kafka lo define Tema 11).
10. Devuelve `201 Created`, el recurso persistido (con `title`/`difficulty` resueltos por HTTP) y el header `Location`.

La operacion es transaccional: si falla cualquier parte, no debe quedar un desafio parcialmente creado.

## 6. API disponible

La ruta privada base es `/api/desafiospracticos`.

| Metodo | Ruta | Comportamiento |
|---|---|---|
| `POST` | `/desafios` | Crea desafio, tests y version inicial |
| `GET` | `/desafios` | Lista desafios del creador; en local lista todos |
| `GET` | `/desafios/{id}` | Recupera el detalle y los tests de la version actual |
| `POST` | `/intentos` | Inicia un intento; endpoint disponible solo en perfil `local` |
| `GET` | `/intentos` | Lista intentos del usuario; en local lista todos |
| `GET` | `/intentos/{id}` | Recupera el detalle de un intento (consigna, codigo inicial y borrador si existe) |
| `PUT` | `/intentos/{id}/borrador` | Guarda (upsert) el borrador de codigo del intento; endpoint disponible solo en perfil `local` |

### Request de creacion

```json
{
  "title": "Sumar dos numeros",
  "statement": "Leer dos numeros y mostrar su suma.",
  "difficulty": "BASICO",
  "type": "ALGORITMOS_CON_PRUEBAS_AUTOMATICAS",
  "language": "JAVA",
  "starterCode": "public class Main { }",
  "testCases": [
    {
      "name": "Suma simple",
      "input": "2 3",
      "expectedOutput": "5",
      "visibility": "PUBLICO"
    }
  ]
}
```

### Validaciones actuales

- Titulo obligatorio.
- Consigna obligatoria.
- Dificultad obligatoria.
- Tipo obligatorio.
- Lenguaje obligatorio.
- Al menos un caso de prueba.
- Nombre de cada caso obligatorio.
- Entrada y salida esperada presentes.
- Visibilidad obligatoria.
- El intento solo puede iniciarse sobre un desafio existente.

## 7. Frontend Angular

| Ruta | Funcion |
|---|---|
| `/` | Conserva la demostracion previa del Motor |
| `/desafios/nuevo` | Formulario de creacion de desafios |
| `/actividad` | Pestañas de desafios creados e intentos |
| `/desafios/:id` | Detalle de un desafio y sus tests |
| `/intentos/:id` | Pantalla de resolucion: consigna en solo lectura, editor Monaco y guardado de borrador |

### Formulario de creacion

- Permite agregar y quitar casos de prueba dinamicamente.
- Envia el formulario al backend.
- Recupera el recurso guardado para mostrar la confirmacion real.
- Conserva lo escrito si ocurre un error.
- Ofrece acceso posterior a la pagina de actividad.

### Pagina de actividad

- Carga desafios e intentos en paralelo.
- Permite abrir el detalle de cada desafio.
- Permite iniciar un intento cuando el backend esta ejecutandose en local.
- Cambia automaticamente a la pestaña de intentos despues de crear uno.
- Muestra estados de carga, confirmacion y error.
- Cada intento ofrece un link "Continuar resolviendo" hacia `/intentos/:id`.

### Pantalla de resolucion del intento

- Carga el intento por `GET /intentos/:id` y muestra consigna en solo lectura.
- Inicializa el editor Monaco (`app-monaco-editor`, reutilizado sin cambios) con `draftCode ?? starterCode`.
- Boton "Guardar borrador": funcional, llama `PUT /intentos/:id/borrador` y muestra confirmacion con hora de guardado.
- Botones "Compilar" y "Entregar": presentes pero deshabilitados, con nota indicando que requieren Sandbox e integracion con Git respectivamente — comunican la forma real del flujo sin fingir una capacidad que todavia no existe.

El frontend usa URLs relativas. Nginx las enruta al backend dentro de Docker Compose.

## 8. Seguridad y perfiles

### Perfil `local`

- Escucha solamente en `127.0.0.1:8080`.
- Usa H2 persistente en `backend/data/`.
- Habilita `/h2-console`.
- Desactiva seguridad para probar el microservicio de forma aislada.
- Desactiva Eureka y discovery.
- Permite crear intentos minimos.
- Lista todos los desafios e intentos porque no hay principal autenticado.

Este bypass solo es aceptable para desarrollo local y no debe utilizarse como configuracion de despliegue.

### Fuera de `local` y `standalone`

- La aplicacion es stateless.
- No usa login por formulario, Basic Auth ni sesiones.
- Confia en headers de identidad previamente validados por el Gateway.
- Crear, listar y consultar desafios requiere rol `ADMIN` o `PROFESOR`.
- El listado de desafios se filtra por `user_creator_id`.
- El listado de intentos se filtra por `user_id`.
- La creacion de intentos no se expone todavia.
- Las respuestas de autenticacion y autorizacion usan `application/problem+json`.

## 9. Persistencia local

La configuracion local usa:

```text
jdbc:h2:file:./data/desafios-practicos
```

Con el backend levantado desde `backend/`, la consola esta disponible en `http://localhost:8080/h2-console` con usuario `sa` y contrasena vacia.

Hibernate usa `ddl-auto: update`. Esto es suficiente para el prototipo local, pero no reemplaza migraciones versionadas para un entorno compartido o productivo.

## 10. Verificacion realizada

Se comprobaron los siguientes puntos:

- Creacion transaccional del agregado completo.
- Relaciones entre las ocho tablas implementadas.
- Orden y version de los tests.
- Listado y detalle de desafios.
- Filtrado por creador y usuario autenticado.
- Inicio de intento solo en perfil local.
- Rechazo de desafio inexistente.
- Restricciones de seguridad y roles.
- Persistencia de H2 despues de reiniciar el backend.
- Build de produccion del frontend.

Resultados de la ultima verificacion:

- Backend: 21 tests aprobados con `mvn test`.
- Frontend: build aprobado con `npm run build`.
- Smoke test: desafio, dos tests y un intento recuperados despues de reiniciar Spring Boot.

## 11. Funcionalidades pendientes por integracion

### Motor de desafios y publicacion

Resuelto para desarrollo: `desafioId` llega desde el redirect de Motor y `PRACTICAL_CHALLENGES.practical_challenge_id` lo usa directamente. Los metadatos se consultan mediante `HttpMotorDesafioClient`; Docker Compose conecta el backend con `motor-mock`.

Falta definir e implementar:

- Reemplazar la URL de `motor-mock` por la del Motor real y acordar autenticacion/contrato definitivo.
- Estados de borrador, publicado, despublicado y archivado.
- Que metadatos son propiedad del Motor y cuales de G05.
- Edicion y nuevas versiones despues de publicar.
- Reglas para impedir cambios incompatibles sobre contenido en uso.
- Idempotencia y compensacion si un alta distribuida falla a mitad de camino.

G05 no deberia asumir que crear contenido significa publicarlo. La publicacion debe ser una operacion explicita y coordinada.

### Gateway y servicio de usuarios

Falta validar el contrato definitivo para:

- Headers confiables de identidad y roles.
- Identificador canonico del usuario.
- Representacion de roles `ADMIN` y `PROFESOR`.
- Permisos para ver, editar, clonar o archivar contenido de otros autores.
- Tratamiento de usuarios eliminados o deshabilitados.
- Propagacion de identidad entre llamadas de microservicios.

Los campos `user_creator_id` y `user_id` almacenan identificadores externos. No deben convertirse en claves foraneas locales a una tabla de usuarios de G05.

### Cursos y trayectos

Falta implementar fuera de G05 la asociacion del contenido con:

- Curso, modulo, unidad o clase.
- Orden de aparicion.
- Fechas de disponibilidad.
- Requisitos previos.
- Audiencia o comision.
- Reintentos permitidos y reglas pedagogicas.

Antes de integrar debe decidirse si Cursos referencia el `challenge_id` global del Motor o el `practical_challenge_id` interno de G05. La opcion global evita acoplar Cursos a un subtipo concreto, pero el contrato debe confirmarse.

### Sandbox

La ejecucion real necesita un contrato que incluya:

- Lenguaje, version y perfil de runtime.
- `sandbox_image_id` valido.
- Codigo fuente y punto de entrada.
- Formato de entrada de cada test.
- Reglas de comparacion de salida.
- Limites de CPU, memoria, tiempo y tamano.
- Separacion entre tests publicos y privados.
- Resultado de compilacion y ejecucion por test.
- Errores normalizados y salida truncada.
- Correlacion, timeout, reintentos e idempotencia.

Cuando se acuerde este contrato puede ser necesario reemplazar o ampliar los campos `input`, `expected_output` y `code`. No conviene consolidar el formato actual como contrato definitivo antes de esa integracion.

### Entregas y correccion

Resuelto: el momento en que se asigna el id del intento ya no es un pendiente — `intentoId` llega como campo obligatorio del request, mismo patron que `desafioId` (ver §6 de `contrato-con-tema-03.md`, confirmado con Tema 03 el 17-sep-2026). `ATTEMPTS.attempt_id` lo usa directo, sin generarlo. `alumnoId` no se agrega como campo: la identidad de quien hace la request la resuelve `userId` (Gateway), no un dato del payload de Motor.

Resuelto via almacenamiento temporal: guardar el codigo mientras el alumno resuelve ya no es un pendiente — el paquete `attemptdraft` (temporal, ver `ATTEMPT_DRAFTS` en §4) guarda el ultimo borrador con upsert en `PUT /intentos/{id}/borrador`, editable desde `/intentos/:id`. Sigue siendo un solo archivo por intento (mismo criterio que `CHALLENGE_FILES` hoy) y no reemplaza el modelo real: `AttemptEntity.repoUrl`/`ref` no se tocan.

Falta desarrollar el resto del ciclo de vida completo del intento:

- Poblado real de `repo_url`/`ref` (hoy `null`) cuando exista la integracion con GitHub.
- Envio definitivo y asignacion de `submission_datetime`.
- Estados intermedios: pendiente, ejecutando, aceptado, rechazado y error tecnico.
- Resultado por caso de prueba.
- Puntaje, feedback y evidencia de ejecucion.
- Distincion entre error del estudiante y falla de infraestructura.
- Reentrega, limite de intentos e historial.
- Proteccion frente a envios duplicados.

La creacion de intentos se restringio a local para no publicar una API incompleta antes de definir estas reglas.

### Asistencia con LLM

`ATTEMPTS.llm_conversation_id` esta reservado, pero no se implemento:

- Creacion de conversaciones.
- Asociacion con un proveedor o servicio de IA.
- Historial de mensajes.
- Privacidad y retencion.
- Limites de uso.
- Reglas para evitar revelar soluciones o tests privados.

### Progreso y gamificacion

No forman parte de esta implementacion:

- Progreso del estudiante.
- Aprobacion del desafio.
- Vidas o energia.
- XP.
- Monedas.
- Rachas, logros o rankings.

Estos servicios deberian reaccionar a eventos de negocio confirmados, por ejemplo una entrega aceptada, en lugar de leer directamente las tablas internas de G05.

## 12. Trabajo tecnico antes de un entorno integrado

Antes de desplegar con otros microservicios se recomienda completar esta lista:

- [ ] Acordar ownership de datos y contratos entre Motor, G05, Cursos y Sandbox.
- [ ] Externalizar la URL del backend en Angular.
- [ ] Confirmar headers y roles enviados por Gateway.
- [ ] Definir el endpoint no local para crear intentos.
- [ ] Definir estados y transiciones de intentos y entregas.
- [ ] Diseñar el contrato de ejecucion con Sandbox.
- [ ] Definir eventos de dominio para publicacion, entrega y correccion.
- [ ] Agregar idempotencia y trazabilidad distribuida.
- [ ] Incorporar migraciones de esquema versionadas.
- [ ] Elegir y configurar la base del entorno desplegado.
- [ ] Revisar CORS y rutas a traves del Gateway.
- [ ] Agregar pruebas de contrato e integracion entre servicios.
- [ ] Definir observabilidad, metricas y alertas.
- [ ] Establecer reglas de retencion para codigo, resultados y conversaciones.

## 13. Fuera de alcance de esta entrega

Para evitar confundir prototipo con producto terminado, esta entrega no incluye:

- Publicacion de desafios.
- Asignacion a cursos.
- Ejecucion o compilacion de codigo.
- Correccion automatica real.
- Entrega formal de una solucion del estudiante (el guardado de borrador temporal si esta implementado, ver §4 `ATTEMPT_DRAFTS`).
- Administracion completa de perfiles y lenguajes.
- Edicion, borrado, archivado o clonado de desafios.
- Versiones posteriores a la version inicial.
- Estadisticas o reportes.
- Progreso y gamificacion.
- Integracion efectiva con LLM.

## 14. Archivos relevantes

| Ruta | Responsabilidad |
|---|---|
| `backend/src/main/java/com/tp/desafiospracticos/practicalchallenge/` | Entidades, repositorios, DTO, servicios y controladores de G05 |
| `backend/src/main/java/com/tp/desafiospracticos/motor/` | Puerto y adaptador HTTP de lectura hacia Motor |
| `motor-mock/` | Microservicio de desarrollo con catalogo fijo y redirect simulado |
| `backend/src/main/java/com/tp/desafiospracticos/attemptdraft/` | `AttemptDraftEntity`/`AttemptDraftJpaRepository`, almacenamiento temporal del borrador, a eliminar cuando el codigo del alumno viva en Git |
| `backend/src/main/java/com/tp/desafiospracticos/config/SecurityConfig.java` | Seguridad para entornos integrados |
| `backend/src/main/resources/application-local.yml` | H2, consola y aislamiento local |
| `backend/src/test/java/com/tp/desafiospracticos/practicalchallenge/` | Pruebas del flujo implementado |
| `frontend/src/app/challenge-create/` | Formulario y cliente HTTP |
| `frontend/src/app/challenge-activity/` | Listado de desafios e intentos |
| `frontend/src/app/challenge-detail/` | Detalle del desafio |
| `frontend/src/app/attempt-resolve/` | Pantalla de resolucion del intento (editor y guardado de borrador) |
| `frontend/src/app/app.routes.ts` | Rutas Angular |

## 15. Ejecucion local

Backend, desde `backend/`:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Frontend, desde `frontend/`:

```bash
npm install
npm start
```

Luego se puede abrir:

- `http://localhost:4200/desafios/nuevo`
- `http://localhost:4200/actividad`
- `http://localhost:8080/h2-console`

La documentacion operativa resumida permanece en [`README.md`](README.md).
