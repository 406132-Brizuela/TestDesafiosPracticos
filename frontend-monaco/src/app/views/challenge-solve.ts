import {
  Component,
  HostListener,
  Input,
  OnDestroy,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { Router } from '@angular/router';
import { MonacoEditor } from '../monaco-editor';
import { ChatPanel } from '../chat-panel';
import type {
  Challenge,
  ChatMessage,
  FailingTest,
  IntegrityEvent,
  JavaCorrectionDimension,
  JavaEngineProfileId,
  Verdict,
} from '../challenge-types';
import {
  challengeFromJavaResponse,
  correctnessSummaryOf,
  feedbackFromJavaResult,
  submissionFromJavaResult,
  verdictFromJavaResult,
} from '../challenge-types';
import type { ProjectFile } from '../projects';
import {
  clearDraft,
  fileNameOf,
  goBack,
  isTestFilePath,
  loadDraft,
  monacoLanguageOf,
  riskOf,
  saveDraft,
} from '../shared';
import { BannerService } from '../services/banner.service';
import { EngineService } from '../services/engine.service';
import { SessionService } from '../services/session.service';

interface CheckState {
  verdict: Verdict;
  feedback: string;
  failingTest?: FailingTest | null;
  tests?: { passed: number; total: number } | null;
  quality?: number | null;
  dimensions?: JavaCorrectionDimension[];
}

function formatCountdown(ms: number): string {
  const totalSeconds = Math.max(0, Math.floor(ms / 1000));
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  const pad = (value: number): string => String(value).padStart(2, '0');
  return hours > 0
    ? `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`
    : `${pad(minutes)}:${pad(seconds)}`;
}

@Component({
  selector: 'app-challenge-solve',
  standalone: true,
  imports: [MonacoEditor, ChatPanel],
  template: `
    @if (challenge(); as challenge) {
      <section class="view student-ide">
        <header class="student-head">
          <button type="button" class="btn btn-secondary" (click)="back()">← Volver</button>
          <h2>{{ challenge.title }}</h2>
          <span class="tag">{{ challenge.subtype }}</span>
          <span class="badge badge-diff badge-difficulty-{{ challenge.difficulty }}">{{
            challenge.difficulty
          }}</span>
          @if (challenge.configuration.runtime) {
            <span class="tag">{{ challenge.configuration.runtime }}</span>
          }
          @if (session.role() !== 'ALUMNO') {
            <span class="muted small mono">{{ challenge.courseCohortId }}</span>
          }
        </header>

        @if (challenge.statement) {
          <p class="muted statement">{{ challenge.statement }}</p>
        }

        <div class="student-workspace">
          <div class="ide-shell">
            <div class="ide-main">
              <div class="workspace-tabs">
                <div class="tabs-zone">
                  @for (path of filePaths(); track path) {
                    <span
                      class="file-tab mono"
                      [class.active]="path === activePath()"
                      (click)="setActivePath(path)"
                    >
                      {{ fileNameOf(path) }}
                    </span>
                  }
                </div>
                <div class="tabs-center">
                  @if (countdownLabel()) {
                    <span class="countdown mono" title="Tiempo límite restante">
                      Tiempo restante: {{ countdownLabel() }}
                    </span>
                  }
                </div>
                <div class="tabs-actions">
                  <label class="muted small" title="Rúbrica de evaluación del engine">
                    Perfil
                    <select [value]="profileId()" (change)="onProfileChange($event)">
                      <option value="introductorio">introductorio</option>
                      <option value="avanzado">avanzado</option>
                    </select>
                  </label>
                  <button
                    type="button"
                    class="btn btn-primary"
                    [disabled]="busy()"
                    (click)="compile()"
                    title="Evaluar contra el engine (F5)"
                  >
                    Compilar <kbd>F5</kbd>
                  </button>
                  <button
                    type="button"
                    class="btn btn-success"
                    [disabled]="busy()"
                    (click)="submit()"
                    title="Enviar resolución (Ctrl+S)"
                  >
                    Enviar <kbd>Ctrl+S</kbd>
                  </button>
                </div>
              </div>

              <app-monaco-editor
                class="student-editor"
                [value]="activeContent()"
                [language]="monacoLanguageOf(activePath())"
                (valueChange)="onEdit($event)"
                (integrityEvent)="recordIntegrityEvent($event)"
              />
              <div class="output-box">
                @for (line of outputLines(); track $index) {
                  <div class="line mono">{{ line }}</div>
                }
              </div>
              @if (check(); as check) {
                <div class="check-strip">
                  <div class="verdict verdict-{{ check.verdict }}">
                    <strong>{{ check.verdict }}</strong>
                    <span>{{ check.feedback }}</span>
                    @if (check.tests; as tests) {
                      <span class="test-summary">
                        Tests superados: <strong>{{ tests.passed }} de {{ tests.total }}</strong>
                      </span>
                    }
                    @if (check.quality !== undefined && check.quality !== null) {
                      <span class="test-summary">Calidad: <strong>{{ check.quality }}/100</strong></span>
                    }
                    @if (check.dimensions; as dims) {
                      <div class="fail-block">
                        @for (dim of dims; track dim.dimension) {
                          <div class="cmp-line">
                            <span class="cmp-label">{{ dim.dimension }}</span>
                            <span class="mono cmp-value">{{ dim.subScore ?? '—' }} (peso {{ dim.weight }})</span>
                          </div>
                        }
                      </div>
                    }
                    @if (check.failingTest; as fail) {
                      <div class="fail-block">
                        <div class="fail-head">
                          Test "{{ fail.name }}" — {{ fail.status }}
                          @if (fail.input) {
                            <span>
                              · entrada: <span class="mono">{{ fail.input }}</span></span
                            >
                          }
                        </div>
                        <div class="cmp-line">
                          <span class="cmp-label">Salida esperada</span>
                          <span class="mono cmp-value">{{ fail.expected }}</span>
                        </div>
                        <div class="cmp-line">
                          <span class="cmp-label">Su salida</span>
                          <span class="mono cmp-value">{{ fail.actual }}</span>
                        </div>
                      </div>
                    }
                  </div>
                </div>
              }
            </div>
          </div>
        </div>

        <app-chat-panel
          [challengeId]="challenge.challengeId"
          [riskLevel]="riskOf(challenge)"
          [allowReset]="canCreateConversation()"
          (transcriptChange)="onTranscript($event)"
        />
      </section>
    } @else if (notFound()) {
      <section class="view">
        <p class="muted">No encontramos este desafío.</p>
        <div class="actions-bar">
          <button type="button" class="btn btn-secondary" (click)="back()">← Volver</button>
        </div>
      </section>
    }
  `,
})
export class ChallengeSolveComponent implements OnInit, OnDestroy {
  @Input() id?: string;

  private readonly router = inject(Router);

  protected readonly session = inject(SessionService);
  protected readonly banner = inject(BannerService);
  private readonly engineService = inject(EngineService);

  protected readonly riskOf = riskOf;
  protected readonly fileNameOf = fileNameOf;
  protected readonly monacoLanguageOf = monacoLanguageOf;

  protected readonly challenge = signal<Challenge | null>(null);
  protected readonly notFound = signal(false);
  protected readonly busy = signal(false);
  protected readonly files = signal<ProjectFile[]>([]);
  protected readonly activePath = signal('');
  protected readonly outputLines = signal<string[]>([]);
  protected readonly check = signal<CheckState | null>(null);
  protected readonly profileId = signal<JavaEngineProfileId>('introductorio');
  protected transcript: ChatMessage[] = [];
  protected readonly integrityEvents = signal<IntegrityEvent[]>([]);
  protected readonly countdownMs = signal<number | null>(null);
  protected readonly countdownLabel = computed(() => {
    const ms = this.countdownMs();
    return ms == null ? '' : formatCountdown(ms);
  });

  private readonly countdownStorageKey = 'mmv-hack-';
  private countdownTimer: ReturnType<typeof setInterval> | null = null;
  private countdownDeadline = 0;

  private windowBlurred = false;

  private readonly onVisibilityChange = (): void => {
    if (document.hidden) {
      this.recordWindowBlur();
    } else {
      this.recordWindowFocus();
    }
  };

  protected readonly filePaths = computed(() =>
    this.files()
      .filter((file) => !isTestFilePath(file.path))
      .map((file) => file.path),
  );
  protected readonly activeContent = computed(
    () => this.files().find((file) => file.path === this.activePath())?.content ?? '',
  );
  protected readonly canCreateConversation = computed(() => this.session.role() !== 'ALUMNO');

  @HostListener('window:keydown', ['$event'])
  protected onWindowKeydown(event: KeyboardEvent): void {
    if (event.key === 'F5') {
      event.preventDefault();
      void this.compile();
    } else if (event.ctrlKey && (event.key === 's' || event.key === 'S')) {
      event.preventDefault();
      void this.submit();
    }
  }

  @HostListener('window:blur')
  protected onWindowBlur(): void {
    this.recordWindowBlur();
  }

  @HostListener('window:focus')
  protected onWindowFocus(): void {
    this.recordWindowFocus();
  }

  ngOnInit(): void {
    document.addEventListener('visibilitychange', this.onVisibilityChange);
    void this.open();
  }

  ngOnDestroy(): void {
    document.removeEventListener('visibilitychange', this.onVisibilityChange);
    this.stopCountdown();
  }

  protected recordIntegrityEvent(event: IntegrityEvent): void {
    this.integrityEvents.update((events) => [...events, event]);
  }

  private recordWindowBlur(): void {
    if (this.windowBlurred) {
      return;
    }
    this.windowBlurred = true;
    this.recordIntegrityEvent({ type: 'WINDOW_BLUR', timestamp: new Date().toISOString() });
  }

  private recordWindowFocus(): void {
    if (!this.windowBlurred) {
      return;
    }
    this.windowBlurred = false;
    this.recordIntegrityEvent({ type: 'WINDOW_FOCUS', timestamp: new Date().toISOString() });
  }

  protected async open(): Promise<void> {
    if (!this.id) {
      this.notFound.set(true);
      return;
    }
    this.busy.set(true);
    try {
      // El desafío a resolver siempre viene del backend Java (creación de desafíos
      // G05), nunca del store Node del monaco — this.id es un desafioId real
      // (ej. "desafio-suma"), no uno de los seeds internos del monaco.
      const java = await this.engineService.getChallenge(this.id);
      const challenge = challengeFromJavaResponse(java);
      this.challenge.set(challenge);
      const baseFiles = (challenge.configuration.baseFiles ?? []).map((file) => ({ ...file }));
      const restored = loadDraft(this.id);
      this.files.set(restored ?? baseFiles);
      if (restored) {
        this.banner.show('Se restauró un borrador guardado de este desafío.');
      }
      saveDraft(this.id, this.payloadFiles());
      this.activePath.set(
        challenge.configuration.entry ?? challenge.configuration.baseFiles?.[0]?.path ?? '',
      );
      this.outputLines.set([]);
      this.check.set(null);
      this.transcript = [];
      this.integrityEvents.set([]);
      this.windowBlurred = false;
      this.startCountdown(challenge);
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      this.banner.show(`No se pudo abrir el desafío: ${message}`);
      this.notFound.set(true);
    } finally {
      this.busy.set(false);
    }
  }

  private startCountdown(challenge: Challenge): void {
    this.stopCountdown();
    this.countdownMs.set(null);
    if (
      challenge.subtype !== 'hackathon' ||
      challenge.durationMs == null ||
      challenge.durationMs <= 0
    ) {
      return;
    }
    const key = `${this.countdownStorageKey}${challenge.challengeId}`;
    let start = 0;
    try {
      const stored = sessionStorage.getItem(key);
      if (stored) {
        const parsed = JSON.parse(stored) as { start?: number; durationMs?: number } | null;
        if (
          parsed &&
          typeof parsed.start === 'number' &&
          parsed.durationMs === challenge.durationMs
        ) {
          start = parsed.start;
        }
      }
    } catch {
      start = 0;
    }
    if (start <= 0) {
      start = Date.now();
      try {
        sessionStorage.setItem(key, JSON.stringify({ start, durationMs: challenge.durationMs }));
      } catch {
        // sessionStorage no disponible: el contador no sobrevive a recargas.
      }
    }
    this.countdownDeadline = start + challenge.durationMs;
    this.stopCountdown();
    this.updateCountdown();
    this.countdownTimer = setInterval(() => this.updateCountdown(), 1000);
  }

  private stopCountdown(): void {
    if (this.countdownTimer !== null) {
      clearInterval(this.countdownTimer);
      this.countdownTimer = null;
    }
  }

  private updateCountdown(): void {
    const remaining = this.countdownDeadline - Date.now();
    if (remaining <= 0) {
      this.countdownMs.set(0);
      this.stopCountdown();
      void this.onCountdownExpired();
      return;
    }
    this.countdownMs.set(remaining);
  }

  private async onCountdownExpired(): Promise<void> {
    const challenge = this.challenge();
    if (!challenge) {
      return;
    }
    if (this.busy()) {
      setTimeout(() => void this.onCountdownExpired(), 500);
      return;
    }
    this.banner.show('Tiempo agotado: se envió su resolución automáticamente.');
    await this.submit();
  }

  protected back(): void {
    goBack(this.router);
  }

  protected onEdit(value: string): void {
    const path = this.activePath();
    const next = this.files().map((file) =>
      file.path === path ? { ...file, content: value } : file,
    );
    this.files.set(next);
    saveDraft(this.id ?? '', next);
  }

  protected setActivePath(path: string): void {
    if (!isTestFilePath(path) && this.files().some((file) => file.path === path)) {
      this.activePath.set(path);
    }
  }

  protected onTranscript(transcript: ChatMessage[]): void {
    this.transcript = transcript;
  }

  private payloadFiles(): ProjectFile[] {
    return this.files().map((file) => ({ ...file }));
  }

  private writeLine(line: string): void {
    this.outputLines.update((lines) => [...lines.slice(-500), line]);
  }

  protected onProfileChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.profileId.set(value === 'avanzado' ? 'avanzado' : 'introductorio');
  }

  /**
   * Único camino de ejecución/evaluación: POST /engine/evaluate al backend Java.
   * Ya no se llama al executor del server Node — ni para "Compilar" (antes
   * runExecution) ni para "Enviar" (antes submit) — así que el front no ejecuta
   * código localmente ni le pide nada al sandbox de Node para este flujo.
   */
  protected async compile(): Promise<void> {
    const challenge = this.challenge();
    if (!challenge || this.busy()) {
      return;
    }
    this.busy.set(true);
    this.outputLines.set([]);
    this.check.set(null);
    this.writeLine('> Evaluando con el engine…');
    try {
      const result = await this.engineService.evaluate({
        submissionId: crypto.randomUUID(),
        challengeId: challenge.challengeId,
        lenguaje: challenge.configuration.language,
        code: this.activeContent(),
        profileId: this.profileId(),
      });
      const feedback = feedbackFromJavaResult(result);
      this.writeLine(`> ${feedback}`);
      this.check.set({
        verdict: verdictFromJavaResult(result),
        feedback,
        failingTest: null,
        tests: correctnessSummaryOf(result),
        quality: result.quality,
        dimensions: result.dimensions,
      });
    } catch (error) {
      const message = this.engineErrorMessage(error);
      this.writeLine(`> ${message}`);
      this.check.set({ verdict: 'ERROR_TECNICO', feedback: message, failingTest: null });
    } finally {
      this.busy.set(false);
    }
  }

  protected async submit(): Promise<void> {
    const challenge = this.challenge();
    if (!challenge || this.busy()) {
      return;
    }
    this.busy.set(true);
    try {
      const result = await this.engineService.evaluate({
        submissionId: crypto.randomUUID(),
        challengeId: challenge.challengeId,
        lenguaje: challenge.configuration.language,
        code: this.activeContent(),
        profileId: this.profileId(),
      });
      const submission = submissionFromJavaResult(challenge, result);
      void this.router.navigate(['/challenges', challenge.challengeId, 'result'], {
        state: {
          payload: {
            submission,
            files: this.payloadFiles(),
            chatTranscript: this.transcript,
            integrityEvents: this.integrityEvents(),
          },
        },
      });
      sessionStorage.removeItem(`${this.countdownStorageKey}${challenge.challengeId}`);
      clearDraft(challenge.challengeId);
    } catch (error) {
      this.banner.show(`No se pudo enviar la resolución: ${this.engineErrorMessage(error)}`);
    } finally {
      this.busy.set(false);
    }
  }

  private engineErrorMessage(error: unknown): string {
    return error instanceof Error ? error.message : String(error);
  }
}
