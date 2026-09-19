import type { ProjectFile } from './projects';

export type RiskLevel = 'ALTO' | 'MEDIO' | 'BAJO';
export type Difficulty = 'BASICO' | 'MEDIO' | 'AVANZADO';
export type Verdict = 'SUPERADO' | 'FALLADO' | 'ERROR_TECNICO';
export type Role = 'PROFESOR' | 'ADMIN' | 'ALUMNO';
export type IntegrityEventType = 'COPY' | 'PASTE' | 'WINDOW_BLUR' | 'WINDOW_FOCUS';

export interface IntegrityEvent {
  type: IntegrityEventType;
  timestamp: string;
  characters?: number;
  lines?: number;
}

export type IntegrityRiskLevel = 'NONE' | 'LOW' | 'MEDIUM' | 'HIGH';

export interface IntegrityRisk {
  level: IntegrityRiskLevel;
  reasons: string[];
}

export type ChallengeSubtype =
  | 'algorithms'
  | 'block-completion'
  | 'find-bug'
  | 'refactoring'
  | 'hackathon'
  | 'modeling'
  | 'code-review';

export const CHALLENGE_SUBTYPES: ChallengeSubtype[] = [
  'algorithms',
  'block-completion',
  'find-bug',
  'refactoring',
  'hackathon',
  'modeling',
  'code-review',
];

export const SUBTYPE_RISK: Record<ChallengeSubtype, RiskLevel> = {
  'block-completion': 'ALTO',
  'find-bug': 'ALTO',
  algorithms: 'MEDIO',
  refactoring: 'MEDIO',
  modeling: 'MEDIO',
  hackathon: 'BAJO',
  'code-review': 'BAJO',
};

export const RUNNABLE_SUBTYPES = new Set<ChallengeSubtype>([
  'algorithms',
  'block-completion',
  'find-bug',
]);

export interface SubtypeMeta {
  label: string;
  short: string;
  description: string;
  riskLevel: RiskLevel;
  runnable: boolean;
}

export const SUBTYPE_META: Record<ChallengeSubtype, SubtypeMeta> = {
  algorithms: {
    label: 'Algoritmos',
    short: 'alg',
    description: 'Resolución de un problema algorítmico completo, de entrada y salida definida.',
    riskLevel: 'MEDIO',
    runnable: true,
  },
  'block-completion': {
    label: 'Completar bloque',
    short: 'blk',
    description: 'Completar una parte faltante dentro de un código ya esqueleto.',
    riskLevel: 'ALTO',
    runnable: true,
  },
  'find-bug': {
    label: 'Encontrar el bug',
    short: 'bug',
    description: 'Detectar y corregir el defecto en un código que no funciona como debería.',
    riskLevel: 'ALTO',
    runnable: true,
  },
  refactoring: {
    label: 'Refactoring',
    short: 'ref',
    description: 'Mejorar calidad, nombres y estructura de un código que ya funciona.',
    riskLevel: 'MEDIO',
    runnable: false,
  },
  hackathon: {
    label: 'Hackathon',
    short: 'hck',
    description: 'Resolución colaborativa y creativa de una consigna abierta con límite de tiempo.',
    riskLevel: 'BAJO',
    runnable: false,
  },
  modeling: {
    label: 'Modelado de dominio',
    short: 'mdl',
    description: 'Diseñar el modelo de datos / clases de acuerdo a una descripción de negocio.',
    riskLevel: 'MEDIO',
    runnable: false,
  },
  'code-review': {
    label: 'Code review',
    short: 'rev',
    description: 'Evaluar críticamente un código ajeno y justificar mejoras.',
    riskLevel: 'BAJO',
    runnable: false,
  },
};

export interface HiddenTest {
  name: string;
  input?: string;
  expected: string;
  source?: string;
}

export interface FailingTest {
  name: string;
  input: string;
  expected: string;
  actual: string;
  status: string;
}

export type MigrationSandboxRuntime = 'maven-test' | 'node-spec';

export interface ChallengeMetadata {
  version: number;
  createdAt: string;
  updatedAt: string;
  softDeleted: boolean;
  notes?: string;
  materialDocs?: string[];
  riskLevel: RiskLevel;
}

export interface ChallengeConfiguration {
  language: string;
  entry: string;
  baseFiles: ProjectFile[];
  hiddenTests: HiddenTest[];
  expectedSolution: string;
  runtime?: MigrationSandboxRuntime;
  timeLimitMs?: number;
}

