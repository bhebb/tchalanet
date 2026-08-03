import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { Observable, map } from 'rxjs';

import { TchSearchOption, TchSearchSelect, TchSectionError } from '@tch/ui/components';
import { AdminPageShellComponent, AdminSectionCardComponent } from '@tch/ui/console';
import { tchMutation } from '@tch/web/async';
import {
  AnalyticsReconciliationMismatch,
  AnalyticsReconciliationRequest,
  AnalyticsReconciliationResponse,
  PlatformOpsApi,
} from '../../data-access/platform-ops-api.service';
import {
  PlatformTenantsApi,
  TenantSummaryView,
} from '../../../tenants/data-access/platform-tenants-api.service';
import {
  RebuildAnalyticsDialog,
} from '../../components/dialogs/rebuild-analytics.dialog';

const MAX_RECONCILIATION_DAYS = 90;

@Component({
  selector: 'tch-platform-ops-analytics-reconciliation-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    AdminPageShellComponent,
    AdminSectionCardComponent,
    TchSearchSelect,
    TchSectionError,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
  ],
  templateUrl: './platform-ops-analytics-reconciliation.page.html',
  styleUrl: './platform-ops-analytics-reconciliation.page.scss',
})
export class PlatformOpsAnalyticsReconciliationPage {
  private readonly api = inject(PlatformOpsApi);
  private readonly tenantsApi = inject(PlatformTenantsApi);
  private readonly dialog = inject(MatDialog);
  private readonly translate = inject(TranslateService);

  readonly selectedTenant = signal<TenantSummaryView | null>(null);
  readonly result = signal<AnalyticsReconciliationResponse | null>(null);
  readonly scopeError = signal<string | null>(null);
  readonly fromControl = new FormControl(relativeIsoDate(-6), { nonNullable: true });
  readonly toControl = new FormControl(relativeIsoDate(0), { nonNullable: true });

  readonly selectedTenantId = computed(() => {
    const tenant = this.selectedTenant();
    return tenant?.id ?? tenant?.tenantId ?? null;
  });
  readonly tenantLabel = computed(() => {
    const tenant = this.selectedTenant();
    return tenant ? `${tenant.name} (${tenant.code})` : this.t('platform.analyticsReconciliation.noTenant');
  });
  readonly actionFeedback = computed(
    () => this.validateMutation.feedback()?.vm ?? this.rebuildMutation.feedback()?.vm ?? null,
  );

  readonly validateMutation = tchMutation<AnalyticsReconciliationRequest, AnalyticsReconciliationResponse>({
    source: 'platform.ops.analytics.reconciliation.validate',
    run: request => this.api.reconcileAnalytics(request, { suppressShellFeedback: true }),
    onSuccess: result => this.result.set(result),
  });

  readonly rebuildMutation = tchMutation<AnalyticsReconciliationRequest, AnalyticsReconciliationResponse>({
    source: 'platform.ops.analytics.reconciliation.rebuild',
    run: (request, context) => this.api.reconcileAnalytics(request, {
      suppressShellFeedback: true,
      headers: context.idempotencyKey ? { 'Idempotency-Key': context.idempotencyKey } : undefined,
    }),
    idempotency: {},
    onSuccess: result => this.result.set(result),
  });

  readonly searchTenants = (query: string): Observable<readonly TchSearchOption<TenantSummaryView>[]> =>
    this.tenantsApi.listTenants({ q: query, page: 0, size: 12, status: 'ACTIVE' }).pipe(
      map(page => (page.items ?? []).map(tenant => this.tenantOption(tenant))),
    );

  selectTenant(option: TchSearchOption | null): void {
    this.selectedTenant.set((option?.data as TenantSummaryView | undefined) ?? null);
    this.clearResult();
  }

  onScopeChange(): void {
    this.clearResult();
  }

  validate(): void {
    const request = this.request('VALIDATE');
    if (request) this.validateMutation.execute(request);
  }

  openRebuild(): void {
    const previous = this.result();
    const tenantId = this.selectedTenantId();
    if (!previous || previous.status !== 'MISMATCH' || !tenantId) return;

    const ref = this.dialog.open(RebuildAnalyticsDialog, {
      width: '34rem',
      data: {
        tenantLabel: this.tenantLabel(),
        from: previous.from,
        to: previous.to,
      },
    });
    ref.afterClosed().subscribe(reason => {
      if (typeof reason !== 'string' || !reason.trim()) return;
      const request = this.request('REBUILD_AND_VALIDATE', reason);
      if (request) this.rebuildMutation.execute(request);
    });
  }

  canRebuild(): boolean {
    return this.result()?.status === 'MISMATCH' && !this.rebuildMutation.pending();
  }

  statusKey(status: AnalyticsReconciliationResponse['status']): string {
    return `platform.analyticsReconciliation.status.${status}`;
  }

  formatAmount(value: number): string {
    const currency = this.selectedTenant()?.currency ?? 'HTG';
    return `${new Intl.NumberFormat(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value / 100)} ${currency}`;
  }

  difference(expected: number, observed: number): string {
    const delta = observed - expected;
    return `${delta > 0 ? '+' : ''}${this.formatAmount(delta)}`;
  }

  trackMismatch(_: number, mismatch: AnalyticsReconciliationMismatch): string {
    return [mismatch.projection, mismatch.businessDate ?? '', mismatch.drawId ?? '', mismatch.sellerTerminalId ?? ''].join(':');
  }

  private request(
    mode: AnalyticsReconciliationRequest['mode'],
    repairReason?: string,
  ): AnalyticsReconciliationRequest | null {
    const tenantId = this.selectedTenantId();
    const from = this.fromControl.value;
    const to = this.toControl.value;
    const invalidScope = !tenantId || !from || !to || from > to || dayDistance(from, to) > MAX_RECONCILIATION_DAYS;
    if (invalidScope) {
      this.scopeError.set(this.t('platform.analyticsReconciliation.validation.scope'));
      return null;
    }
    this.scopeError.set(null);
    return { tenantId, from, to, mode, repairReason };
  }

  private clearResult(): void {
    this.result.set(null);
    this.scopeError.set(null);
    this.validateMutation.clearFeedback();
    this.rebuildMutation.clearFeedback();
  }

  private tenantOption(tenant: TenantSummaryView): TchSearchOption<TenantSummaryView> {
    return {
      id: tenant.id ?? tenant.tenantId ?? tenant.code,
      title: tenant.name,
      subtitle: tenant.code,
      badge: tenant.status,
      icon: 'apartment',
      data: tenant,
    };
  }

  private t(key: string): string {
    return this.translate.instant(key);
  }
}

function relativeIsoDate(offsetDays: number): string {
  const date = new Date();
  date.setDate(date.getDate() + offsetDays);
  return date.toISOString().slice(0, 10);
}

function dayDistance(from: string, to: string): number {
  return Math.floor((Date.parse(`${to}T00:00:00Z`) - Date.parse(`${from}T00:00:00Z`)) / 86_400_000);
}
