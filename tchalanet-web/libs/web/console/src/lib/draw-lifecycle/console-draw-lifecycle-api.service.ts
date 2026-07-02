import { Injectable, inject } from '@angular/core';
import { TchBackendClient, TchRequestOptions } from '@tch/api';
import { Observable, map } from 'rxjs';
import {
  ConsoleDrawLifecycleAction,
  ConsoleDrawLifecycleOneRequest,
  ConsoleDrawLifecycleRequest,
} from './console-draw-lifecycle.models';

@Injectable({ providedIn: 'root' })
export class ConsoleDrawLifecycleApi {
  private readonly backend = inject(TchBackendClient);

  execute<T>(
    action: ConsoleDrawLifecycleAction,
    request: ConsoleDrawLifecycleRequest,
    options?: TchRequestOptions,
  ): Observable<T[]> {
    return this.backend.post<T[]>(`/admin/draws/lifecycle/${action}`, request, options);
  }

  executeOne<T>(
    action: ConsoleDrawLifecycleAction,
    drawId: string,
    request: ConsoleDrawLifecycleOneRequest = {},
    options?: TchRequestOptions,
  ): Observable<T> {
    return this.execute<T>(action, { ...request, drawIds: [drawId] }, options).pipe(
      map(draws => draws[0]),
    );
  }

  cancelOne<T>(
    drawId: string,
    reason?: string | null,
    options?: TchRequestOptions,
  ): Observable<T> {
    return this.executeOne<T>(
      'cancel',
      drawId,
      {
        reasonCode: 'ADMIN_REQUEST',
        reasonLabel: reason ?? null,
        force: false,
      },
      options,
    );
  }
}