export interface Challenge {
  challengeId: string;
  courseCohortId: string;
  title: string;
  topic?: string;
  /** Consigna del desafío. Solo viene poblado para desafíos del backend Java (ver challengeFromJavaResponse). */
  statement?: string;
  subtype: ChallengeSubtype;
  difficulty: Difficulty;
  mandatory?: boolean;
  durationMs?: number | null;
  configuration: ChallengeConfiguration;
  metadata: ChallengeMetadata;
  riskLevel?: RiskLevel;
}

export interface ChallengeListItem {
  challengeId: string;
  courseCohortId: string;
  title: string;
  topic: string;
  notes: string;
  subtype: ChallengeSubtype;
  difficulty: Difficulty;
  mandatory: boolean;
  riskLevel: RiskLevel;
  metadata: { version: number; createdAt: string; updatedAt: string; softDeleted: boolean };
  configuration: {
    language: string;
    entry: string;
    runtime?: MigrationSandboxRuntime;
    fileCount: number;
    entryContent: string;
    testCount: number;
  };
}

export interface Draft {
  challengeId: string;
  courseCohortId: string;
  title: string;
  topic: string;
  difficulty: Difficulty;
  mandatory: boolean;
  subtype: ChallengeSubtype;
  durationMs: number | null;
  notes: string;
  materialDocs: string[];
  language: 'typescript';
  entry: string;
  runtime?: MigrationSandboxRuntime;
  baseFiles: ProjectFile[];
  hiddenTestsText: string;
  expectedSolutionText: string;
  timeLimitMs: number;
}

export interface ChatMessage {
  role: 'user' | 'assistant';
  text: string;
}

export interface ChatReply {
  ok: boolean;
  reply: string;
  blocked: boolean;
  riskLevel: RiskLevel;
}

export interface ChatStatus {
  ok: boolean;
  chat: { provider: string; model: string; ollamaUrl: string | null };
}

export interface PreviewRunResult {
  ok: boolean;
  status: string;
  output: string;
  error: string;
  tests?: Array<{ name: string; passed: boolean; message?: string }>;
  timeMs?: number;
  memoryBytes?: number;
}

export interface EvaluationResult {
  ok: boolean;
  executionId: string;
  challengeId: string;
  evaluate: boolean;
  verdict?: Verdict;
  feedback?: string;
  failingTest?: FailingTest | null;
  status?: string;
  output?: string;
  error?: string;
  tests?: Array<{ name: string; passed: boolean; message?: string }>;
  timeMs?: number;
  memoryBytes?: number;
}

export interface SubmissionResult {
  ok: boolean;
  submissionId: string;
  challengeId: string;
  courseCohortId: string;
  studentId: string;
  verdict: Verdict;
  feedback: string;
  failingTest?: FailingTest | null;
  submittedAt: string;
  integrityEvents?: IntegrityEvent[];
  integrityRisk?: IntegrityRisk;
  /** Presente solo cuando la resolución se evaluó contra el engine Java (ver submissionFromJavaResult). */
  engine?: {
    quality: number | null;
    profileId: string;
    profileVersion: number;
    approvalThreshold: number;
    status: JavaEvaluationStatus;
    dimensions: JavaCorrectionDimension[];
    testsTotal: number | null;
    testsPassed: number | null;
  };
}

// ---------------------------------------------------------------------------
// Backend Java real (Tema 05 / G05) — GET /challenges/{id} y POST /engine/evaluate.
// Tipos y mapeos separados de los del store Node de arriba: son dos fuentes de
// datos distintas que hoy no comparten forma (ver EngineService).
// ---------------------------------------------------------------------------

export interface JavaPublicTestCase {
  name: string;
  input: string;
}

/** Espejo de web.ChallengeResponse (backend Java). Nunca trae expected ni tests PRIVADO. */
export interface JavaChallengeResponse {
  id: string;
  consigna: string;
  lenguaje: string;
  starterCode: string;
  testsPublicos: JavaPublicTestCase[];
}

export type JavaEngineProfileId = 'introductorio' | 'avanzado';

/** Espejo de web.CompileCheckRequest (backend Java) — POST /engine/compile. */
export interface JavaCompileCheckRequest {
  lenguaje: string;
  code: string;
}

