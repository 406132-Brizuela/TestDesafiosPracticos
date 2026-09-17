# TestDesafiosPracticos

TP de Progra IV - Desafíos Prácticos.

El detalle completo de lo implementado, las decisiones temporales y las integraciones pendientes se encuentra en [G05_IMPLEMENTACION_Y_PENDIENTES.md](G05_IMPLEMENTACION_Y_PENDIENTES.md).

## Creación de desafíos G05

Esta primera versión permite guardar contenido práctico reutilizable, consultar su detalle y verlo en un listado. Guardar un desafío no lo publica ni lo asocia a un curso. El perfil local también permite iniciar y listar un registro mínimo de intento, pero no ejecuta código ni administra entregas, vidas, progreso, XP o monedas.

### Levantar en local

Requisitos: Java 21, Maven, Node.js y npm. No requiere Docker ni PostgreSQL.

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
- `TESTS`
- `CHALLENGE_VERSIONS`
- `ATTEMPTS`

`PRACTICAL_CHALLENGES.challenge_id` queda nullable hasta integrar el Motor. La consola H2 solo se habilita en el perfil local y el servidor queda limitado a localhost.

### Simplificación técnica

En esta versión los casos de prueba se modelan como pares de entrada/salida. Es una simplificación técnica temporal pendiente de acordar con Sandbox, no una exigencia del PRD.

Los intentos locales solo registran el inicio y un `answer_code` vacío. No equivalen a publicación, entrega ni ejecución.

## Verificación automatizada

```bash
cd backend
mvn test

cd ../frontend
npm run build
```
