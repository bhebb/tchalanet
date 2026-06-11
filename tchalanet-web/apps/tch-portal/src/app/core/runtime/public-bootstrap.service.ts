import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { TchBackendClient } from '@tch/api';

import { PublicBootstrapResponse } from './public-bootstrap.model';

@Injectable({ providedIn: 'root' })
export class PublicBootstrapService {
  private readonly backend = inject(TchBackendClient);

  /** Unauthenticated public startup runtime. Stays anonymous even with a Keycloak session. */
  bootstrap(locale?: string): Observable<PublicBootstrapResponse> {
    return this.backend.get<PublicBootstrapResponse>('/public/runtime/bootstrap', {
      suppressShellFeedback: true,
      params: locale ? { locale } : undefined,
    });
  }
}
