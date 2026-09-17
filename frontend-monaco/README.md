# Monaco MultiView — Desafíos Prácticos (G5)

Prototype of the **"Motor de Desafíos Prácticos"** (Grupo 5) for the UTN TUP gamified learning platform. It evolved from a Monaco IDE sandbox into a full challenge production and solving platform: challenge authoring in a 4-step wizard, CRUD, sandbox code execution, hidden-test evaluation, anti-cheat integrity signals, an AI tutor chat and an IDE for solving challenges.

## What it does

- **Challenge authoring wizard** (`Challenges → Nuevo desafío`): pick a subtype (algorithm, block-completion, find-bug, refactoring, hackathon, modeling, code-review), fill metadata + hidden tests, preview-run, and publish. Templates exportable/pasteable into the "Motor G3" gateway payload.
- **Challenge CRUD**: list, view, edit (bumps `metadata.version`), soft delete (reversible by an admin).
- **Sandbox execution** through the Node backend:
  - **TypeScript / JavaScript** — bundled with esbuild, runs on Node (stdin supported).
  - **Java** — compiled with `javac`, run with `java`.
  - **`maven-test` runtime** — runs a real `mvn test` and parses Surefire reports.
  - **`node-spec` runtime** — installs jsdom and runs `node --test` (TAP parsed).