/** Espejo de engine.metrics.CompileDiagnostic (backend Java). */
export interface JavaCompileDiagnostic {
  line: number;
  message: string;
}

/** Espejo de engine.metrics.CompileCheckResult (backend Java). Sin tests, sin quality. */
export interface JavaCompileCheckResult {
  compiles: boolean;
  diagnostics: JavaCompileDiagnostic[];
}

/**
 * Espejo de web.EvaluationRequest (backend Java). profileId es un campo de
 * compat que el backend ignora: la rúbrica ahora es un dato del desafío
 * (Challenge.evaluationProfileId), resuelto server-side por challengeId — el
 * front ya no lo elige ni lo manda.
 */
export interface JavaEvaluationRequest {
  submissionId: string;
  challengeId: string;
  lenguaje: string;
  code: string;
  profileId?: JavaEngineProfileId | string | null;
}

export type JavaEvaluationStatus = 'COMPLETED' | 'NO_COMPILE' | 'PARTIAL_PENDING';
export type JavaSuggestedVerdict = 'APPROVED' | 'NOT_APPROVED' | 'PENDING';
// PENDING_SANDBOX: transitorio, se reintenta cuando el sandbox vuelva. NOT_APPLICABLE:
// final, no hay analizador estático para el lenguaje del request (nunca se reintenta).
export type JavaDimensionState = 'OK' | 'PENDING_SANDBOX' | 'NOT_APPLICABLE';

export interface JavaCorrectionDimension {
  dimension: string;
  subScore: number | null;
  weight: number;
  contribution: number;
  source: string;
  state: JavaDimensionState;
  evidence: Record<string, unknown>;
}

/** Espejo de engine.domain.EvaluationResult (backend Java). */
export interface JavaEvaluationResult {
  submissionId: string;
  profileId: string;
  profileVersion: number;
  engineVersion: string;
  status: JavaEvaluationStatus;
  quality: number | null;
  suggestedVerdict: JavaSuggestedVerdict;
  approvalThreshold: number;
  dimensions: JavaCorrectionDimension[];
  feedbackAlumno: string[];
}

/** Arma el Challenge que ya consume la UI a partir de la respuesta del backend Java. */
export function challengeFromJavaResponse(java: JavaChallengeResponse): Challenge {
  const entry = 'Main.java';
  return {
    challengeId: java.id,
    courseCohortId: '',
    // El backend Java (GET /challenges/{id}) no devuelve título (eso vive del lado de
    // Motor, en el endpoint de profesor) — usamos el id como fallback visible.
    title: java.id,
    statement: java.consigna,
    subtype: 'algorithms',
    difficulty: 'BASICO',
    mandatory: false,
    durationMs: null,
    configuration: {
      language: java.lenguaje.toLowerCase(),
      entry,
      baseFiles: [{ path: entry, content: java.starterCode }],
      // Solo inputs de tests PUBLICO. expected queda '' a propósito: el backend Java
      // nunca lo manda y el front no debe mostrarlo.
      hiddenTests: java.testsPublicos.map((test) => ({ name: test.name, input: test.input, expected: '' })),
      expectedSolution: '',
    },
    metadata: {
      version: 1,
      createdAt: '',
      updatedAt: '',
      softDeleted: false,
      riskLevel: 'MEDIO',
    },
  };
}

/** Traduce el resultado del engine (status + suggestedVerdict) al Verdict que ya usa la UI. */
export function verdictFromJavaResult(result: JavaEvaluationResult): Verdict {
  if (result.status === 'NO_COMPILE') {
    return 'FALLADO';
  }
  if (result.status === 'PARTIAL_PENDING') {
    return 'ERROR_TECNICO';
  }
  return result.suggestedVerdict === 'APPROVED' ? 'SUPERADO' : 'FALLADO';
}

export function feedbackFromJavaResult(result: JavaEvaluationResult): string {
  if (result.status === 'NO_COMPILE') {
    return 'Tu código no compiló.';
  }
  if (result.status === 'PARTIAL_PENDING') {
    return 'El sandbox no respondió; el resultado quedó pendiente de reevaluación.';
  }
  const quality = result.quality != null ? `Calidad: ${result.quality}/100.` : '';
  const feedback = result.feedbackAlumno.join(' ');
  return [quality, feedback].filter(Boolean).join(' ') || 'Sin feedback adicional.';
}

