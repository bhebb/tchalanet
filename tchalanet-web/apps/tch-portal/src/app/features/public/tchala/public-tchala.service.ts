import { HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { TchBackendClient, TchPage } from '@tch/api';
import { Observable } from 'rxjs';

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
  private readonly backend = inject(TchBackendClient);

  search(lang: string, q: string | undefined, page = 0, limit = 24): Observable<TchPage<TchalaEntry>> {
    let params = new HttpParams()
      .set('lang', lang)
      .set('offset', String(page * limit))
      .set('limit', String(limit));
    if (q) params = params.set('q', q);

    return this.backend.get<TchPage<TchalaEntry>>('/public/tchala/search', { params });
  }

  byDream(lang: string, dream: string): Observable<TchalaEntry> {
    const params = new HttpParams().set('lang', lang).set('dream', dream);
    return this.backend.get<TchalaEntry>('/public/tchala/by-dream', { params });
  }

  byNumber(lang: string, number: number, page = 0, limit = 20): Observable<TchPage<TchalaEntry>> {
    const params = new HttpParams()
      .set('lang', lang)
      .set('number', String(number))
      .set('offset', String(page * limit))
      .set('limit', String(limit));
    return this.backend.get<TchPage<TchalaEntry>>('/public/tchala/by-number', { params });
  }

  suggestionStatus(): Observable<TchalaSuggestionStatus> {
    return this.backend.get<TchalaSuggestionStatus>('/public/tchala/suggestions/status');
  }

  submitSuggestion(body: TchalaSuggestionRequest): Observable<TchalaSuggestionResult> {
    return this.backend.post<TchalaSuggestionResult>('/public/tchala/suggestions', body);
  }
}
