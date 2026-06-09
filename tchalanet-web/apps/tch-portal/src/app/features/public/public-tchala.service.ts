import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { ApiResponse, TchPage, unwrapApiResponse } from '@tch/api';

export interface TchalaEntry {
  readonly id: string;
  readonly lang: string;
  readonly dream: string;
  readonly numbers: readonly number[];
  readonly note: string | null;
  readonly status: string;
  readonly source: string;
  readonly conflictWithEntryId: string | null;
  readonly canonicalEntryId: string | null;
}

export interface TchalaSuggestionStatus {
  readonly open: boolean;
  readonly pendingCount: number;
  readonly maxPending: number;
}

export interface TchalaSuggestionRequest {
  readonly lang: string;
  readonly dream: string;
  readonly numbers: string;
  readonly note?: string;
}

export interface TchalaSuggestionResult {
  readonly entryId: string;
  readonly status: string;
  readonly conflictsWithCanonical: boolean;
  readonly conflictWithEntryId: string | null;
}

@Injectable({ providedIn: 'root' })
export class PublicTchalaService {
  private readonly http = inject(HttpClient);

  search(lang: string, q: string | undefined, page = 0, limit = 24): Observable<TchPage<TchalaEntry>> {
    let params = new HttpParams()
      .set('lang', lang)
      .set('offset', String(page * limit))
      .set('limit', String(limit));
    if (q) params = params.set('q', q);

    return this.http
      .get<ApiResponse<TchPage<TchalaEntry>>>('/api/v1/public/tchala/search', { params })
      .pipe(map(unwrapApiResponse));
  }

  byDream(lang: string, dream: string): Observable<TchalaEntry> {
    const params = new HttpParams().set('lang', lang).set('dream', dream);

    return this.http
      .get<ApiResponse<TchalaEntry>>('/api/v1/public/tchala/by-dream', { params })
      .pipe(map(unwrapApiResponse));
  }

  byNumber(lang: string, number: number, page = 0, limit = 20): Observable<TchPage<TchalaEntry>> {
    const params = new HttpParams()
      .set('lang', lang)
      .set('number', String(number))
      .set('offset', String(page * limit))
      .set('limit', String(limit));

    return this.http
      .get<ApiResponse<TchPage<TchalaEntry>>>('/api/v1/public/tchala/by-number', { params })
      .pipe(map(unwrapApiResponse));
  }

  suggestionStatus(): Observable<TchalaSuggestionStatus> {
    return this.http
      .get<ApiResponse<TchalaSuggestionStatus>>('/api/v1/public/tchala/suggestions/status')
      .pipe(map(unwrapApiResponse));
  }

  submitSuggestion(body: TchalaSuggestionRequest): Observable<TchalaSuggestionResult> {
    return this.http
      .post<ApiResponse<TchalaSuggestionResult>>('/api/v1/public/tchala/suggestions', body)
      .pipe(map(unwrapApiResponse));
  }
}