/** Lee testsTotal/testsPassed de la evidencia de la dimensión "correctness", si está. */
export function correctnessSummaryOf(
  result: JavaEvaluationResult,
): { passed: number; total: number } | null {
  const correctness = result.dimensions.find((dimension) => dimension.dimension === 'correctness');
  const evidence = correctness?.evidence as { testsTotal?: number; testsPassed?: number } | undefined;
  if (!evidence || typeof evidence.testsTotal !== 'number' || typeof evidence.testsPassed !== 'number') {
    return null;
  }
  return { passed: evidence.testsPassed, total: evidence.testsTotal };
}

/** Arma el SubmissionResult que ya consume challenge-result.ts a partir del EvaluationResult del engine. */
export function submissionFromJavaResult(challenge: Challenge, result: JavaEvaluationResult): SubmissionResult {
  const summary = correctnessSummaryOf(result);
  return {
    ok: true,
    submissionId: result.submissionId,
    challengeId: challenge.challengeId,
    courseCohortId: challenge.courseCohortId,
    studentId: 'alumno-demo',
    verdict: verdictFromJavaResult(result),
    feedback: feedbackFromJavaResult(result),
    failingTest: null,
    submittedAt: new Date().toISOString(),
    engine: {
      quality: result.quality,
      profileId: result.profileId,
      profileVersion: result.profileVersion,
      approvalThreshold: result.approvalThreshold,
      status: result.status,
      dimensions: result.dimensions,
      testsTotal: summary?.total ?? null,
      testsPassed: summary?.passed ?? null,
    },
  };
}

export interface CreateChallengePayload {
  challengeId: string;
  courseCohortId: string;
  title: string;
  topic: string;
  subtype: ChallengeSubtype;
  difficulty: Difficulty;
  mandatory: boolean;
  durationMs?: number | null;
  notes: string;
  materialDocs: string[];
  configuration: {
    language: string;
    entry: string;
    runtime?: MigrationSandboxRuntime;
    baseFiles: ProjectFile[];
    hiddenTests: HiddenTest[];
    expectedSolution: string;
    timeLimitMs?: number;
  };
}

export const DEFAULT_COHORT = 'TUP-2026-01';

export function slugify(text: string): string {
  const cleaned = text
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
  return cleaned || 'desafio';
}

export function parseHiddenTests(
  text: string,
): { tests: HiddenTest[]; language?: string } | { tests: null; error: string; language?: string } {
  try {
    const value = JSON.parse(text);
    if (Array.isArray(value)) {
      for (const test of value) {
        if (!test || typeof test.name !== 'string' || typeof test.expected !== 'string') {
          return { tests: null, error: 'Cada test debe tener name (string) y expected (string).' };
        }
        if (test.input !== undefined && typeof test.input !== 'string') {
          return { tests: null, error: 'El campo input (si existe) debe ser un string.' };
        }
      }
      return { tests: value as HiddenTest[] };
    }
    if (value && typeof value === 'object' && Array.isArray(value.testCases)) {
      const language = value.language;
      if (language !== undefined && language !== 'java' && language !== 'javascript') {
        return { tests: null, error: 'El campo language del JSON de tests debe ser "java" o "javascript".' };
      }
      const tests: HiddenTest[] = [];
      for (const testCase of value.testCases) {
        if (!testCase || typeof testCase.name !== 'string' || !testCase.name.trim()) {
          return { tests: null, error: 'Cada testCase del JSON debe tener un name (string) no vacío.' };
        }
        const test: HiddenTest = {
          name: testCase.name,
          expected:
            typeof testCase.expected === 'string'
              ? testCase.expected
              : typeof testCase.source === 'string'
                ? testCase.source
                : '',
        };
        if (typeof testCase.input === 'string') {
          test.input = testCase.input;
        }
        if (typeof testCase.source === 'string') {
          test.source = testCase.source;
        }
        tests.push(test);
      }
      if (tests.length === 0) {
        return { tests: null, error: 'El JSON de tests no contiene testCases.' };
      }
      return { tests, language };
    }
    return { tests: null, error: 'El JSON debe ser un arreglo de tests o un objeto { language, testCases }.' };
  } catch (error) {
    return { tests: null, error: error instanceof Error ? `JSON invalido: ${error.message}` : 'JSON invalido.' };
  }
}