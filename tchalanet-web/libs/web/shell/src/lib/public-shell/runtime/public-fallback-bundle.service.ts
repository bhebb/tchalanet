import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';

import { PageRuntimeResponse } from '@tch/page-model';
import { TCH_FALLBACK_ASSETS } from '@tch/shared-assets';

import { PublicBootstrapResponse } from './public-bootstrap.model';

export interface PublicFallbackBundle {
  readonly capturedAt: string;
  readonly schemaVersion: number;
  readonly publicBootstrap: PublicBootstrapResponse;
  readonly pagePayload: PageRuntimeResponse;
}

const FALLBACK_PATH = TCH_FALLBACK_ASSETS.publicBootstrapFr;

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
