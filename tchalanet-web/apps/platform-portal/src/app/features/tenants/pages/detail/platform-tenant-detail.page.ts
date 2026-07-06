import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatTabsModule } from '@angular/material/tabs';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import {
  BadgeStatus,
  TchActionButton,
  TchConfirmDialog,
  TchConfirmDialogData,
  TchErrorPanel,
  TchLoading,
  TchNotice,
  TchStatusBadge,
} from '@tch/ui/components';

import { AdminDetailLayoutComponent } from '@tch/ui/console';
import { AdminEmptyStateComponent } from '@tch/ui/console';
import { AdminPageShellComponent } from '@tch/ui/console';
import { AdminSectionCardComponent } from '@tch/ui/console';
import { AdminStatusTone } from '@tch/ui/console';
import { TchIdentityCardComponent } from '@tch/ui/console';
import {
  PlatformTenantsApi,
  TenantAdminView,
  TenantDetailView,
} from '../../data-access/platform-tenants-api.service';
import {
  PlanSummaryView,
  SubscriptionView,
  TenantCapabilitySnapshot,
  TenantProvisioningProfile,
  TenantStatus,
} from '../../data-access/platform-tenant-contracts';
import { PlatformSubscriptionApi } from '../../data-access/platform-subscription-api.service';
import { PlatformEntitlementApi } from '../../data-access/platform-entitlement-api.service';
import {
  PlatformRecipientSellerTerminalRow,
  PlatformRecipientSellerTerminalsApi,
} from '../../../shared/data-access/platform-recipient-seller-terminals-api.service';
import { AuditEventView, PlatformAuditApi } from '../../../operations/data-access/platform-audit-api.service';
import { TenantDetailOverviewComponent } from '../../components/tenant-detail-overview/tenant-detail-overview.component';
import { TenantCommercialSummaryComponent } from '../../components/tenant-commercial-summary/tenant-commercial-summary.component';
import { TenantReadinessCardComponent } from '../../components/tenant-readiness-card/tenant-readiness-card.component';
import { TenantAdminsTableComponent } from '../../components/tenant-admins-table/tenant-admins-table.component';
import { TenantConfigSummaryComponent } from '../../components/tenant-config-summary/tenant-config-summary.component';
import { TenantSubscriptionPanelComponent } from '../../components/tenant-subscription-panel/tenant-subscription-panel.component';
import { TenantEntitlementsPanelComponent } from '../../components/tenant-entitlements-panel/tenant-entitlements-panel.component';
import { TenantSellerTerminalsTableComponent } from '../../components/tenant-seller-terminals-table/tenant-seller-terminals-table.component';
import { TenantAuditTableComponent } from '../../components/tenant-audit-table/tenant-audit-table.component';
import { StartTenantAdminAccessDialog } from '../../../shared/start-tenant-admin-access/start-tenant-admin-access-dialog';

type ProblemLike = { title?: string; detail?: string; traceId?: string; errorId?: string; requestId?: string };
type FormState = 'idle' | 'submitting' | 'error' | 'success';

@Component({
  selector: 'tch-platform-tenant-detail-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    RouterLink,
    DatePipe,
    AdminPageShellComponent,
    AdminDetailLayoutComponent,
    AdminSectionCardComponent,
    AdminEmptyStateComponent,
    TchIdentityCardComponent,
    TenantDetailOverviewComponent,
    TenantCommercialSummaryComponent,
    TenantReadinessCardComponent,
    TenantAdminsTableComponent,
    TenantConfigSummaryComponent,
    TenantSubscriptionPanelComponent,
    TenantEntitlementsPanelComponent,
    TenantSellerTerminalsTableComponent,
    TenantAuditTableComponent,
    TchLoading,
    TchErrorPanel,
    TchStatusBadge,
    TchActionButton,
    TchNotice,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatTabsModule,
  ],
  templateUrl: './platform-tenant-detail.page.html',
  styleUrls: ['./platform-tenant-detail.page.scss'],
})
export class PlatformTenantDetailPage implements OnInit {
  private readonly api = inject(PlatformTenantsApi);
  private readonly subscriptionApi = inject(PlatformSubscriptionApi);
  private readonly entitlementApi = inject(PlatformEntitlementApi);
  private readonly sellerTerminalsApi = inject(PlatformRecipientSellerTerminalsApi);
  private readonly auditApi = inject(PlatformAuditApi);
  private readonly route = inject(ActivatedRoute);
  private readonly translate = inject(TranslateService);
  private readonly fb = inject(FormBuilder);
  private readonly dialog = inject(MatDialog);

