import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { PracticalChallengeResponse } from '../challenge-create/challenge.models';
import { ChallengeService } from '../challenge-create/challenge.service';

@Component({
  selector: 'app-challenge-detail',
  imports: [RouterLink],
  templateUrl: './challenge-detail.component.html',
  styleUrl: './challenge-detail.component.css',
})
export class ChallengeDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(ChallengeService);

  protected readonly challenge = signal<PracticalChallengeResponse | null>(null);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.error.set('No se indicó qué desafío consultar.');
      return;
    }

    this.service.findById(id).subscribe({
      next: (challenge) => this.challenge.set(challenge),
      error: (error: HttpErrorResponse) => {
        this.error.set(
          error.status === 404 ? 'El desafío no existe.' : 'No se pudo cargar el detalle.',
        );
      },
    });
  }
}