- **Evaluation**: LeetCode-style — only the first failing hidden test is exposed to the student; the full test set and `expectedSolution` never leave the server.
- **Anti-cheat integrity**: a pure function (`assessIntegrityRisk`) grades `COPY / PASTE / WINDOW_BLUR / WINDOW_FOCUS` events into `NONE / LOW / MEDIUM / HIGH` risk (e.g. `COPY → FOCUS_LOST → PASTE` = `HIGH`).
- **AI tutor chat**: floating chat panel wired to `POST /api/chat` (stub provider by default, Ollama optional). Risk level is always derived server-side.
- **Role-simulated views**: a header selector switches between `PROFESOR`, `ADMIN` and `ALUMNO` (no real auth — it's a prototype shell).

## Tech stack

| Layer | Technology |
|---|---|
| Frontend | Angular 22 (standalone components, signals, `inject()`, new `@if/@for/@switch` control flow) + Monaco Editor |
| API calls | Plain `fetch` via `src/app/compile.service.ts` (no `HttpClient`) |
| Backend | Plain Node ESM (`server/*.mjs`, no framework, no Express) |
| Build | `@angular/build` (esbuild-based); Monaco workers for TS/JSON/editor |
| Sandbox runtimes | Node, `javac`/`java`, Maven, jsdom + `node --test` |

## Requirements

- **Node >= 18** (npm 11 via `packageManager: npm@11.19.0`)
- **JDK 21** and **Maven** on the PATH (Java sandbox + `maven-test` challenges)
- On Windows the server launches Maven/NPM via `cmd /c mvn.cmd` / `npm.cmd`.

> Note: `esbuild` is used by the backend (for TS bundling) but arrives transitively through `@angular/build`; it is not a direct dependency of `package.json`.

## Quick start

```bash
npm install
npm run dev
```

`npm run dev` starts the API (**port 3100**) and the Angular dev server together on a free port (the URL is printed in the console). Open it and sign in as whatever role you want.

Alternatively, in two terminals:

```bash
# Terminal 1: API only (port 3100)
npm run server

# Terminal 2: frontend (port 4200)
npm start
```

Open `http://localhost:4200/`. The frontend proxies `/api` to `http://localhost:3100` via `proxy.conf.json`.

The first time you run a `maven-test` challenge it downloads Maven dependencies and may take a while. If `server/data/challenges.json` is emptied/deleted, the 7 seed challenges are re-inserted on the next boot.

## Available scripts

| Command | What it does |
|---|---|
| `npm run dev` | API (3100) + Angular dev server together (free port) |
| `npm start` | Angular dev server only (port 4200) |
| `npm run server` | Node API only (port 3100) |
| `npm run build` | Production build (`ng build`); the closest thing to a typecheck |
| `npm run watch` | `ng build --watch --configuration development` |
| `npm test` | **NOT wired**: `angular.json` has no `test` target and there are no `*.spec.ts` files. Real tests live at `server/practical.test.mjs` |

## Tests

The only real test suite is the integration one on the backend:

```bash
node --test server/practical.test.mjs
```

It spawns the API on **port 3901** with a temporary `MMV_DATA_DIR` and covers seeding, CRUD, executions, submissions, integrity risk and chat. It is slow: the seeds run a real `mvn test` (needs a warm `~/.m2`) and an `npm install jsdom` + `node --test`.

## API overview

`server/index.mjs` routes requests through a single `handleRequest` (CORS enabled, JSON bodies up to 20 MB). Main endpoints:

| Method | Route | Purpose |
|---|---|---|
| `GET` | `/api/status` | Server health + whether a Java run is active |
| `POST` | `/api/ts/compile` | esbuild bundle TS/JS → CJS |
| `POST` | `/api/java/compile` | `javac` compilation |
| `POST` | `/api/java/run` | Run Java, streaming stdout (chunked, abortable) |
| `POST` | `/api/java/stop` | Kill an active Java run |
| `GET` | `/api/challenges` | List challenges (never exposes `hiddenTests`/`expectedSolution`) |
| `POST` | `/api/challenges` | Create challenge (validated) |
| `GET` | `/api/challenges/:id` | Full challenge |
| `PUT` | `/api/challenges/:id` | Update (bumps `metadata.version`) |
| `DELETE` | `/api/challenges/:id` | Soft delete |
| `POST` | `/api/practical-challenges/:id/executions` | Run / evaluate a challenge solution |
| `POST` | `/api/submissions` | Submit with verdict, feedback, failing test and integrity risk |
| `POST` | `/api/chat` | AI tutor reply |
| `GET` | `/api/chat/status` | Chat provider / model / Ollama URL |
| `POST` | `/api/preview-run` | Wizard preview execution |

### Environment variables

| Variable | Default | Purpose |
|---|---|---|
| `PORT` | `3100` | API port |
| `NG_APP_API_BASE` | `/api` | Frontend base URL for the API (build-time; default works via the dev proxy — override in `angular.json` → `define` before `npm run build`) |
| `MMV_DATA_DIR` | `server/data` | JSON persistence directory |
| `CHAT_PROVIDER` | `stub` | `stub` or `ollama` |
| `OLLAMA_URL` | `http://localhost:11434/v1` | Ollama OpenAI-compatible endpoint |
| `OLLAMA_MODEL` | `llama3` | Ollama model name |

### Challenge domain

- **Subtypes**: `algorithms`, `block-completion`, `find-bug`, `refactoring`, `hackathon`, `modeling`, `code-review`. The first three are **runnable** (and therefore evaluable). A challenge is also evaluable if its `configuration.runtime` is `maven-test` or `node-spec`. Challenges with neither are plain "consignas" — executions return `422`.
- **Difficulties**: `BASICO`, `MEDIO`, `AVANZADO`. **Risk levels**: `ALTO`, `MEDIO`, `BAJO`.
- **Seeds**: 7 example challenges covering TS algorithms, block-completion, find-bug, a Spring Boot `maven-test` challenge, two Angular forms challenges and an HTML/CSS/JS `node-spec` challenge.

## Project layout

```
.
├── src/                          Angular 22 app (standalone components)
│   ├── index.html                app shell / entry HTML
│   ├── main.ts                   bootstrap (imports monaco-setup first, then bootstrapApplication)
│   ├── styles.css                global styles
│   ├── monaco-setup.ts           wires self.MonacoEnvironment.getWorker → the web workers in workers/
│   ├── workers/                  Monaco web workers (loaded by the browser in background threads)
│   │   ├── ts.worker.ts          TypeScript/JavaScript language services (IntelliSense, diagnostics)
│   │   ├── json.worker.ts        JSON validation & completion
│   │   ├── html.worker.ts        HTML formatting & validation
│   │   ├── css.worker.ts         CSS/SCSS/Less language services
│   │   └── editor.worker.ts      fallback / core editor worker
│   └── app/
│       ├── app.ts                root component: toolbar, role selector (PROFESOR/ADMIN/ALUMNO), banner, router-outlet
│       ├── app.config.ts         bootstrapping providers (router + view transitions + global error listeners)
│       ├── app.routes.ts         routes (dashboard, challenges/new, /:id/edit, /:id/solve, /:id/result, /:id/published)
│       ├── views/                page components (inline templates)
│       │   ├── dashboard.ts      landing: challenge list, role-aware actions, header/entry points
│       │   ├── challenge-wizard.ts  4-step create/edit wizard (metadata, hidden tests, preview-run, publish)
│       │   ├── challenge-solve.ts   student IDE: statement, Monaco editor, run/evaluate, integrity events, chat
│       │   ├── challenge-result.ts  evaluation verdict, failing test, feedback
│       │   └── challenge-published.ts  publish/export of the challenge (Gateway G3 payload, copy/paste)
│       ├── services/             shared signals state
│       │   ├── session.service.ts    simulated role (PROFESOR/ADMIN/ALUMNO)
│       │   ├── challenges.service.ts challenge list/busy state
│       │   └── banner.service.ts     global feedback banner
│       ├── monaco-editor.ts      reusable Monaco wrapper (sidecar .html/.css); emits value + integrity events
│       ├── chat-panel.ts         floating AI tutor panel (sidecar .html/.css)
│       ├── compile.service.ts    single fetch-based API client (no HttpClient), all /api endpoints
│       ├── challenge-types.ts    domain model + types + subtype/runtime constants
│       ├── challenge-drafts.ts   wizard state + per-subtype empty templates
│       ├── projects.ts           sample multi-file projects (TypeScript, Java, Spring Boot) for the sandbox demos
│       ├── run.ts                client-side TS runner in a sandboxed Worker (for simple demos; time-bounded)
│       ├── shared.ts             shared helpers (riskOf, isRunnable, output comparison, navigation)
│       └── test-formatter.ts     parses author-written hidden-test snippets into JSON test cases
├── public/                       static assets copied verbatim into the build (angular.json assets → only favicon.ico)
├── dist/                         production build output (generated by npm run build; monaco-multiview/browser/)
├── server/                       Node backend (ESM, no framework)
│   ├── index.mjs                 HTTP routing + API + seed challenges + validation
│   ├── executor.mjs              sandbox runtimes (esbuild / javac+java / maven-test / node-spec)
│   ├── chat.mjs                  AI tutor (stub / Ollama, anti-leak guard)
│   ├── integrity.mjs             assessIntegrityRisk (pure function)
│   ├── store.mjs                 JSON persistence (in-memory cache + write-through)
│   ├── dev.mjs                   dev launcher (API + Angular together)
│   ├── practical.test.mjs        integration tests (node --test)
│   └── data/*.json               persistence — challenges, executions, submissions, ia-logs (gitignored; don't edit while the server runs)
├── docs/                         PRD, Grupo 5 epic proposal, PIV architecture docs (Spanish)
├── taiga/                        project-management exports from Taiga: epics + user stories + IDE analysis (Spanish)
├── proxy.conf.json               dev-time proxy: /api → http://localhost:3100
├── angular.json / package.json / tsconfig*.json / .prettierrc / .editorconfig
├── AGENTS.md                     agent/developer conventions
├── DESIGN_DECISIONS.md           design decisions log (Spanish)
└── .opencode/                    OpenCode skills + slash commands (openspec workflow)
```

> Note: syntax highlighting for the ~100 languages Monaco supports out of the box (Java included) comes from **Monarch**, the regex-based tokenizer bundled inside the `monaco-editor` package's `basic-languages` set (no extra dependency). The web workers above are a different thing: they run the language services that provide real IntelliSense (completion, diagnostics, etc.) and only exist for TypeScript/JavaScript, JSON, HTML, CSS and the core editor. Java has Monarch highlighting but **no** language server, so no IntelliSense for it.

## Documentation

- `docs/PRD-Plataforma-Gamificada-TP.md` — full product PRD for the gamified platform.
- `docs/Propuesta_Grupo5.docx.md` — Grupo 5 "Motor de Desafíos Prácticos" epics.
- `docs/PIV-FE-Propuesta-Arquitectura.md`, `docs/PIV-BE-Propuesta-Arquitectura.md` — frontend/backend architecture proposals.
- `DESIGN_DECISIONS.md` — design decisions and rationale.

> Note: `docs/` files are in Spanish; code and messages stay in English, while server UI-facing verdicts/feedback are Spanish.