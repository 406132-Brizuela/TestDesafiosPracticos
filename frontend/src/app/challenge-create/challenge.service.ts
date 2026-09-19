import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AttemptCreateRequest,
  AttemptDetailResponse,
  AttemptDraftSaveRequest,
  AttemptResponse,
  MotorChallenge,
  PracticalChallengeRequest,
  PracticalChallengeResponse,
  PracticalChallengeSummary,
  TutorSessionResponse,
  TutorMessageResponse,
} from './challenge.models';

@Injectable({ providedIn: 'root' })
export class ChallengeService {
  private readonly baseUrl = '/api/desafiospracticos';
  private readonly endpoint = `${this.baseUrl}/desafios`;

  constructor(private readonly http: HttpClient) {}

  create(request: PracticalChallengeRequest): Observable<PracticalChallengeResponse> {
    return this.http.post<PracticalChallengeResponse>(this.endpoint, request);
  }

  findById(id: string): Observable<PracticalChallengeResponse> {
    return this.http.get<PracticalChallengeResponse>(`${this.endpoint}/${id}`);
  }

  findAll(): Observable<PracticalChallengeSummary[]> {
    return this.http.get<PracticalChallengeSummary[]>(this.endpoint);
  }

  findMotorChallenges(): Observable<MotorChallenge[]> {
    return this.http.get<MotorChallenge[]>(`${this.baseUrl}/motor/desafios`);
  }

  findAttempts(): Observable<AttemptResponse[]> {
    return this.http.get<AttemptResponse[]>(`${this.baseUrl}/intentos`);
  }

  startAttempt(request: AttemptCreateRequest): Observable<AttemptResponse> {
    return this.http.post<AttemptResponse>(`${this.baseUrl}/intentos`, request);
  }

  getAttempt(id: string): Observable<AttemptDetailResponse> {
    return this.http.get<AttemptDetailResponse>(`${this.baseUrl}/intentos/${id}`);
  }

  saveDraft(id: string, content: string): Observable<AttemptDetailResponse> {
    const request: AttemptDraftSaveRequest = { content };
    return this.http.put<AttemptDetailResponse>(`${this.baseUrl}/intentos/${id}/borrador`, request);
  }

  createTutorSession(attemptId: string): Observable<TutorSessionResponse> {
    return this.http.post<TutorSessionResponse>(
      `${this.baseUrl}/intentos/${attemptId}/tutor/sesion`,
      {},
    );
  }

  sendTutorMessage(
    attemptId: string,
    content: string,
    currentCode: string,
  ): Observable<TutorMessageResponse> {
    return this.http.post<TutorMessageResponse>(
      `${this.baseUrl}/intentos/${attemptId}/tutor/mensajes`,
      { content, currentCode },
    );
  }
}
