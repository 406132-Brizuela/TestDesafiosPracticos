import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import {
  AttemptDetailResponse,
  TutorMessageResponse,
  TutorSessionResponse,
} from '../challenge-create/challenge.models';
import { ChallengeService } from '../challenge-create/challenge.service';
import { MonacoEditorComponent } from '../monaco-editor.component';

@Component({
  selector: 'app-attempt-resolve',
  imports: [FormsModule, MonacoEditorComponent, RouterLink],
  templateUrl: './attempt-resolve.component.html',
  styleUrl: './attempt-resolve.component.css',
})
export class AttemptResolveComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(ChallengeService);

  protected readonly attempt = signal<AttemptDetailResponse | null>(null);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly confirmation = signal<string | null>(null);
  protected readonly tutorOpen = signal(true);
  protected readonly tutorLoading = signal(false);
  protected readonly tutorSession = signal<TutorSessionResponse | null>(null);
  protected readonly tutorError = signal<string | null>(null);
  protected readonly tutorMessages = signal<TutorMessageResponse[]>([]);
  protected readonly tutorSending = signal(false);
  protected readonly tutorMessageError = signal<string | null>(null);
  protected tutorMessage = '';

  // Contenido actual del editor. Se inicializa con draftCode ?? starterCode
  // al cargar el intento (ver loadAttempt) y se actualiza con cada cambio
  // del usuario vía (valueChange).
  protected code = '';

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.error.set('No se indicó qué intento resolver.');
      this.loading.set(false);
      return;
    }
    this.loadAttempt(id);
  }

  protected onCodeChange(value: string): void {
    this.code = value;
  }

  protected saveDraft(): void {
    const current = this.attempt();
    if (!current || this.saving()) {
      return;
    }

    this.saving.set(true);
    this.error.set(null);
    this.confirmation.set(null);

    this.service.saveDraft(current.id, this.code).subscribe({
      next: (updated) => {
        this.attempt.set(updated);
        this.saving.set(false);
        this.confirmation.set(`Borrador guardado a las ${this.formatNow()}.`);
      },
      error: (error: HttpErrorResponse) => {
        this.saving.set(false);
        this.error.set(this.saveErrorMessage(error));
      },
    });
  }

  protected toggleTutor(): void {
    this.tutorOpen.update((open) => !open);
  }

  protected retryTutor(): void {
    const current = this.attempt();
    if (current) {
      this.createTutorSession(current.id);
    }
  }

  protected sendTutorMessage(): void {
    const current = this.attempt();
    const session = this.tutorSession();
    const content = this.tutorMessage.trim();
    if (!current || !session || !content || this.tutorSending()) {
      return;
    }

    const studentMessage: TutorMessageResponse = {
      messageId: `local-${crypto.randomUUID()}`,
      sessionId: session.sessionId,
      role: 'STUDENT',
      content,
      createdAt: new Date().toISOString(),
    };
    this.tutorMessages.update((messages) => [...messages, studentMessage]);
    this.tutorMessage = '';
    this.tutorSending.set(true);
    this.tutorMessageError.set(null);

    this.service.sendTutorMessage(current.id, content, this.code).subscribe({
      next: (message) => {
        this.tutorMessages.update((messages) => [...messages, message]);
        this.tutorSending.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.tutorSending.set(false);
        this.tutorMessageError.set(this.tutorMessageErrorMessage(error));
      },
    });
  }

  private loadAttempt(id: string): void {
    this.loading.set(true);
    this.error.set(null);

    this.service.getAttempt(id).subscribe({
      next: (attempt) => {
        this.attempt.set(attempt);
        this.code = attempt.draftCode ?? attempt.starterCode;
        this.loading.set(false);
        this.createTutorSession(attempt.id);
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(error.status === 404 ? 'El intento no existe.' : this.loadErrorMessage(error));
      },
    });
  }

  private createTutorSession(attemptId: string): void {
    this.tutorLoading.set(true);
    this.tutorError.set(null);

    this.service.createTutorSession(attemptId).subscribe({
      next: (session) => {
        this.tutorSession.set(session);
        this.tutorLoading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.tutorLoading.set(false);
        this.tutorError.set(this.tutorErrorMessage(error));
      },
    });
  }

  private formatNow(): string {
    return new Date().toLocaleTimeString('es-AR', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    });
  }

  private loadErrorMessage(error: HttpErrorResponse): string {
    if (error.status === 0) {
      return 'No se pudo conectar con el backend.';
    }
    if (error.status === 401 || error.status === 403) {
      return 'Tu usuario no tiene permiso para esta acción.';
    }
    if (typeof error.error?.message === 'string') {
      return error.error.message;
    }
    return 'No se pudo cargar el intento. Reintentá en unos segundos.';
  }

  private saveErrorMessage(error: HttpErrorResponse): string {
    if (error.status === 0) {
      return 'No se pudo conectar con el backend.';
    }
    if (error.status === 401 || error.status === 403) {
      return 'Tu usuario no tiene permiso para esta acción.';
    }
    if (typeof error.error?.message === 'string') {
      return error.error.message;
    }
    return 'No se pudo guardar el borrador. Lo escrito se conserva para que puedas reintentar.';
  }

  private tutorErrorMessage(error: HttpErrorResponse): string {
    if (error.status === 0) {
      return 'No se pudo conectar con el backend.';
    }
    if (error.status === 502) {
      return 'El servicio del tutor IA no está disponible.';
    }
    if (typeof error.error?.message === 'string') {
      return error.error.message;
    }
    return 'No se pudo iniciar la sesión del tutor.';
  }

  private tutorMessageErrorMessage(error: HttpErrorResponse): string {
    if (error.status === 400) {
      return 'El mensaje no es válido o es demasiado largo.';
    }
    if (error.status === 403) {
      return 'Este intento no pertenece a tu usuario.';
    }
    if (error.status === 409) {
      return typeof error.error?.message === 'string'
        ? error.error.message
        : 'La sesión del tutor no está disponible para este intento.';
    }
    if (error.status === 502 || error.status === 0) {
      return 'No se pudo contactar al tutor IA. Podés volver a intentarlo.';
    }
    return 'No se pudo enviar el mensaje.';
  }
}
