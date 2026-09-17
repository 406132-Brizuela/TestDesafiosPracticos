export interface ChallengeResponse {
  id: string;
  consigna: string;
  lenguaje: string;
  starterCode: string;
}

export interface EvaluationRequest {
  submissionId: string;
  challengeId: string;
  lenguaje: string;
  code: string;
  profileId: string;
}

export type EvaluationStatus = 'COMPLETED' | 'NO_COMPILE' | 'PARTIAL_PENDING';
export type Verdict = 'APPROVED' | 'NOT_APPROVED' | 'PENDING';
export type DimensionState = 'OK' | 'PENDING_SANDBOX';

export interface CorrectionDimension {
  dimension: string;
  subScore: number | null;
  weight: number;
  contribution: number;
  source: string;
  state: DimensionState;
  evidence: Record<string, unknown>;
}

export interface EvaluationResult {
  submissionId: string;
  profileId: string;
  profileVersion: number;
  engineVersion: string;
  status: EvaluationStatus;
  quality: number | null;
  suggestedVerdict: Verdict;
  approvalThreshold: number;
  dimensions: CorrectionDimension[];
  feedbackAlumno: string[];
}