  readonly loading = signal(false);
  readonly errorTitle = signal<string | null>(null);
  readonly errorDetail = signal<string | null>(null);
  readonly traceId = signal<string | null>(null);
  readonly tenant = signal<TenantDetailView | null>(null);

  readonly showIdentityForm = signal(false);
  readonly identityFormState = signal<FormState>('idle');
  readonly identityFormError = signal<string | null>(null);
  readonly identityForm = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(200)]],
    timezone: ['', Validators.maxLength(100)],
    currency: ['', Validators.maxLength(10)],
  });

  readonly adminsLoading = signal(false);
  readonly adminsError = signal<string | null>(null);
  readonly admins = signal<TenantAdminView[]>([]);
  readonly adminsLoaded = signal(false);

  readonly activating = signal(false);
  readonly activateError = signal<string | null>(null);
  readonly suspending = signal(false);
  readonly suspendError = signal<string | null>(null);
  readonly archiving = signal(false);
  readonly archiveError = signal<string | null>(null);

  readonly subscriptionLoading = signal(false);
  readonly subscriptionError = signal<string | null>(null);
  readonly subscriptionLoaded = signal(false);
  readonly subscription = signal<SubscriptionView | null>(null);
  readonly plans = signal<readonly PlanSummaryView[]>([]);
  readonly showPlanPicker = signal(false);
  readonly selectedPlanCode = signal<string | null>(null);
  readonly applyingPlan = signal(false);
  readonly applyPlanError = signal<string | null>(null);

  readonly entitlementsLoading = signal(false);
  readonly entitlementsError = signal<string | null>(null);
  readonly entitlementsLoaded = signal(false);
  readonly entitlements = signal<TenantCapabilitySnapshot | null>(null);

  readonly sellerTerminalsLoading = signal(false);
  readonly sellerTerminalsError = signal<string | null>(null);
  readonly sellerTerminalsLoaded = signal(false);
  readonly sellerTerminals = signal<readonly PlatformRecipientSellerTerminalRow[]>([]);
  readonly sellerTerminalsPage = signal(0);
  readonly sellerTerminalsTotal = signal(0);
  readonly sellerTerminalsPageSize = 20;

  readonly auditLoading = signal(false);
  readonly auditError = signal<string | null>(null);
  readonly auditLoaded = signal(false);
  readonly auditEvents = signal<readonly AuditEventView[]>([]);
  readonly auditPage = signal(0);
  readonly auditTotal = signal(0);
  readonly auditPageSize = 20;

  readonly title = computed(() =>
    this.tenant()?.name ?? this.translate.instant('platform.tenants.detail.title'),
  );

  readonly tenantId = computed(() => {
    const t = this.tenant();
    return t?.tenantId ?? t?.id ?? null;
  });

  readonly adminsLink = computed(() => {
    const id = this.tenantId();
    return id ? ['/app/platform/tenants', id, 'admins'] : ['/app/platform/tenant-admins'];
  });

  readonly adminCreateLink = computed(() => {
    const id = this.tenantId();
    return id ? ['/app/platform/tenants', id, 'admins', 'new'] : null;
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
    this.identityFormState.set('idle');
    this.api.getTenant(id).subscribe({
      next: tenant => {
        this.tenant.set(tenant);
        this.loading.set(false);
        this.identityForm.patchValue({
          name: tenant.name ?? '',
          timezone: tenant.timezone ?? '',
          currency: tenant.currency ?? '',
        });
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

  openIdentityForm(): void {
    this.showIdentityForm.set(true);
    this.identityFormState.set('idle');
    this.identityFormError.set(null);
  }

  cancelIdentityForm(): void {
    this.showIdentityForm.set(false);
    this.identityFormError.set(null);
    const t = this.tenant();
    this.identityForm.patchValue({ name: t?.name ?? '', timezone: t?.timezone ?? '', currency: t?.currency ?? '' });
  }

  submitIdentity(): void {
    if (this.identityForm.invalid) {
      this.identityForm.markAllAsTouched();
      return;
    }
    const id = this.tenantId();
    if (!id) return;
    this.identityFormState.set('submitting');
    this.identityFormError.set(null);
    const v = this.identityForm.value;
    this.api.updateTenant(id, { name: v.name!, timezone: v.timezone || '', currency: v.currency || '' }).subscribe({
      next: () => {
        this.identityFormState.set('success');
        this.showIdentityForm.set(false);
        this.load();
      },
      error: (err: unknown) => {
        const pd = this.problem(err);
        this.identityFormError.set(pd.detail ?? pd.title ?? this.translate.instant('platform.tenants.detail.action.updateError'));
        this.identityFormState.set('error');
      },
    });
  }

  onAdminsTabSelected(): void {
    if (this.adminsLoaded()) return;
    const id = this.tenantId();
    if (!id) return;
    this.adminsLoading.set(true);
    this.adminsError.set(null);
    this.api.listTenantAdmins(id).subscribe({
      next: admins => {
        this.admins.set(admins);
        this.adminsLoaded.set(true);
        this.adminsLoading.set(false);
      },
      error: (err: unknown) => {
        const pd = this.problem(err);
        this.adminsError.set(pd.title ?? this.translate.instant('platform.tenants.detail.admins.error'));
        this.adminsLoading.set(false);
      },
    });
  }

  onTabSelected(index: number): void {
    if (index === 1) this.onAdminsTabSelected();
    if (index === 3) this.onSubscriptionTabSelected();
    if (index === 4) this.onEntitlementsTabSelected();
    if (index === 5) this.onSellerTerminalsTabSelected();
    if (index === 6) this.onAuditTabSelected();
  }

  onSubscriptionTabSelected(): void {
    if (this.subscriptionLoaded()) return;
    const id = this.tenantId();
    if (!id) return;
    this.subscriptionLoading.set(true);
    this.subscriptionError.set(null);
    this.subscriptionApi.resolve(id).subscribe({
      next: subscription => {
        this.subscription.set(subscription);
        this.subscriptionLoaded.set(true);
        this.subscriptionLoading.set(false);
        this.selectedPlanCode.set(subscription?.planCode ?? null);
      },
      error: (err: unknown) => {
        const pd = this.problem(err);
        this.subscriptionError.set(pd.title ?? this.translate.instant('platform.tenants.detail.subscription.error'));
        this.subscriptionLoading.set(false);
      },
    });
    this.subscriptionApi.listActivePlans().subscribe({
      next: plans => this.plans.set(plans),
      error: () => this.plans.set([]),
    });
  }

  onEntitlementsTabSelected(): void {
    if (this.entitlementsLoaded()) return;
    const id = this.tenantId();
    if (!id) return;
    this.entitlementsLoading.set(true);
    this.entitlementsError.set(null);
    this.entitlementApi.getSnapshot(id).subscribe({
      next: snapshot => {
        this.entitlements.set(snapshot);
        this.entitlementsLoaded.set(true);
        this.entitlementsLoading.set(false);
      },
      error: (err: unknown) => {
        const pd = this.problem(err);
        this.entitlementsError.set(pd.title ?? this.translate.instant('platform.tenants.detail.entitlements.error'));
        this.entitlementsLoading.set(false);
      },
    });
  }

  onSellerTerminalsTabSelected(): void {
    if (this.sellerTerminalsLoaded()) return;
    this.loadSellerTerminals();
  }

  sellerTerminalsPageChange(page: number): void {
    this.sellerTerminalsPage.set(page);
    this.loadSellerTerminals();
  }

  private loadSellerTerminals(): void {
    const id = this.tenantId();
    if (!id) return;
    this.sellerTerminalsLoading.set(true);
    this.sellerTerminalsError.set(null);
    this.sellerTerminalsApi.list({
      tenantId: id,
      page: this.sellerTerminalsPage(),
      size: this.sellerTerminalsPageSize,
    }).subscribe({
      next: page => {
        this.sellerTerminals.set(page.items);
        this.sellerTerminalsTotal.set(page.totalElements);
        this.sellerTerminalsLoaded.set(true);
        this.sellerTerminalsLoading.set(false);
      },
      error: (err: unknown) => {
        const pd = this.problem(err);
        this.sellerTerminalsError.set(pd.title ?? this.translate.instant('platform.tenants.detail.sellerTerminals.error'));
        this.sellerTerminalsLoading.set(false);
      },
    });
  }

  onAuditTabSelected(): void {
    if (this.auditLoaded()) return;
    this.loadAudit();
  }

  auditPageChange(page: number): void {
    this.auditPage.set(page);
    this.loadAudit();
  }

  private loadAudit(): void {
    const id = this.tenantId();
    if (!id) return;
    this.auditLoading.set(true);
    this.auditError.set(null);
    this.auditApi.listAuditEvents({
      tenantId: id,
      page: this.auditPage(),
      size: this.auditPageSize,
    }).subscribe({
      next: page => {
        this.auditEvents.set(page.items);
        this.auditTotal.set(page.totalElements);
        this.auditLoaded.set(true);
        this.auditLoading.set(false);
      },
      error: (err: unknown) => {
        const pd = this.problem(err);
        this.auditError.set(pd.title ?? this.translate.instant('platform.tenants.detail.audit.error'));
        this.auditLoading.set(false);
      },
    });
  }

  openPlanPicker(): void {
    this.showPlanPicker.set(true);
    this.applyPlanError.set(null);
    this.selectedPlanCode.set(this.subscription()?.planCode ?? null);
  }

  cancelPlanPicker(): void {
    this.showPlanPicker.set(false);
    this.applyPlanError.set(null);
  }

  selectPlan(code: string): void {
    this.selectedPlanCode.set(code);
  }

  applyPlan(): void {
    const id = this.tenantId();
    const code = this.selectedPlanCode();
    if (!id || !code || this.applyingPlan()) return;
    this.applyingPlan.set(true);
    this.applyPlanError.set(null);
    this.subscriptionApi.apply(id, code).subscribe({
      next: () => {
        this.applyingPlan.set(false);
        this.showPlanPicker.set(false);
        this.subscriptionLoaded.set(false);
        this.onSubscriptionTabSelected();
      },
      error: (err: unknown) => {
        const pd = this.problem(err);
        this.applyPlanError.set(pd.title ?? this.translate.instant('platform.tenants.detail.subscription.applyError'));
        this.applyingPlan.set(false);
      },
    });
  }

  activate(): void {
    const id = this.tenantId();
    if (!id || this.activating()) return;
    this.activating.set(true);
    this.activateError.set(null);
    this.api.activateTenant(id).subscribe({
      next: () => {
        this.activating.set(false);
        this.load();
      },
      error: (err: unknown) => {
        const pd = this.problem(err);
        this.activateError.set(pd.title ?? this.translate.instant('platform.tenants.detail.action.activateError'));
        this.activating.set(false);
      },
    });
  }

  suspend(): void {
    const id = this.tenantId();
    if (!id || this.suspending()) return;
    this.suspending.set(true);
    this.suspendError.set(null);
    this.api.suspendTenant(id).subscribe({
      next: () => {
        this.suspending.set(false);
        this.load();
      },
      error: (err: unknown) => {
        const pd = this.problem(err);
        this.suspendError.set(pd.title ?? this.translate.instant('platform.tenants.detail.action.suspendError'));
        this.suspending.set(false);
      },
    });
  }

  canArchive(status: TenantStatus): boolean {
    return status !== 'ARCHIVED';
  }

  archive(): void {
    const t = this.tenant();
    const id = this.tenantId();
    if (!t || !id || this.archiving()) return;
    const data: TchConfirmDialogData = {
      title: this.translate.instant('platform.tenants.action.archive'),
      message: this.translate.instant('platform.tenants.confirm.archive', { name: t.name }),
      confirmLabel: this.translate.instant('platform.tenants.action.archive'),
      destructive: true,
      sensitive: true,
      requireReason: true,
      auditLabel: this.translate.instant('platform.tenants.confirm.reasonLabel'),
    };
    this.dialog.open(TchConfirmDialog, { data }).afterClosed().subscribe(result => {
      if (!result?.confirmed) return;
      this.archiving.set(true);
      this.archiveError.set(null);
      this.api.archiveTenant(id).subscribe({
        next: () => {
          this.archiving.set(false);
          this.load();
        },
        error: (err: unknown) => {
          const pd = this.problem(err);
          this.archiveError.set(pd.title ?? this.translate.instant('platform.tenants.detail.action.archiveError'));
          this.archiving.set(false);
        },
      });
    });
  }

  openSupportAccess(): void {
    const t = this.tenant();
    const id = this.tenantId();
    if (!t || !id) return;
    this.dialog.open(StartTenantAdminAccessDialog, {
      width: '520px',
      data: {
        tenantId: id,
        tenantName: t.name,
        tenantCode: t.code,
        tenantStatus: t.status,
      },
    });
  }

  statusBadge(status: TenantStatus): BadgeStatus {
    const map: Record<string, BadgeStatus> = {
      ACTIVE: 'ready', DRAFT: 'pending', SUSPENDED: 'warning', REJECTED: 'blocked', ARCHIVED: 'missing',
    };
    return map[status] ?? 'missing';
  }

  statusTone(status: TenantStatus | string): AdminStatusTone {
    const map: Record<string, AdminStatusTone> = {
      ACTIVE: 'success', DRAFT: 'info', SUSPENDED: 'warning', REJECTED: 'danger', ARCHIVED: 'danger',
    };
    return map[status] ?? 'neutral';
  }

  statusLabel(status: TenantStatus | string): string {
    return this.translate.instant(`platform.tenants.status.${status.toLowerCase()}`);
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
