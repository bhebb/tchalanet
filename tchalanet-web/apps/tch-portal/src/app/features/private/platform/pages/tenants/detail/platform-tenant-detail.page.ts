import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import {
  BadgeStatus,
  TchActionButton,
  TchErrorPanel,
  TchLoading,
  TchStatusBadge,
} from '@tch/ui/components';

import { AdminDetailLayoutComponent } from '../../../../shared/admin-ui/components/admin-detail-layout/admin-detail-layout.component';
import { AdminEmptyStateComponent } from '../../../../shared/admin-ui/admin-empty-state.component';
import { AdminPageShellComponent } from '../../../../shared/admin-ui/admin-page-shell.component';
import { AdminSectionCardComponent } from '../../../../shared/admin-ui/admin-section-card.component';
import { AdminStatusTone } from '../../../../shared/admin-ui/admin-status-pill.component';
import { TchIdentityCardComponent } from '../../../../shared/admin-ui/components/tch-identity-card/tch-identity-card.component';
import {
  PlatformTenantsApi,
  TenantDetailView,
  TenantProvisioningProfile,
  TenantReadinessStatus,
  TenantStatus,
} from '../../../platform-tenants-api.service';

type ProblemLike = { title?: string; detail?: string; traceId?: string; errorId?: string; requestId?: string };

@Component({
  selector: 'tch-platform-tenant-detail-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
    RouterLink,
    DatePipe,
    AdminPageShellComponent,
    AdminDetailLayoutComponent,
    AdminSectionCardComponent,
    AdminEmptyStateComponent,
    TchIdentityCardComponent,
    TchLoading,
    TchErrorPanel,
    TchStatusBadge,
    TchActionButton,
    MatButtonModule,
  ],
  templateUrl: './platform-tenant-detail.page.html',
  styleUrls: ['./platform-tenant-detail.page.scss'],
})
export class PlatformTenantDetailPage implements OnInit {
  private readonly api = inject(PlatformTenantsApi);
  private readonly route = inject(ActivatedRoute);
  private readonly translate = inject(TranslateService);

  readonly loading = signal(false);
  readonly errorTitle = signal<string | null>(null);
  readonly errorDetail = signal<string | null>(null);
  readonly traceId = signal<string | null>(null);
  readonly tenant = signal<TenantDetailView | null>(null);

  readonly title = computed(() =>
    this.tenant()?.name ?? this.translate.instant('platform.tenants.detail.title'),
  );

  readonly adminsLink = computed(() => {
    const id = this.tenant()?.tenantId ?? this.tenant()?.id;
    return id ? ['/app/platform/tenants', id, 'admins'] : ['/app/platform/tenant-admins'];
  });

  readonly identityMeta = computed(() => {
    const t = this.tenant();
    if (!t) return [];
    const na = this.translate.instant('common.not_available');
    return [
      { label: this.translate.instant('platform.tenants.detail.field.type'), value: this.typeLabel(t.type) },
      { label: this.translate.instant('platform.tenantProvisioning.field.currency'), value: t.currency ?? na },
      ...(t.defaultCommissionRate != null
        ? [{ label: this.translate.instant('platform.tenantProvisioning.field.defaultCommissionRate'), value: `${t.defaultCommissionRate} %` }]
        : []),
      { label: this.translate.instant('platform.tenants.column.profile'), value: this.profileLabel(t.profile) },
    ];
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    const id = this.route.snapshot.paramMap.get('tenantId');
    if (!id) return;
    this.loading.set(true);
    this.errorTitle.set(null);
    this.errorDetail.set(null);
    this.traceId.set(null);
    this.api.getTenant(id).subscribe({
      next: tenant => {
        this.tenant.set(tenant);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        const pd = this.problem(err);
        this.errorTitle.set(pd.title ?? this.translate.instant('platform.tenants.detail.error'));
        this.errorDetail.set(pd.detail ?? null);
        this.traceId.set(pd.traceId ?? pd.errorId ?? pd.requestId ?? null);
        this.loading.set(false);
      },
    });
  }

  statusBadge(status: TenantStatus): BadgeStatus {
    const map: Record<string, BadgeStatus> = {
      ACTIVE: 'ready',
      DRAFT: 'pending',
      SUSPENDED: 'warning',
      REJECTED: 'blocked',
      ARCHIVED: 'missing',
    };
    return map[status] ?? 'missing';
  }

  statusTone(status: TenantStatus | string): AdminStatusTone {
    const map: Record<string, AdminStatusTone> = {
      ACTIVE: 'success',
      DRAFT: 'info',
      SUSPENDED: 'warning',
      REJECTED: 'danger',
      ARCHIVED: 'danger',
    };
    return map[status] ?? 'neutral';
  }

  readinessBadge(status: TenantReadinessStatus | null | undefined): BadgeStatus {
    const map: Record<string, BadgeStatus> = {
      READY: 'ready',
      INCOMPLETE: 'warning',
      MISSING: 'missing',
      BLOCKED: 'blocked',
      UNKNOWN: 'missing',
    };
    return status ? (map[status] ?? 'missing') : 'missing';
  }

  statusLabel(status: TenantStatus | string): string {
    return this.translate.instant(`platform.tenants.status.${status.toLowerCase()}`);
  }

  readinessLabel(status: TenantReadinessStatus | null | undefined): string {
    if (!status || status === 'UNKNOWN') {
      return this.translate.instant('platform.tenants.readiness.unknown');
    }
    return this.translate.instant(`platform.tenants.readiness.${status.toLowerCase()}`);
  }

  profileLabel(profile: TenantProvisioningProfile | string | null | undefined): string {
    if (!profile) return this.translate.instant('common.not_set');
    const keys: Record<string, string> = {
      MINIMAL: 'platform.tenants.profile.minimal',
      DEFAULT_HAITI_LOTTERY: 'platform.tenants.profile.defaultHaitiLottery',
      DEMO: 'platform.tenants.profile.demo',
    };
    return this.translate.instant(keys[profile] ?? profile);
  }

  typeLabel(type: string | null | undefined): string {
    if (!type) return this.translate.instant('common.not_available');
    const keys: Record<string, string> = {
      BORLETTE: 'platform.tenants.type.borlette',
      RESEAU: 'platform.tenants.type.reseau',
      AMBULANT: 'platform.tenants.type.ambulant',
    };
    return this.translate.instant(keys[type] ?? type);
  }

  private problem(err: unknown): ProblemLike {
    return ((err as { error?: ProblemLike })?.error ?? {}) as ProblemLike;
  }
}
