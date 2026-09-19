# TestDesafiosPracticos

TP de Progra IV - Desafíos Prácticos.

El detalle completo de lo implementado, las decisiones temporales y las integraciones pendientes se encuentra en [G05_IMPLEMENTACION_Y_PENDIENTES.md](G05_IMPLEMENTACION_Y_PENDIENTES.md).

## Levantar todo con Docker Compose

```bash
docker compose up --build
```

Servicios:

- Frontend: `http://localhost:4200`
- Motor mock y selector de ejemplos: `http://localhost:8081`
- Tutor IA mock: `http://localhost:8082`
- Backend: `http://localhost:8080`
- H2 Console: `http://localhost:8080/h2-console`

El camino recomendado para probar el contrato es abrir Motor mock en
`http://localhost:8081`, elegir uno de sus tres desafíos y seguir el redirect
al frontend. El backend valida y recupera título/dificultad consultando Motor
por HTTP. Los datos de H2 se conservan en el volumen `backend-data`.

Al iniciar un intento, el frontend crea de forma idempotente una sesión de
tutor a través del backend; al abrir la resolución vuelve a recuperarla o
reintenta su creación. El backend consulta por HTTP al servicio `llm-mock` y
guarda el `sessionId` en `ATTEMPTS.llm_conversation_id`. El panel lateral del
tutor es plegable y permite enviar mensajes. El mock conserva el historial en
memoria y responde siempre que el servicio todavía está mockeado.

## Creación de desafíos G05

Esta primera versión permite guardar contenido práctico reutilizable, consultar su detalle y verlo en un listado. Guardar un desafío no lo publica ni lo asocia a un curso. El perfil local también permite iniciar y listar un registro mínimo de intento, pero no ejecuta código ni administra entregas, vidas, progreso, XP o monedas.

### Levantar en local

Requisitos para ejecución manual: Java 21, Maven, Node.js 24 y npm. No requiere PostgreSQL.

Primero levantar Motor mock, desde `motor-mock/`:

```bash
mvn spring-boot:run
```

Luego levantar el tutor IA mock, desde `llm-mock/`:

```bash
mvn spring-boot:run
```

Backend, desde `backend/`:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Frontend, desde `frontend/`:

```bash
npm install
npm start
```

Abrir `http://localhost:4200/desafios/nuevo`.

La página `http://localhost:4200/actividad` contiene dos pestañas: desafíos creados e intentos iniciados. Desde el listado se puede abrir el detalle de cada desafío.

El perfil `local` escucha únicamente en `127.0.0.1`, usa H2 en `backend/data/` y desactiva la autenticación para permitir esta prueba aislada. Este bypass no representa la autenticación final. Fuera de los perfiles `local` y `standalone`, crear, listar y consultar contenido requiere identidad validada por el Gateway y rol `ADMIN` o `PROFESOR`. La creación de intentos no se expone fuera de `local`.

### Ejemplo

- Título: `Sumar dos números`
- Consigna: `Leer dos números y mostrar su suma.`
- Dificultad: `BASICO`
- Test público: nombre `Suma simple`, entrada `2 3`, salida `5`
- Test privado: nombre `Suma de centenas`, entrada `100 250`, salida `350`

Al guardar, el frontend vuelve a consultar `GET /api/desafiospracticos/desafios/{id}` y muestra el detalle recuperado. Lo escrito se conserva si la operación falla. Luego se puede abrir `Actividad`, iniciar un intento local y comprobar su aparición en la segunda pestaña.

### Verificar H2

Con el backend levantado en perfil `local`, abrir `http://localhost:8080/h2-console` y usar:

- JDBC URL: `jdbc:h2:file:./data/desafios-practicos`
- Usuario: `sa`
- Contraseña: vacía

El sector implementado del diagrama contiene estas tablas:

- `LANGUAGES`
- `PROFILES`
- `CHALLENGE_TYPES`
- `PRACTICAL_CHALLENGES`
- `CHALLENGE_FILES`
- `TESTS`
- `CHALLENGE_VERSIONS`
- `ATTEMPTS`

`PRACTICAL_CHALLENGES.practical_challenge_id` es el mismo `desafioId` que genera Motor: ya no es nullable, es la propia clave primaria. `title` y `difficulty` no viven en esta tabla; se consultan por HTTP a Motor mediante el paquete `motor`. El código inicial vive en `CHALLENGE_FILES`, con forma de archivo (`path` + `content`). Hoy el profesor carga un único archivo con `path = "Main.java"`, pero el storage queda preparado para multiarchivo.

### Simplificación técnica

En esta versión los casos de prueba se modelan como pares de entrada/salida. Es una simplificación técnica temporal pendiente de acordar con Sandbox, no una exigencia del PRD.

Los intentos locales registran el inicio, pueden asociar una sesión mock del
tutor IA y guardan el borrador temporal en `ATTEMPT_DRAFTS`. No equivalen a
publicación ni entrega.

Los mensajes del tutor pasan siempre por Practical Challenges. Antes de llamar
al LLM, valida que el intento exista, pertenezca al usuario, siga activo, tenga
una sesión asociada y que el mensaje respete el contrato. Solo envía el
enunciado, código inicial y código visible; no envía tests privados.

## Verificación automatizada

```bash
cd backend
mvn test

cd ../motor-mock
mvn test

cd ../frontend
npm run build
```
