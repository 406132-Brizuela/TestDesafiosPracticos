import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';
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
  MotorChallenge,
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
export class ChallengeCreateComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder).nonNullable;
  private readonly service = inject(ChallengeService);
  private readonly route = inject(ActivatedRoute);

  // El desafioId llega por redirect de Motor. Si se abre la pantalla de forma
  // directa, se selecciona explícitamente uno de los ejemplos devueltos por
  // el microservicio mock; G05 nunca genera un id propio.
  protected readonly desafioId = signal<string | null>(
    this.route.snapshot.queryParamMap.get('desafioId'),
  );

  protected readonly motorChallenges = signal<MotorChallenge[]>([]);
  protected readonly loadingMotor = signal(true);
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

  ngOnInit(): void {
    this.service.findMotorChallenges().subscribe({
      next: (challenges) => {
        this.motorChallenges.set(challenges);
        const requestedId = this.desafioId();
        const selected =
          challenges.find((challenge) => challenge.id === requestedId) ?? challenges[0];
        if (selected) {
          this.selectMotorChallenge(selected.id);
        } else {
          this.error.set('Motor no devolvió desafíos de ejemplo.');
        }
        this.loadingMotor.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.loadingMotor.set(false);
        this.error.set(this.errorMessage(error));
      },
    });
  }

  protected selectMotorChallenge(id: string): void {
    const selected = this.motorChallenges().find((challenge) => challenge.id === id);
    if (!selected) {
      return;
    }
    this.desafioId.set(selected.id);
    this.form.patchValue({ title: selected.title, difficulty: selected.difficulty });
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
    const desafioId = this.desafioId();
    if (!desafioId) {
      this.error.set('Seleccioná un desafío proveniente de Motor.');
      return;
    }
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
      desafioId,
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
      return 'No se pudo conectar con el backend.';
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
