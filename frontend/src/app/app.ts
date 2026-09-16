import { Component, OnInit, signal } from '@angular/core';
import { EngineService } from './engine.service';
import { MonacoEditorComponent } from './monaco-editor.component';
import { ChallengeResponse, CorrectionDimension, EvaluationResult } from './models';

@Component({
  selector: 'app-root',
  imports: [MonacoEditorComponent],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App implements OnInit {
  protected readonly challenge = signal<ChallengeResponse | null>(null);
  protected readonly resultado = signal<EvaluationResult | null>(null);
  protected readonly corrigiendo = signal(false);
  protected readonly error = signal<string | null>(null);

  protected codigo = '';

  constructor(private readonly engineService: EngineService) {}

  ngOnInit(): void {
    this.engineService.getSampleChallenge().subscribe({
      next: (challenge) => {
        this.challenge.set(challenge);
        this.codigo = challenge.starterCode;
      },
      error: () => {
        this.error.set('No se pudo cargar el desafío. ¿Está corriendo el backend en el puerto 8080?');
      },
    });
  }

  enviarACorregir(): void {
    const challenge = this.challenge();
    if (!challenge) {
      return;
    }

    this.corrigiendo.set(true);
    this.error.set(null);
    this.resultado.set(null);

    this.engineService
      .evaluate({
        submissionId: crypto.randomUUID(),
        challengeId: challenge.id,
        lenguaje: challenge.lenguaje,
        code: this.codigo,
        profileId: 'default',
      })
      .subscribe({
        next: (resultado) => {
          this.resultado.set(resultado);
          this.corrigiendo.set(false);
        },
        error: () => {
          this.error.set('Ocurrió un error al corregir la entrega.');
          this.corrigiendo.set(false);
        },
      });
  }

  protected correctness(): CorrectionDimension | null {
    return this.resultado()?.dimensions.find((d) => d.dimension === 'correctness') ?? null;
  }

  protected casosFallados(): string[] {
    const fallados = this.correctness()?.evidence?.['fallados'];
    return Array.isArray(fallados) ? (fallados as string[]) : [];
  }

  protected qualityPercent(): number {
    const quality = this.resultado()?.quality;
    return quality === null || quality === undefined ? 0 : Math.max(0, Math.min(100, quality));
  }
}
