import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AttemptDetailResponse } from '../challenge-create/challenge.models';
import { ChallengeService } from '../challenge-create/challenge.service';
import { MonacoEditorComponent } from '../monaco-editor.component';

@Component({
  selector: 'app-attempt-resolve',
  imports: [MonacoEditorComponent, RouterLink],
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

  private loadAttempt(id: string): void {
    this.loading.set(true);
    this.error.set(null);

    this.service.getAttempt(id).subscribe({
      next: (attempt) => {
        this.attempt.set(attempt);
        this.code = attempt.draftCode ?? attempt.starterCode;
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(error.status === 404 ? 'El intento no existe.' : this.loadErrorMessage(error));
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
      return 'No se pudo conectar con el backend en localhost:8080.';
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
      return 'No se pudo conectar con el backend en localhost:8080.';
    }
    if (error.status === 401 || error.status === 403) {
      return 'Tu usuario no tiene permiso para esta acción.';
    }
    if (typeof error.error?.message === 'string') {
      return error.error.message;
    }
    return 'No se pudo guardar el borrador. Lo escrito se conserva para que puedas reintentar.';
  }
}
