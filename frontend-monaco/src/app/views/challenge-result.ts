import { Component, Input, OnInit, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import type {
  ChatMessage,
  IntegrityEvent,
  JavaCorrectionDimension,
  JavaDimensionState,
  SubmissionResult,
} from '../challenge-types';
import type { ProjectFile } from '../projects';
import { goBack, prettyJson } from '../shared';
import { SessionService } from '../services/session.service';

interface SubmitPayload {
  submission: SubmissionResult;
  files: ProjectFile[];
  chatTranscript: ChatMessage[];
  integrityEvents?: IntegrityEvent[];
}

interface RubricRow {
  id: string;
  label: string;
  subScore: number | null;
  weight: number;
  contribution: number | null;
  source: string | null;
  counts: boolean;
  state: JavaDimensionState;
}

// Orden y nombre en español fijos: la rúbrica siempre muestra las 4 filas, aunque
// el engine solo haya devuelto las dimensiones con weight > 0 para el perfil usado
// (ver EngineServiceImpl: correctness siempre corre, el resto solo si pesa).
const DIMENSION_LABELS: Record<string, string> = {
  correctness: 'Correctitud',
  performance: 'Rendimiento',
  complexity: 'Complejidad',
  style: 'Estilo',
};
const DIMENSION_ORDER = ['correctness', 'performance', 'complexity', 'style'];

@Component({
  selector: 'app-challenge-result',
  standalone: true,
  template: `
    <section class="view student-submitted">
      @if (payload(); as payload) {
        <div class="result-card" [class.result-centered]="isStudent()">
          <!-- 0. Corrección parcial: el sandbox no respondió, esto no es un resultado final -->
          @if (isPartialPending()) {
            <div class="result-status-note">
              Corrección parcial — pendiente. Las dimensiones estáticas ya tienen nota; las que
              dependen del sandbox se van a completar cuando esté disponible.
            </div>
          }

          <!-- 1. Encabezado: veredicto + calidad, con el color como barra de progreso hasta {quality}% -->
          <div class="result-verdict" [class.ok]="isOk(payload.submission.verdict)" [class.fail]="!isOk(payload.submission.verdict)">
            <div class="result-verdict-fill" [style.width.%]="qualityPercent()"></div>
            <span class="result-verdict-label">{{ verdictLabel(payload.submission.verdict) }}</span>
            @if (engine(); as engine) {
              @if (engine.quality !== null) {
                <span class="result-quality">Calidad: <strong>{{ engine.quality }}/100</strong></span>
              }
            }
          </div>

          <!-- 2. Tests -->
          @if (engine(); as engine) {
            @if (engine.testsTotal !== null && engine.testsPassed !== null) {
              <span class="test-summary">
                Tests superados: <strong>{{ engine.testsPassed }} de {{ engine.testsTotal }}</strong>
              </span>
            }
          }

          <!-- 3. Rúbrica de evaluación -->
          @if (rubricRows().length > 0) {
            <div class="rubric">
              <p class="rubric-title">Rúbrica de evaluación</p>
              @for (row of rubricRows(); track row.id) {
                <div
                  class="rubric-row"
                  [class.rubric-row-off]="!row.counts"
                  [class.rubric-row-pending]="row.state === 'PENDING_SANDBOX'"
                  [class.rubric-row-na]="row.state === 'NOT_APPLICABLE'"
                >
                  <div class="rubric-row-top">
                    <span class="rubric-row-name">{{ row.label }}</span>
                    <span class="rubric-row-score mono">{{ scoreLabel(row) }}</span>
                  </div>
                  @if (row.state === 'OK') {
                    <div class="rubric-bar">
                      <div class="rubric-bar-fill" [style.width.%]="row.subScore ?? 0"></div>
                    </div>
                  }
                  <div class="rubric-row-meta">
                    <span>Peso {{ row.weight }}</span>
                    @if (row.contribution !== null) {
                      <span>Aporte {{ row.contribution }}</span>
                    }
                    @if (row.source) {
                      <span class="rubric-source">{{ row.source }}</span>
                    }
                    @if (!row.counts) {
                      <span class="rubric-off-note">no cuenta en este perfil</span>
                    }
                    @if (row.state === 'PENDING_SANDBOX') {
                      <span class="rubric-off-note">se reintenta cuando el sandbox esté disponible</span>
                    }
                    @if (row.state === 'NOT_APPLICABLE') {
                      <span class="rubric-off-note">sin analizador para este lenguaje</span>
                    }
                  </div>
                </div>
              }
            </div>
          }

          <!-- 4. Feedback al alumno -->
          @if (payload.submission.feedback) {
            <div class="feedback-block">
              <span class="cmp-label">Feedback</span>
              <p>{{ payload.submission.feedback }}</p>
            </div>
          }

          @if (payload.submission.failingTest; as fail) {
            <div class="fail-block">
              <div class="fail-head">
                Test "{{ fail.name }}" — {{ fail.status }}
                @if (fail.input) {
                  <span> · entrada: <span class="mono">{{ fail.input }}</span></span>
                }
              </div>
              <div class="cmp-grid">
                <div>
                  <span class="cmp-label">Salida esperada</span>
                  <pre class="mono">{{ fail.expected }}</pre>
                </div>
                <div>
                  <span class="cmp-label">Su salida</span>
                  <pre class="mono">{{ fail.actual }}</pre>
                </div>
              </div>
            </div>
          }

          <!-- 5. Trazabilidad -->
          <div class="traceability">
            @if (engine(); as engine) {
              <div class="cmp-line">
                <span class="cmp-label">Perfil</span>
                <span class="mono cmp-value">{{ engine.profileId }} v{{ engine.profileVersion }}</span>
              </div>
              <div class="cmp-line">
                <span class="cmp-label">Status</span>
                <span class="mono cmp-value">{{ engine.status }}</span>
              </div>
              <div class="cmp-line">
                <span class="cmp-label">Umbral de aprobación</span>
                <span class="mono cmp-value">{{ engine.approvalThreshold }}</span>
              </div>
            }
            <div class="cmp-line">
              <span class="cmp-label">Submission</span>
              <span class="mono cmp-value">{{ payload.submission.submissionId }}</span>
            </div>
            <div class="cmp-line">
              <span class="cmp-label">Fecha</span>
              <span class="mono cmp-value">{{ payload.submission.submittedAt }}</span>
            </div>
          </div>

          <!-- 6. JSON crudo, colapsable -->
          @if (!isStudent()) {
            <details class="json-details">
              <summary>Ver payload al Motor (JSON)</summary>
              <pre class="json-panel">{{ json() }}</pre>
            </details>
          }

          <div class="actions-bar">
            <button type="button" class="btn btn-secondary" (click)="retry()">
              Volver a intentar
            </button>
            <button type="button" class="btn" (click)="toDashboard()">Volver a la lista</button>
          </div>
        </div>
      } @else {
        <p class="muted">No encontramos la entrega de esta resolución.</p>
        <div class="actions-bar">
          <button type="button" class="btn btn-secondary" (click)="back()">← Volver</button>
          <button type="button" class="btn" (click)="toDashboard()">Volver a la lista</button>
        </div>
      }
    </section>
  `,
})
export class ChallengeResultComponent implements OnInit {
  @Input() id?: string;

  protected readonly payload = signal<SubmitPayload | null>(null);
  protected readonly engine = computed(() => this.payload()?.submission.engine ?? null);
  protected readonly isPartialPending = computed(() => this.engine()?.status === 'PARTIAL_PENDING');
  // Ancho del relleno del header: clampeado 0-100; sin quality (p. ej. ERROR_TECNICO,
  // sin score) no hay nada que rellenar.
  protected readonly qualityPercent = computed(() => {
    const quality = this.engine()?.quality;
    return quality == null ? 0 : Math.max(0, Math.min(100, quality));
  });
  protected readonly rubricRows = computed<RubricRow[]>(() => {
    const dimensions: JavaCorrectionDimension[] = this.engine()?.dimensions ?? [];
    if (dimensions.length === 0) {
      return [];
    }
    return DIMENSION_ORDER.map((id) => {
      const found = dimensions.find((dimension) => dimension.dimension === id);
      return {
        id,
        label: DIMENSION_LABELS[id] ?? id,
        subScore: found?.subScore ?? null,
        weight: found?.weight ?? 0,
        contribution: found?.contribution ?? null,
        source: found?.source ?? null,
        counts: (found?.weight ?? 0) > 0,
        state: found?.state ?? 'OK',
      };
    });
  });
  protected readonly json = computed(() =>
    prettyJson({
      submission: this.payload()?.submission ?? null,
      files: this.payload()?.files ?? [],
      chatTranscript: this.payload()?.chatTranscript ?? [],
    }),
  );
  protected readonly challengeId = computed(() => this.payload()?.submission.challengeId ?? this.id ?? null);

  protected readonly session = inject(SessionService);
  protected readonly isStudent = computed(() => this.session.role() === 'ALUMNO');
  private readonly router = inject(Router);

  /** "pendiente" (transitorio, sandbox caído) / "no aplica" (final, sin analizador) / nota numérica. */
  protected scoreLabel(row: RubricRow): string {
    if (row.state === 'PENDING_SANDBOX') {
      return 'pendiente';
    }
    if (row.state === 'NOT_APPLICABLE') {
      return 'no aplica';
    }
    return `${row.subScore ?? '—'}/100`;
  }

  protected isOk(verdict: string): boolean {
    return verdict === 'SUPERADO';
  }

  protected verdictLabel(verdict: string): string {
    if (verdict === 'SUPERADO') {
      return 'SUPERADO';
    }
    if (verdict === 'ERROR_TECNICO') {
      return 'ERROR TÉCNICO';
    }
    return 'NO SUPERADO';
  }

  ngOnInit(): void {
    const state = (window.history.state ?? {}) as { payload?: SubmitPayload };
    if (state.payload) {
      this.payload.set(state.payload);
    }
  }

  protected retry(): void {
    const id = this.challengeId();
    if (id) {
      void this.router.navigate(['/challenges', id, 'solve']);
    }
  }

  protected toDashboard(): void {
    void this.router.navigate(['/dashboard']);
  }

  protected back(): void {
    goBack(this.router);
  }
}
