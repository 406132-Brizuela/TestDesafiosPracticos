# Contexto de arquitectura (Tema 05 — Desafíos Prácticos)

Este archivo resume decisiones de arquitectura que no son obvias leyendo el
código solo, para que cualquier agente que trabaje acá no las rompa sin
saberlo. La documentación completa de decisiones del proyecto vive en una
carpeta del equipo fuera de este repo (no se sube a GitHub); esto es el
resumen mínimo necesario para trabajar en el código.

Se va a ir ampliando por secciones a medida que se documenten otras partes
del sistema. No asumas que cubre todo el dominio.

## Desafío: supra-entidad (Motor) / sub-entidad (nosotros)

Un desafío práctico está partido en dos, por diseño, con un mismo
`desafioId` compartido y **sin duplicación de campos entre los dos lados**:

- **Motor (Tema 03) administra**: nombre, dificultad, fechas de
  apertura/cierre, obligatoriedad, reintentos permitidos, tipo, estado
  publicable.
- **Nosotros (Tema 05) administramos**: perfil de evaluación, escenario
  base/código inicial, casos de prueba — todo referenciado por el
  `desafioId` que genera Motor.

Reglas derivadas:

- **Nunca generamos un id propio para el desafío.** `desafioId` es la
  identidad real; cualquier entidad nuestra debe referenciarlo como campo
  obligatorio, no opcional.
- **Nunca duplicamos `nombre`/`dificultad`/`fechas`** en entidades propias
  — esos campos son de Motor, no nuestros, aunque en la práctica hoy no
  tengamos forma de consultarlos.

Mismo patrón supra/sub aplicado al intento (§6 de `contrato-con-tema-03.md`,
confirmado con Tema 03 el 17-sep-2026): `AttemptEntity.id` es el `intentoId`
recibido como dato de entrada, no generado por nosotros; `alumnoId` del
payload de Motor no se usa como identidad — `userId` del Gateway ya cumple
ese rol.

### Motor nunca nos llama, y nosotros nunca llamamos a Motor, para crear

Importante — esto se corrigió después de haberlo modelado al revés una vez:
la creación es un **redirect de browser**, no una llamada de backend en
ningún sentido. Motor crea su registro, genera `desafioId`, y redirige al
profesor a nuestra pantalla con `desafioId` por query param. Nosotros
**nunca le pedimos un id a Motor** — `desafioId` es un dato de **entrada**
en nuestro propio request (`PracticalChallengeRequest.desafioId`), no algo
que generemos ni confirmemos con nadie.

- `MotorDesafioClient` (paquete `motorstub`) ya no genera ids — solo
  **registra/ecoa localmente** el `title`/`difficulty` que llegaron en el
  request, para que nuestras propias pantallas los puedan seguir mostrando
  sin duplicarlos en la entidad real. Es un eco temporal, no un cliente que
  hable con nadie.
- A diferencia de otros puertos de este mismo patrón (ver el publicador de
  eventos abajo), **este puerto no se reemplaza por un adaptador HTTP real
  — desaparece por completo** el día que Motor exista, porque en ese
  momento el dato ya llega solo, sin necesitar eco local.
- El publicador del evento `ContenidoPracticoPersistido` sí sigue el
  patrón puerto+adaptador clásico: hoy loguea (`LoggingDesafioContenidoEventPublisher`),
  el día que Motor esté disponible se cambia por un adaptador real (bus de
  eventos) — ahí sí el modelo no cambia, solo el adaptador.

Implementación: paquete `com.tp.desafiospracticos.motorstub`
(`MotorDesafioClient` / `StubMotorDesafioClient`), y
`DesafioContenidoEventPublisher` / `LoggingDesafioContenidoEventPublisher`
en `practicalchallenge`. El paquete `motorstub` completo se elimina el día
que se integra Motor real (no se convierte en cliente HTTP).

Si estás tocando código de creación de desafíos y ves `title`/`difficulty`
o un id propio en una entidad de contenido, es una violación de este
contrato, no una decisión válida — avisá antes de replicar el patrón.

## Entrega del código del alumno: Git para transporte, Sandbox para ejecución (DT-08)

**Estado: en negociación (dirección de equipo del 17-sep-2026), no
confirmado con el PO ni renegociado con Tema 06.** No tratar como cerrado.

- El código del alumno **no se guarda como texto en nuestra base** — vive
  en un repo de GitHub por `(alumno, desafío)`, bajo una organización que
  administra Tema 05. El alumno pushea con su propia cuenta (vinculada en
  el login, `RF-USR-06`) como colaborador.
- Al evaluar, no le mandamos código inline al Sandbox (Tema 06) — le
  mandamos una referencia `{ repo, ref }` y ellos clonan.
- El Sandbox **sigue siendo el motor de ejecución** — esto NO es
  GitHub Actions. Ejecutar tests sigue siendo trabajo de Tema 06.
- Implicación pendiente de aplicar en código: `AttemptEntity.answerCode`
  (hoy `Map<String,Object>`/JSON) no debería guardar código — debería
  guardar la referencia al repo/commit evaluado. Todavía no corregido.

## Qué autoría el profesor vs. qué recibe el Sandbox — no son lo mismo

El profesor carga **un solo archivo** de código base (`template`) más
casos de prueba como pares `entrada`/`esperado` — así está modelado hoy y
así debe seguir. El array multi-archivo con un test tipo JUnit
(`archivos: [Solucion.java, SolucionTest.java]`) que ve el Sandbox
(`contrato-con-tema-06.md`) **lo generamos nosotros** a partir de esos
casos de prueba — es una traducción interna (capa anticorrupción /
adaptador de métricas, ver `engine-evaluacion-G05.md` §5.1), no algo que
el profesor edite ni un cambio en su modelo de autoría.
