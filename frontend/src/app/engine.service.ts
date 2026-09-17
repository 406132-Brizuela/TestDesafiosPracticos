import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ChallengeResponse, EvaluationRequest, EvaluationResult } from './models';

@Injectable({ providedIn: 'root' })
export class EngineService {
  private readonly baseUrl = 'http://localhost:8080';

  constructor(private readonly http: HttpClient) {}

  getSampleChallenge(): Observable<ChallengeResponse> {
    return this.http.get<ChallengeResponse>(`${this.baseUrl}/challenges/sample`);
  }

  evaluate(request: EvaluationRequest): Observable<EvaluationResult> {
    return this.http.post<EvaluationResult>(`${this.baseUrl}/engine/evaluate`, request);
  }
}
