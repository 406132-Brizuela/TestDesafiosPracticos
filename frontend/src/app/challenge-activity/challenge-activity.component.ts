import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import {
  AttemptCreateRequest,
  AttemptResponse,
  PracticalChallengeSummary,
} from '../challenge-create/challenge.models';
import { ChallengeService } from '../challenge-create/challenge.service';

type ActivityTab = 'challenges' | 'attempts';

@Component({
  selector: 'app-challenge-activity',
  imports: [DatePipe, RouterLink],
  templateUrl: './challenge-activity.component.html',
  styleUrl: './challenge-activity.component.css',
})
export class ChallengeActivityComponent implements OnInit {
  private readonly service = inject(ChallengeService);

  protected readonly activeTab = signal<ActivityTab>('challenges');
  protected readonly challenges = signal<PracticalChallengeSummary[]>([]);
  protected readonly attempts = signal<AttemptResponse[]>([]);
  protected readonly loading = signal(true);
  protected readonly startingChallengeId = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly confirmation = signal<string | null>(null);

  ngOnInit(): void {
    this.loadActivity();
  }

  protected selectTab(tab: ActivityTab): void {
    this.activeTab.set(tab);
    this.error.set(null);
  }

  protected startAttempt(challenge: PracticalChallengeSummary): void {
    if (this.startingChallengeId()) {
      return;
    }

    this.startingChallengeId.set(challenge.id);
    this.error.set(null);
    this.confirmation.set(null);
    // El intentoId real lo asigna Motor al abrir/registrar el intento. Todavía no
    // integramos Motor, así que generamos un UUID local como stand-in temporal
    // (mismo patrón que desafioId en challenge-create.component.ts).
    const request: AttemptCreateRequest = {
      intentoId: crypto.randomUUID(),
      practicalChallengeId: challenge.id,
    };
    this.service.startAttempt(request).subscribe({
      next: (attempt) => {
        this.attempts.update((current) => [attempt, ...current]);
        this.startingChallengeId.set(null);
        this.confirmation.set(`Intento iniciado para “${challenge.title}”.`);
        this.activeTab.set('attempts');
      },
      error: (error: HttpErrorResponse) => {
        this.startingChallengeId.set(null);
        this.error.set(this.errorMessage(error));
      },
    });
  }

  protected loadActivity(): void {
    this.loading.set(true);
    this.error.set(null);
    forkJoin({
      challenges: this.service.findAll(),
      attempts: this.service.findAttempts(),
    }).subscribe({
      next: ({ challenges, attempts }) => {
        this.challenges.set(challenges);
        this.attempts.set(attempts);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(this.errorMessage(error));
        this.loading.set(false);
      },
    });
  }

  private errorMessage(error: HttpErrorResponse): string {
    if (error.status === 0) {
      return 'No se pudo conectar con el backend.';
    }
    if (error.status === 401 || error.status === 403) {
      return 'Tu usuario no tiene permiso para consultar esta actividad.';
    }
    return 'No se pudo cargar la actividad. Reintentá en unos segundos.';
  }
}
