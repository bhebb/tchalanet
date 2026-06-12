import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';

import { PageRuntimeResponse } from '@tch/page-model';

import { PublicBootstrapResponse } from './public-bootstrap.model';

export interface PublicFallbackBundle {
  readonly capturedAt: string;
  readonly schemaVersion: number;
  readonly publicBootstrap: PublicBootstrapResponse;
  readonly pagePayload: PageRuntimeResponse;
}

const FALLBACK_PATH = '/assets/fallback/public-bootstrap-fallback.fr.json';

@Injectable({ providedIn: 'root' })
export class PublicFallbackBundleService {
  private readonly http = inject(HttpClient);

  private readonly bundle$ = this.http
    .get<PublicFallbackBundle>(FALLBACK_PATH)
    .pipe(shareReplay(1));

  load(): Observable<PublicFallbackBundle> {
    return this.bundle$;
  }
}
