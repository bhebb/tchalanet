import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { TchBackendClient } from '@tch/api';

import { RuntimeBootstrapResponse } from './private-bootstrap.model';

@Injectable({ providedIn: 'root' })
export class PrivateBootstrapService {
  private readonly backend = inject(TchBackendClient);

  bootstrap(): Observable<RuntimeBootstrapResponse> {
    return this.backend.get<RuntimeBootstrapResponse>('/tenant/runtime/bootstrap', {
      suppressShellFeedback: true,
    });
  }
}
