# Reporte de integración — engine + creación de desafíos + mock de Motor

Análisis estático del árbol de trabajo actual (branch `DevBrizu` @ `cdb2d60`, working tree limpio).
No se modificó ningún archivo. No se pudo ejecutar `mvn compile`/`mvn test`: no hay Maven ni `mvnw`
instalado en este entorno — la sección 4 se basa en revisión manual de cada referencia cruzada
(imports, firmas de constructor, miembros de enum), no en una compilación real. Recomiendo correr
`mvn -q compile test` antes de confiar en que compila.

## 0. Qué se integró realmente (según el historial de git)

`git log --oneline --graph --all` muestra que el HEAD actual (`cdb2d60`, rama `feature/ide-integration`
== `DevBrizu`) es hijo directo de `3d864d1` (merge de `feature/creacion-desafios`, PR #1 + #2), y que
lo único que agrega por encima es **`frontend-monaco/`** completo (56 archivos, +17223 líneas, commit
"feat: the whole thing"). Es decir, lo que hoy está en el working tree son en realidad **dos cosas**,
no tres:

1. **Engine + creación de desafíos**, ya integrados entre sí *como código Java en el mismo módulo*
   `backend/` desde antes de `cdb2d60` (commits `a74622d Engine`, `5532a3f`, `47517af`, luego
   `0306a3a`/`15f2eda`/`18ac8bc` de creación de desafíos, mergeados en `761237e`/`92db838`/`3d864d1`).
2. **`frontend-monaco/`**, agregado entero en `cdb2d60`.

Hay además dos ramas remotas con trabajo relacionado que **no están mergeadas** en `DevBrizu`:
`feature/mock-motor` (`5c2bc61`, "added dockerfiles and mocked challenge engine") y
`feature/database` (`a9dfe27`, "Add JPA model and PostgreSQL integration"). Si el "mock de Motor" que
tenías en mente es el de esa rama, **no está en el working tree actual** — vale la pena confirmarlo
antes de seguir, porque cambia bastante el resto de este reporte.

## 1. Mapa general

```
backend/                     Spring Boot 3.3.4 / Java 21, único módulo Maven
  engine/, challenge/          → motor de evaluación (identity-free)
  practicalchallenge/,
  motorstub/, attemptdraft/    → creación de desafíos + intentos (JPA/H2)
  web/, config/, job/          → controllers puente, seguridad, identidad de Gateway, JWKS
frontend/                    Angular, habla con backend/ en localhost:8080
  engine.service.ts             → GET /challenges/{id}, POST /engine/evaluate
  challenge-create/,
  challenge-activity/,
  attempt-resolve/              → /api/desafiospracticos/**
frontend-monaco/             Angular 22 + servidor Node ESM propio (puerto 3100), STANDALONE
  server/index.mjs, executor.mjs, chat.mjs, integrity.mjs, store.mjs
```

- `backend/` es **un solo proceso**: el engine y la creación de desafíos comparten JVM, `pom.xml` y
  `application*.yml`, pero **no comparten modelo de dominio** (ver §2 y §3).
- `frontend/` es **una sola app Angular** que internamente tiene dos clientes HTTP que nunca se cruzan:
  `EngineService` (`frontend/src/app/engine.service.ts:8-18`) y `ChallengeService`
  (`frontend/src/app/challenge-create/challenge.service.ts:16-49`).
- `frontend-monaco/` es un prototipo aparte, con su propio backend Node (`npm run server`, puerto 3100,
  ver `frontend-monaco/README.md:9,44-56`), su propio store de desafíos (`server/store.mjs`, JSON en
  disco), su propio ejecutor de sandbox (`server/executor.mjs`) y su propio chat anti-leak
  (`server/chat.mjs`). **No hace ninguna llamada a `backend/`** — verificado: no hay ninguna referencia
  a puerto 8080/8086 ni a `/api/desafiospracticos` en `frontend-monaco/src/**` ni `server/**`; su
  `proxy.conf.json` apunta a `http://localhost:3100` (su propio servidor Node). El propio
  `frontend-monaco/README.md:3-7` se autodescribe como "Prototype of the Motor de Desafíos Prácticos
  (Grupo 5)" — es una reimplementación paralela completa, no un cliente de nada.

### Puntos de entrada (backend/, Java)

| Método | Ruta | Archivo | Expone |
|---|---|---|---|
| `POST` | `/engine/evaluate` | `web/EvaluationController.java:20` | Evalúa código contra `challengeId` (hardcodeado a "sample", ver §2) |
| `GET` | `/challenges/{id}` | `web/ChallengeController.java:20` | Devuelve el `Challenge` en memoria |
| `POST`/`GET` | `${app.api.private-path}/desafios[/{id}]` | `practicalchallenge/PracticalChallengeController.java:29-50` | Crea/lista/consulta desafíos reales (rol `ADMIN`/`PROFESOR`) |
| `POST` | `${app.api.private-path}/intentos` | `practicalchallenge/LocalAttemptCreationController.java:24-32` | Inicia un intento (solo perfil `local`) |
| `GET` | `${app.api.private-path}/intentos[/{id}]` | `practicalchallenge/AttemptQueryController.java:23-31` | Lista/consulta intentos |
| `PUT` | `${app.api.private-path}/intentos/{id}/borrador` | `practicalchallenge/LocalAttemptDraftController.java:31-40` | Guarda borrador de código (solo perfil `local`) |
| `GET` | `${app.api.public-path}/ping`, `.../quien-soy`, `.../interno` | `web/DesafiosController.java:25-39` | Diagnóstico de identidad/Gateway |

`ChallengeController` y `EvaluationController` **no** usan los placeholders `${app.api.*}` que
`DesafiosController.java:16-21` documenta como "REGLA DURA" — usan rutas literales `/challenges` y
`/engine`. Efecto práctico: en perfiles distintos de `local`, ambas rutas caen bajo el catch-all
`.anyRequest().authenticated()` de `SecurityConfig.java:44` **sin restricción de rol** — cualquier
principal autenticado (headers de Gateway) puede pegarle a `/engine/evaluate`.

## 2. El mock de Motor — lo más importante

**No existe, en el código actual, un mock que le mande al engine un payload con perfilId/challengeId
desde un componente que simule a Motor.** Hay dos cosas que podrían confundirse con eso, y ninguna
encaja con lo que describiste:

### a) `motorstub` (Java) — no llama a nadie, solo ecoa

`motorstub.MotorDesafioClient` / `StubMotorDesafioClient` (`motorstub/MotorDesafioClient.java:26-29`,
`motorstub/StubMotorDesafioClient.java:19-32`) **no es un cliente que le pega a nada**. Es, tal como
lo documenta el propio `CLAUDE.md` del repo, un puerto que persiste localmente (`title`, `difficulty`)
lo que llega *como dato de entrada* en el request de creación del profesor — nunca sale del proceso,
nunca toca el engine.

Payload real que recibe `POST /desafios` (que es lo que dispara `motorstub`), en
`practicalchallenge/PracticalChallengeRequest.java:10-27`:

```json
{
  "desafioId": "string (obligatorio, generado por Motor)",
  "title": "string",
  "statement": "string",
  "difficulty": "BASICO | MEDIO | AVANZADO",
  "type": "ALGORITMOS_CON_PRUEBAS_AUTOMATICAS",
  "language": "JAVA",
  "starterCode": "string | null",
  "testCases": [
    { "name": "string", "input": "string", "expectedOutput": "string", "visibility": "PUBLICO | PRIVADO" }
  ]
}
```

De ahí, `PracticalChallengeService.create()` (`practicalchallenge/PracticalChallengeService.java:38-42`)
llama a `motorDesafioClient.registrarDesafioRecibido(desafioId, title, difficulty)` **una sola vez, en
el mismo request, en la misma transacción** — no hay un segundo momento ("al evaluar") en que esto se
dispare. `desafioId`/`title`/`difficulty` **no** incluyen ni alumno ni curso. Tampoco viaja nada de esto
hacia `engine/`.

### b) `frontend-monaco` — tampoco llama al engine

Es una reimplementación completa e independiente (ver §1), con su propio modelo de "hidden tests" +
"expectedSolution" (`frontend-monaco/server/index.mjs:99-118` para el seed de ejemplo) y su propio
ejecutor (`server/executor.mjs`, vía esbuild/`javac`/Maven). Nunca hace un `fetch` hacia el backend Java.
No es un mock de Motor que le hable al engine — es un motor de evaluación alternativo, en otro stack,
que no comparte nada con `backend/engine`.

### El payload real que sí llega al engine

El único caller real de `POST /engine/evaluate` es `frontend/src/app/engine.service.ts:16-18`, con el
DTO `web/EvaluationRequest.java:3-9`:

```json
{ "submissionId": "string", "challengeId": "string", "lenguaje": "string", "code": "string", "profileId": "string | null" }
```

Ejemplo real tal como lo arma el frontend (`frontend/src/app/engine-demo.component.ts` /
`engine.service.ts:12-14`): `challengeId` siempre sale de `GET /challenges/sample`, es decir, **siempre
es `"sample"`** — nunca un `desafioId` real creado por un profesor.

Respondiendo tus preguntas puntuales:

- **¿Incluye perfilId?** Sí, `profileId` (`EvaluationRequest.java:8`), y el engine sí lo usa
  (`engine/EngineServiceImpl.java:73-74`) — pero contra `InMemoryProfileRepository`
  (`engine/profile/InMemoryProfileRepository.java:14-33`), que solo conoce dos ids hardcodeados
  (`"introductorio"`, `"avanzado"`). Esto **no tiene ninguna relación** con `ProfileEntity`/
  `ProfileJpaRepository` (JPA) del lado de creación de desafíos, cuyo único registro hoy es
  `"java-io-v1"` (`practicalchallenge/PracticalChallengeCatalog.java:11,29-37`). Son dos espacios de
  ids completamente distintos que comparten nombre ("perfil") por casualidad, no el mismo perfilId
  viajando Roadmap → Motor → engine.
- **¿Incluye challengeId?** Sí, pero solo resuelve contra `challenge.ChallengeRepository`
  (`challenge/InMemoryChallengeRepository.java`), que tiene **un único desafío hardcodeado** (`"sample"`,
  líneas 14-33). Cualquier otro id tira `IllegalArgumentException` (línea 40). Los desafíos reales
  creados vía `POST /desafios` viven en `PracticalChallengeJpaRepository` y **el engine no lo conoce**.
- **¿Incluye alumno/curso?** No, en ningún lado — ni en `EvaluationRequest`, ni en `motorstub`. El
  engine sigue siendo identity-free tal como está documentado (no encontré ninguna referencia a
  alumno/curso dentro de `engine/`; el único hit de "alumno" es el nombre de campo
  `EvaluationResult.feedbackAlumno`, que es texto de salida para mostrarle al estudiante, no un dato de
  entrada de identidad).
- **¿Manda tests/expected?** No desde el front — `EvaluationRequest` no tiene campo de tests; el engine
  los resuelve server-side desde `Challenge.tests()` (hardcodeado). La pantalla real de resolución de
  intento (`AttemptDetailResponse.java:13-23`) tampoco expone tests. El único lugar donde
  `expectedOutput` sale del servidor para **todos** los tests (incluidos `PRIVADO`) es
  `PracticalChallengeResponse.TestCaseResponse` (`practicalchallenge/PracticalChallengeResponse.java:15-22`,
  poblado en `PracticalChallengeService.toResponse`, líneas 96-118) vía `GET /desafios/{id}` — hoy
  gateado a rol `ADMIN`/`PROFESOR` (`PracticalChallengeController.java:20`), así que no hay leak a
  alumnos *hoy*, pero el DTO en sí no tiene ninguna protección propia si ese mismo response se reutiliza
  en una pantalla de alumno el día de mañana.
- **¿En qué momento llama?** `motorstub` solo al crear el desafío (nunca al evaluar). El engine solo se
  invoca manualmente (Postman/`engine-demo.component.ts`) contra el desafío `"sample"` — no hay ningún
  flujo real que dispare `EngineService.evaluate()` a partir de un `AttemptEntity`.

## 3. Creación de desafíos

Entidades (`practicalchallenge/`): `PracticalChallengeEntity` (tabla `practical_challenges`,
`PracticalChallengeEntity.java:18-120`) — 1:N `ChallengeFileEntity` (`challenge_files`, hoy siempre una
fila `path="Main.java"`) y 1:N `ChallengeVersionEntity` (`challenge_versions`), que a su vez referencia
`TestEntity` (`tests`, `input`/`expectedOutput`/`visibility`). Catálogo: `ChallengeTypeEntity` →
`ProfileEntity` → `LanguageEntity`, sembrado on-demand por `PracticalChallengeCatalog.java:26-47`.
Repos JPA solo para el agregado raíz (`PracticalChallengeJpaRepository`); files/tests/versions cuelgan
por cascade (`CascadeType.ALL`, `orphanRemoval=true`, `PracticalChallengeEntity.java:52-56`).

**¿Puede el engine resolver los tests de un desafío real a partir del `challengeId`, sin que se los
manden desde afuera?** No. No existe ningún adaptador que implemente `challenge.ChallengeRepository`
respaldado por `PracticalChallengeJpaRepository`. Falta una clase que, dado un `desafioId`, traiga
`PracticalChallengeEntity` + su `ChallengeVersionEntity` de versión vigente + sus `TestEntity`, y arme un
`challenge.Challenge`/`challenge.TestCase` (`input`/`expectedOutput` → `TestCase.input`/`expected`). Hoy
la única implementación de `ChallengeRepository` es la in-memory de un solo desafío
(`challenge/InMemoryChallengeRepository.java`).

**¿Hay versionado (snapshot inmutable) o el desafío es mutable?** El esquema tiene el campo `version`
en `ChallengeVersionEntity` y `PracticalChallengeService.currentTests()`
(`PracticalChallengeService.java:133-141`) ya sabe tomar la versión numéricamente más alta — pero
**no existe ningún endpoint de edición** (`PracticalChallengeController` solo tiene `POST`/`GET`, sin
`PUT`/`PATCH`), y `create()` siempre escribe todos los tests en `version = 1`
(`PracticalChallengeService.java:70-71`). En la práctica el desafío es **inmutable por ausencia de
feature**, no por un diseño deliberado de snapshot — el versionado está preparado en el modelo pero no
es alcanzable todavía (coincide con lo que dice `G05_IMPLEMENTACION_Y_PENDIENTES.md:198,510-521`).

## 4. Integración y conflictos del merge

- **Compilación**: no verificable en este entorno (sin Maven/`mvnw`). Revisión manual de todas las
  referencias cruzadas entre `engine/`, `challenge/`, `practicalchallenge/`, `motorstub/`, `web/` no
  encontró mismatches de tipos, constructores ni miembros de enum. Correr `mvn -q compile test` para
  confirmar antes de dar esto por bueno.
- **Configuración**: sin duplicación real. `SecurityConfig` es `@Profile("!standalone & !local")`
  (`config/SecurityConfig.java:27`) y tanto `application-local.yml:26-30` como
  `application-standalone.yml:10-14` excluyen explícitamente la autoconfig de Security — coherente, sin
  solapamiento. `application-local.properties` existe pero está **vacío** (posible archivo de
  scaffolding olvidado; no rompe nada porque `application-local.yml` ya cubre todo).
- **Puertos**: `application.yml:19` usa `8086` por default (gateway), `application-local.yml:3` y
  `application-standalone.yml:18` fuerzan `8080` — consistente con lo que espera
  `frontend/engine.service.ts:8` y `challenge.service.ts:16` (ambos hardcodean `localhost:8080`).
  `frontend-monaco` corre aparte en `3100`/`4200` (Angular dev server) — no compite por puerto con nada
  del lado Java salvo coincidencia si alguna vez se lo despliega en el mismo host.
- **Identity-free del engine**: no encontré violaciones. `EvaluationRequest`, `Challenge`,
  `EvaluationProfile` no tienen campos de alumno/curso, y `EngineServiceImpl` nunca toca
  `Authentication`/`SecurityContext`. Se mantiene el contrato documentado.
- **Duplicación a nivel de producto, no de merge git**: `frontend-monaco` reimplementa desde cero
  autoría de desafíos, ejecución de código y evaluación — en paralelo a `backend/engine` +
  `frontend/challenge-create`. Git no reporta conflicto porque vive en su propio directorio, pero hoy
  el repo tiene **dos backends y dos frontends** que resuelven el mismo problema sin contrato compartido
  ni entidad "fuente de verdad" declarada.

## 5. Gaps de contrato (engine ↔ Motor ↔ creación)

Riesgos de seguridad: no encontré secretos commiteados (`application-local.yml` usa H2 file-based sin
credenciales reales; `application-local.properties` vacío). El único hallazgo de exposición de datos es
el `expectedOutput` de tests `PRIVADO` en `PracticalChallengeResponse` (§2), mitigado hoy solo por
`@PreAuthorize` a nivel de controller, no a nivel de DTO.

## Qué falta / qué arreglar

### BLOQUEANTE

1. **No hay adaptador `PracticalChallengeJpaRepository` → `challenge.ChallengeRepository`.** El engine
   solo puede evaluar el desafío hardcodeado `"sample"`; ningún desafío creado por un profesor es
   evaluable hoy. (`challenge/InMemoryChallengeRepository.java`, `engine/EngineServiceImpl.java:45,62,75`)
2. **No existe ningún flujo que dispare `EngineService.evaluate()` a partir de un `AttemptEntity` real.**
   Los botones "Compilar"/"Entregar" de la pantalla de resolución están deshabilitados a propósito
   (confirmado en `G05_IMPLEMENTACION_Y_PENDIENTES.md:326`) — coherente con este gap, pero significa que
   el "merge de las tres ramas" todavía no produce un flujo end-to-end ejecutable.
3. **`profileId` no se pinnea en ningún lado cerca de la creación del intento**, y los dos conceptos de
   "perfil" (rúbrica del engine, in-memory: `"introductorio"/"avanzado"` vs. perfil técnico JPA:
   `"java-io-v1"`) no comparten espacio de ids. `AttemptEntity` (`practicalchallenge/AttemptEntity.java:20-103`)
   no tiene columna `profileId`. El "Roadmap → Motor → engine, pinneado al iniciar el Attempt" que
   describiste no tiene ninguna implementación todavía.
4. **`frontend-monaco` es una reimplementación paralela completa y desconectada** (propio backend Node,
   propio store, propio ejecutor, propia evaluación). Antes de dar este "merge" por cerrado hay que
   decidir explícitamente si es el prototipo que reemplaza a `frontend/` + partes de `backend/engine`
   (y entonces conectarlo) o si es un experimento descartable (y entonces sacarlo del branch principal)
   — hoy el repo tiene dos sistemas completos resolviendo lo mismo sin que nada indique cuál manda.

### MENOR

5. `ChallengeController` (`web/ChallengeController.java:11`) y `EvaluationController`
   (`web/EvaluationController.java:11`) usan rutas literales `/challenges` y `/engine` en vez de
   `${app.api.public-path}`/`${app.api.private-path}`, violando la "REGLA DURA" documentada en
   `DesafiosController.java:16-21`; como efecto colateral quedan sin restricción de rol fuera de
   `local`.
6. Colisión de nombres: "perfil"/`profileId` significa dos cosas no relacionadas (rúbrica de
   corrección vs. perfil técnico de sandbox/lenguaje). Conviene renombrar uno de los dos antes de
   integrar Motor de verdad, para no arrastrar la ambigüedad al contrato real.
7. `PracticalChallengeResponse.TestCaseResponse.expectedOutput` (`PracticalChallengeResponse.java:19`)
   expone la salida esperada de tests `PRIVADO` sin ninguna guarda a nivel de DTO — funciona hoy porque
   el único consumidor es un endpoint `ADMIN`/`PROFESOR`, pero es una trampa si se reutiliza el mismo
   response type para una pantalla de alumno.
8. No se pudo correr `mvn compile`/`mvn test` en este entorno (sin Maven instalado) — confirmar
   manualmente.
9. El versionado de `ChallengeVersionEntity` está en el esquema pero es inalcanzable (no hay endpoint de
   edición); si la intención es "snapshot inmutable" declararlo explícitamente, si es "todavía no
   implementado" el naming/documentación ya lo deja claro en `G05_IMPLEMENTACION_Y_PENDIENTES.md`.
10. `application-local.properties` está vacío y no se usa (todo vive en `application-local.yml`) — o se
    completa o se borra para no confundir sobre cuál es la fuente de config real del perfil `local`.
