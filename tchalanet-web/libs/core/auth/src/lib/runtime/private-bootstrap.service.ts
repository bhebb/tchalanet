import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { TchBackendClient } from '@tch/api';

import { RuntimeBootstrapResponse } from './private-bootstrap.model';

@Injectable({ providedIn: 'root' })
export class PrivateBootstrapService {
  private readonly backend = inject(TchBackendClient);

  bootstrap(): Observable<RuntimeBootstrapResponse> {
    return this.backend
      .getApiResponse<RuntimeBootstrapResponse>('/runtime/private', {
        suppressShellFeedback: true,
      })
      .pipe(map(normalizeRuntimeBootstrapResponse));
  }
}

function normalizeRuntimeBootstrapResponse(input: unknown): RuntimeBootstrapResponse {
  if (isRecord(input) && isRecord(input['data'])) {
    return input['data'] as unknown as RuntimeBootstrapResponse;
  }
  return input as RuntimeBootstrapResponse;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}
