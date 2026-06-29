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
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTableModule } from '@angular/material/table';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import {
  BadgeStatus,
  TchActionButton,
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
  TenantBusinessCalendarRules,
  TenantDeliveryChannelConfig,
  TenantDetailView,
  TenantInternalSettings,
  TenantProvisioningProfile,
  TenantReadinessStatus,
  TenantReceiptConfig,
  TenantStatus,
} from '../../data-access/platform-tenants-api.service';

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
    MatTableModule,
  ],
  templateUrl: './platform-tenant-detail.page.html',
  styleUrls: ['./platform-tenant-detail.page.scss'],
})
export class PlatformTenantDetailPage implements OnInit {
  private readonly api = inject(PlatformTenantsApi);
  private readonly route = inject(ActivatedRoute);
  private readonly translate = inject(TranslateService);
  private readonly fb = inject(FormBuilder);

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

  readonly adminsColumns = ['displayName', 'email', 'status', 'createdAt'];

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

  readonly hasAddress = computed(() => {
    const a = this.tenant()?.address;
    return !!(a && (a.country || a.city || a.line1));
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

  readinessBadge(status: TenantReadinessStatus | null | undefined): BadgeStatus {
    const map: Record<string, BadgeStatus> = {
      READY: 'ready', INCOMPLETE: 'warning', MISSING: 'missing', BLOCKED: 'blocked', UNKNOWN: 'missing',
    };
    return status ? (map[status] ?? 'missing') : 'missing';
  }

  statusLabel(status: TenantStatus | string): string {
    return this.translate.instant(`platform.tenants.status.${status.toLowerCase()}`);
  }

  readinessLabel(status: TenantReadinessStatus | null | undefined): string {
    if (!status || status === 'UNKNOWN') return this.translate.instant('platform.tenants.readiness.unknown');
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

  adminStatusLabel(status: string): string {
    const key = `platform.tenants.adminStatus.${status.toLowerCase()}`;
    const translated = this.translate.instant(key);
    return translated === key ? status : translated;
  }

  settings(): TenantInternalSettings | null | undefined {
    return this.tenant()?.internalSettings;
  }

  locale() {
    return this.settings()?.locale;
  }

  communication() {
    return this.settings()?.communication?.buyerTicketDelivery;
  }

  receipt(): TenantReceiptConfig | null | undefined {
    return this.settings()?.document?.receipt;
  }

  calendar(): TenantBusinessCalendarRules | null | undefined {
    return this.settings()?.rules?.businessCalendar;
  }

  channelLabel(channel: TenantDeliveryChannelConfig | null | undefined): string {
    if (!channel) return this.translate.instant('common.not_available');
    const enabledLabel = channel.enabled
      ? this.translate.instant('common.enabled')
      : this.translate.instant('common.disabled');
    if (!channel.enabled) return enabledLabel;
    const amount = channel.amount != null ? `${channel.amount} ${channel.currency ?? ''}`.trim() : '';
    const paidBy = channel.paidBy
      ? this.translate.instant(`platform.tenants.detail.config.paidBy.${channel.paidBy.toLowerCase()}`)
      : '';
    return [enabledLabel, amount, paidBy].filter(Boolean).join(' · ');
  }

  weekdayLabel(day: string): string {
    const key = `common.weekday.${day.toLowerCase()}`;
    const translated = this.translate.instant(key);
    return translated === key ? day : translated;
  }

  private problem(err: unknown): ProblemLike {
    return ((err as { error?: ProblemLike })?.error ?? {}) as ProblemLike;
  }
}
