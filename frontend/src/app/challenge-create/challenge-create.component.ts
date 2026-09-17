import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import {
  FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import {
  ChallengeType,
  Difficulty,
  PracticalChallengeRequest,
  PracticalChallengeResponse,
  ProgrammingLanguage,
  TestVisibility,
} from './challenge.models';
import { ChallengeService } from './challenge.service';

const NON_BLANK = Validators.pattern(/\S/);

type TestCaseForm = FormGroup<{
  name: FormControl<string>;
  input: FormControl<string>;
  expectedOutput: FormControl<string>;
  visibility: FormControl<TestVisibility>;
}>;

@Component({
  selector: 'app-challenge-create',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './challenge-create.component.html',
  styleUrl: './challenge-create.component.css',
})
export class ChallengeCreateComponent {
  private readonly formBuilder = inject(FormBuilder).nonNullable;
  private readonly service = inject(ChallengeService);
  private readonly route = inject(ActivatedRoute);

  // El desafioId real llega por query param (?desafioId=...) en el redirect
  // que hace Motor al abrir esta pantalla. Si no vino (todavía no integramos
  // Motor), generamos un UUID local como stand-in temporal para poder seguir
  // probando el flujo: en producción siempre debería venir por query param.
  private readonly desafioId =
    this.route.snapshot.queryParamMap.get('desafioId') ?? crypto.randomUUID();

  protected readonly difficulties: Difficulty[] = ['BASICO', 'MEDIO', 'AVANZADO'];
  protected readonly visibilities: TestVisibility[] = ['PUBLICO', 'PRIVADO'];
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly confirmation = signal<string | null>(null);
  protected readonly savedChallenge = signal<PracticalChallengeResponse | null>(null);

  protected readonly form = this.formBuilder.group({
    title: this.formBuilder.control('', [Validators.required, NON_BLANK]),
    statement: this.formBuilder.control('', [Validators.required, NON_BLANK]),
    difficulty: this.formBuilder.control<Difficulty>('BASICO', [Validators.required]),
    type: this.formBuilder.control<ChallengeType>('ALGORITMOS_CON_PRUEBAS_AUTOMATICAS', [
      Validators.required,
    ]),
    language: this.formBuilder.control<ProgrammingLanguage>('JAVA', [Validators.required]),
    starterCode: this.formBuilder.control(''),
    testCases: this.formBuilder.array<TestCaseForm>([], [Validators.minLength(1)]),
  });

  constructor() {
    this.addTestCase();
  }

  protected get testCases(): FormArray<TestCaseForm> {
    return this.form.controls.testCases;
  }

  protected addTestCase(): void {
    if (this.saving()) {
      return;
    }
    this.testCases.push(this.newTestCase());
  }

  protected removeTestCase(index: number): void {
    if (this.saving() || this.testCases.length === 1) {
      return;
    }
    this.testCases.removeAt(index);
  }

  protected save(): void {
    if (this.saving()) {
      return;
    }

    this.form.markAllAsTouched();
    if (this.form.invalid) {
      this.error.set('Revisá los campos marcados antes de guardar.');
      return;
    }

    this.saving.set(true);
    this.error.set(null);
    this.confirmation.set(null);
    this.savedChallenge.set(null);

    const request: PracticalChallengeRequest = {
      ...this.form.getRawValue(),
      desafioId: this.desafioId,
    };
    this.service.create(request).subscribe({
      next: (created) => this.recoverCreatedChallenge(created),
      error: (error: HttpErrorResponse) => {
        this.error.set(this.errorMessage(error));
        this.saving.set(false);
      },
    });
  }

  protected isInvalid(control: FormControl<string>): boolean {
    return control.invalid && control.touched;
  }

  private recoverCreatedChallenge(created: PracticalChallengeResponse): void {
    this.service
      .findById(created.id)
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (recovered) => {
          this.savedChallenge.set(recovered);
          this.confirmation.set('Desafío guardado y recuperado desde H2.');
        },
        error: () => {
          this.savedChallenge.set(created);
          this.confirmation.set(
            'El desafío se guardó, pero no se pudo volver a consultar el detalle.',
          );
        },
      });
  }

  private errorMessage(error: HttpErrorResponse): string {
    if (error.status === 0) {
      return 'No se pudo conectar con el backend en localhost:8080.';
    }
    if (error.status === 401 || error.status === 403) {
      return 'Tu usuario no tiene permiso para crear desafíos.';
    }
    if (typeof error.error?.message === 'string') {
      return error.error.message;
    }
    return 'No se pudo guardar el desafío. Lo escrito se conserva para que puedas reintentar.';
  }

  private newTestCase(): TestCaseForm {
    return this.formBuilder.group({
      name: this.formBuilder.control('', [Validators.required, NON_BLANK]),
      input: this.formBuilder.control(''),
      expectedOutput: this.formBuilder.control(''),
      visibility: this.formBuilder.control<TestVisibility>('PUBLICO', [Validators.required]),
    });
  }
}
