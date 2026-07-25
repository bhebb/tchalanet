import { Injectable, inject } from '@angular/core';
import { TchBackendClient, TchRequestOptions } from '@tch/api';
import type { ApiResponse } from '@tch/api';
import type { ConsoleReadinessStatus } from '@tch/web/console';
import { Observable } from 'rxjs';

export type ReadinessStatus = Extract<
  ConsoleReadinessStatus,
  'READY' | 'PARTIAL' | 'MISSING' | 'UNKNOWN'
>;

export interface AddressView {
  readonly id?: string;
  readonly line1?: string;
  readonly line2?: string | null;
  readonly city?: string;
  readonly region?: string | null;
  readonly country?: string;
  readonly postalCode?: string | null;
}

export interface TenantHeader {
  readonly tenantId: string;
  readonly tenantCode: string | null;
  readonly tenantName: string | null;
  readonly timezone: string | null;
  readonly currency: string | null;
  readonly tenantType: string | null;
  readonly tenantStatus: string | null;
  readonly address: AddressView | null;
}

export interface ReadinessSection {
  readonly id: string;
  readonly labelKey: string;
  readonly status: ReadinessStatus;
  readonly route: string;
  readonly issues: readonly ReadinessIssue[];
}

export interface ReadinessIssue {
  readonly code: string;
  readonly messageKey: string;
  readonly route: string;
}

export interface TenantSetupView {
  readonly totalSteps: number;
  readonly completedSteps: number;
  readonly status: 'COMPLETE' | 'INCOMPLETE';
  readonly canCreateSellerTerminal: boolean;
  readonly blockingSteps: readonly string[];
  readonly nextRecommendedStep: string | null;
}

export interface TenantAdminOverviewView {
  readonly header: TenantHeader;
  readonly status: ReadinessStatus;
  readonly missingCount: number;
  readonly sections: readonly ReadinessSection[];
  readonly setup: TenantSetupView;
}

@Injectable({ providedIn: 'root' })
export class TenantAdminOverviewApiService {
  private readonly backend = inject(TchBackendClient);

  getOverview(options?: TchRequestOptions): Observable<TenantAdminOverviewView> {
    return this.backend.get<TenantAdminOverviewView>('/admin/overview', options);
  }

  getOverviewResponse(
    options?: TchRequestOptions,
  ): Observable<ApiResponse<TenantAdminOverviewView>> {
    return this.backend.getApiResponse<TenantAdminOverviewView>('/admin/overview', options);
  }
}
