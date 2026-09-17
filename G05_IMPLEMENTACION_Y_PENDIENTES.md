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

Por ese motivo, `PRACTICAL_CHALLENGES.challenge_id` permanece nullable. El valor debera completarse cuando exista el contrato de alta o asociacion con el Motor.

### Intento no equivale a entrega

Un registro en `ATTEMPTS` representa un inicio minimo. Actualmente:

- `answer_code` comienza con `{ "code": "" }`.
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
| `practical_challenge_id` | UUID propio de G05 |
| `challenge_id` | Referencia futura al Motor; actualmente nullable |
| `challenge_type_id` | Tipo y perfil tecnico |
| `title` | Titulo visible |
| `statement` | Consigna |
| `difficulty` | `BASICO`, `MEDIO` o `AVANZADO` |
| `template` | Codigo inicial |
| `user_creator_id` | Identidad recibida desde seguridad, si existe |
| `creation_datetime` | Fecha de creacion |

`title` y `difficulty` son extensiones necesarias para el formulario y el listado del MVP.

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
| `attempt_id` | UUID del intento |
| `practical_challenge_id` | Contenido que se intenta resolver |
| `answer_code` | JSON con codigo de respuesta; comienza vacio |
| `creation_datetime` | Fecha de inicio |
| `submission_datetime` | Fecha futura de entrega |
| `llm_conversation_id` | Referencia futura a una conversacion asistida |
| `user_id` | Usuario que inicio el intento |

`user_id` se agrego para poder filtrar actividad por usuario cuando la aplicacion recibe identidad del Gateway.

## 5. Flujo transaccional de creacion

La operacion `POST /api/desafiospracticos/desafios` realiza estos pasos:

1. Valida el request.
2. Obtiene o crea el catalogo minimo de Java, perfil Java I/O y tipo de algoritmo.
3. Genera el UUID de `PRACTICAL_CHALLENGES`.
4. Persiste titulo, consigna, dificultad, plantilla, creador y fecha.
5. Crea una entidad `TESTS` por cada caso recibido.
6. Crea una relacion `CHALLENGE_VERSIONS` version `1` por cada test.
7. Conserva el orden original mediante `test_order`.
8. Devuelve `201 Created`, el recurso persistido y el header `Location`.

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

La URL del backend esta fija temporalmente en `http://localhost:8080/api/desafiospracticos`. Antes de desplegar con Gateway debe externalizarse mediante configuracion de entorno.

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
- Relaciones entre las siete tablas implementadas.
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

Falta definir e implementar:

- Contrato para registrar o asociar el contenido de G05 en el Motor.
- Momento en que se asigna `PRACTICAL_CHALLENGES.challenge_id`.
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

Falta desarrollar el ciclo de vida completo del intento:

- Guardado de borrador y codigo por archivo o lenguaje.
- Actualizacion controlada de `answer_code`.
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
- Guardado o entrega de una solucion del estudiante.
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
| `backend/src/main/java/com/tp/desafiospracticos/config/SecurityConfig.java` | Seguridad para entornos integrados |
| `backend/src/main/resources/application-local.yml` | H2, consola y aislamiento local |
| `backend/src/test/java/com/tp/desafiospracticos/practicalchallenge/` | Pruebas del flujo implementado |
| `frontend/src/app/challenge-create/` | Formulario y cliente HTTP |
| `frontend/src/app/challenge-activity/` | Listado de desafios e intentos |
| `frontend/src/app/challenge-detail/` | Detalle del desafio |
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
