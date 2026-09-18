import { Injectable } from '@angular/core';
import type { JavaChallengeResponse, JavaEvaluationRequest, JavaEvaluationResult } from '../challenge-types';

/**
 * Cliente del backend Java real (Tema 05 / G05), NO del server Node del monaco.
 * A diferencia de CompileService (que le pega a /api -> localhost:3100 vía proxy),
 * este pega directo a localhost:8080: es otro origen, así que depende del CORS que
 * el backend habilita para localhost:4200 en perfil local (config/WebConfig.java).
 */
const JAVA_BACKEND_BASE = 'http://localhost:8080';

export class EngineHttpError extends Error {}

@Injectable({ providedIn: 'root' })
export class EngineService {
  private readonly base = JAVA_BACKEND_BASE;

  getChallenge(challengeId: string): Promise<JavaChallengeResponse> {
    return this.fetchJson<JavaChallengeResponse>(`/challenges/${encodeURIComponent(challengeId)}`);
  }

  evaluate(request: JavaEvaluationRequest): Promise<JavaEvaluationResult> {
    return this.fetchJson<JavaEvaluationResult>('/engine/evaluate', 'POST', request);
  }

  private async fetchJson<T>(path: string, method: 'GET' | 'POST' = 'GET', body?: unknown): Promise<T> {
    let response: Response;
    try {
      response = await fetch(`${this.base}${path}`, {
        method,
        headers: body !== undefined ? { 'Content-Type': 'application/json' } : undefined,
        body: body !== undefined ? JSON.stringify(body) : undefined,
        signal: AbortSignal.timeout(20000),
      });
    } catch {
      throw new EngineHttpError(
        `No se pudo conectar con el backend Java en ${this.base}. ¿Está levantado (perfil local, puerto 8080)?`,
      );
    }

    const text = await response.text();
    let data: unknown = {};
    if (text) {
      try {
        data = JSON.parse(text);
      } catch {
        data = { raw: text };
      }
    }

    if (!response.ok) {
      const record = data as { detail?: string; message?: string; title?: string } | null;
      const detail = record?.detail ?? record?.message ?? record?.title ?? `HTTP ${response.status}`;
      throw new EngineHttpError(detail);
    }

    return data as T;
  }
}
