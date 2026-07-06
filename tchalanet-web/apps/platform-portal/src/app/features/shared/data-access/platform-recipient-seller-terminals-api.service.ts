import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { TchBackendClient, TchPage, TchRequestOptions, appendQuery } from '@tch/api';
import type { ConsoleSellerTerminalStatus } from '@tch/web/console';

export type PlatformRecipientSellerTerminalStatus = ConsoleSellerTerminalStatus;

export interface PlatformRecipientSellerTerminalRow {
  readonly id: { readonly value: string };
  readonly terminalCode: string;
  readonly displayName: string;
  readonly email?: string | null;
  readonly phoneNumber?: string | null;
  readonly status: PlatformRecipientSellerTerminalStatus;
}

export interface ListPlatformRecipientSellerTerminalsParams {
  readonly tenantId: string;
  readonly q?: string;
  readonly page?: number;
  readonly size?: number;
}

@Injectable({ providedIn: 'root' })
export class PlatformRecipientSellerTerminalsApi {
  private readonly backend = inject(TchBackendClient);

  list(
    params: ListPlatformRecipientSellerTerminalsParams,
    options?: TchRequestOptions,
  ): Observable<TchPage<PlatformRecipientSellerTerminalRow>> {
    return this.backend.get<TchPage<PlatformRecipientSellerTerminalRow>>(
      appendQuery('/admin/seller-terminals', {
        q: params.q,
        page: params.page,
        size: params.size,
      }),
      {
        ...(options ?? {}),
        asTenantAdmin: {
          tenantId: params.tenantId,
          reason: 'SUPER_ADMIN: list seller terminals for recipient picker',
        },
      },
    );
  }
}
