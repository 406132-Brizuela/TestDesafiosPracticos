export type Difficulty = 'BASICO' | 'MEDIO' | 'AVANZADO';
export type ChallengeType = 'ALGORITMOS_CON_PRUEBAS_AUTOMATICAS';
export type ProgrammingLanguage = 'JAVA';
export type TestVisibility = 'PUBLICO' | 'PRIVADO';

export interface PracticalTestCaseRequest {
  name: string;
  input: string;
  expectedOutput: string;
  visibility: TestVisibility;
}

export interface PracticalChallengeRequest {
  desafioId: string;
  title: string;
  statement: string;
  difficulty: Difficulty;
  type: ChallengeType;
  language: ProgrammingLanguage;
  starterCode: string;
  testCases: PracticalTestCaseRequest[];
}

export interface PracticalTestCaseResponse extends PracticalTestCaseRequest {
  id: number;
}

export interface PracticalChallengeResponse extends PracticalChallengeRequest {
  id: string;
  testCases: PracticalTestCaseResponse[];
}

export interface PracticalChallengeSummary {
  id: string;
  title: string;
  difficulty: Difficulty | null;
  creationDatetime: string;
  testCount: number;
}

export interface AttemptCreateRequest {
  intentoId: string;
  practicalChallengeId: string;
}

export interface AttemptResponse {
  id: string;
  practicalChallengeId: string;
  challengeTitle: string;
  creationDatetime: string;
  status: 'INICIADO' | 'ENTREGADO';
}
