import { ChangeDetectionStrategy, Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { Subject, takeUntil } from 'rxjs';

import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { AdminPageShellComponent } from '../../../../shared/admin-ui/admin-page-shell.component';
import { AdminSectionCardComponent } from '../../../../shared/admin-ui/admin-section-card.component';
import { AdminStatusTone } from '../../../../shared/admin-ui/admin-status-pill.component';
import {
  PlatformProvisioningApi,
  TenantProvisioningPreviewView,
  TenantProvisioningProfile,
  TenantProvisioningRequest,
  TenantProvisioningResultView,
  TenantType,
} from '../../../platform-provisioning-api.service';

type ProblemLike = { title?: string; detail?: string; traceId?: string; errorId?: string; requestId?: string };

const TENANT_TYPES: readonly TenantType[] = ['BORLETTE', 'RESEAU', 'AMBULANT'];
const PROFILES: readonly TenantProvisioningProfile[] = ['MINIMAL', 'DEFAULT_HAITI_LOTTERY', 'DEMO'];
const TIMEZONES = ['America/Port-au-Prince', 'America/New_York', 'UTC'];
const CURRENCIES = ['HTG', 'USD'];

@Component({
  selector: 'tch-platform-tenant-create-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
    RouterLink,
    ReactiveFormsModule,
    AdminPageShellComponent,
    AdminSectionCardComponent,
    TchLoading,
    TchErrorPanel,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './platform-tenant-create.page.html',
  styleUrls: ['./platform-tenant-create.page.scss'],
})
export class PlatformTenantCreatePage implements OnInit, OnDestroy {
  private readonly api = inject(PlatformProvisioningApi);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly translate = inject(TranslateService);
  private readonly fb = inject(FormBuilder);
  private readonly destroy$ = new Subject<void>();

  readonly tenantTypes = TENANT_TYPES;
  readonly profiles = PROFILES;
  readonly timezones = TIMEZONES;
  readonly currencies = CURRENCIES;

  readonly submitting = signal(false);
  readonly submitErrorTitle = signal<string | null>(null);
  readonly submitErrorDetail = signal<string | null>(null);
  readonly submitTraceId = signal<string | null>(null);
  readonly previewLoading = signal(false);
  readonly previewErrorTitle = signal<string | null>(null);
  readonly previewErrorDetail = signal<string | null>(null);
  readonly previewTraceId = signal<string | null>(null);
  readonly preview = signal<TenantProvisioningPreviewView | null>(null);
  readonly result = signal<TenantProvisioningResultView | null>(null);

  readonly form = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.pattern(/^[a-z0-9-]{3,30}$/)]],
    name: ['', Validators.required],
    type: ['BORLETTE' as TenantType, Validators.required],
    timezone: ['America/Port-au-Prince', Validators.required],
    currency: ['HTG', Validators.required],
    profile: ['DEFAULT_HAITI_LOTTERY' as TenantProvisioningProfile, Validators.required],
    initialAdminEmail: ['', [Validators.email]],
  });

  readonly readinessTone = computed((): AdminStatusTone => {
    const s = this.result()?.readiness.status;
    if (s === 'READY') return 'success';
    if (s === 'INCOMPLETE' || s === 'MISSING') return 'warning';
    if (s === 'BLOCKED') return 'danger';
    return 'neutral';
  });

  readonly domainEntries = computed(() => {
    const ds = this.result()?.domainStatuses ?? {};
    return Object.entries(ds).map(([key, value]) => ({ key, value }));
  });

  ngOnInit(): void {
    this.form.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.preview.set(null);
      this.result.set(null);
      this.previewErrorTitle.set(null);
      this.submitErrorTitle.set(null);
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  previewProvisioning(): void {
    if (this.form.invalid || this.previewLoading() || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    this.previewLoading.set(true);
    this.previewErrorTitle.set(null);
    this.previewErrorDetail.set(null);
    this.previewTraceId.set(null);

    this.api.preview(this.request()).subscribe({
      next: res => {
        this.preview.set(res);
        this.previewLoading.set(false);
      },
      error: (err: unknown) => {
        const pd = this.problem(err);
        this.previewErrorTitle.set(pd.title ?? this.translate.instant('platform.tenants.create.previewError'));
        this.previewErrorDetail.set(pd.detail ?? null);
        this.previewTraceId.set(pd.traceId ?? pd.errorId ?? pd.requestId ?? null);
        this.previewLoading.set(false);
      },
    });
  }

  submit(): void {
    if (this.form.invalid || this.previewLoading() || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.submitErrorTitle.set(null);
    this.submitErrorDetail.set(null);
    this.submitTraceId.set(null);

    this.api.provision(this.request()).subscribe({
      next: res => {
        this.submitting.set(false);
        this.result.set(res);
        this.snackBar.open(this.translate.instant('platform.tenants.create.success'), 'OK', { duration: 4000 });
        void this.router.navigate(['/app/platform/tenants', res.tenantId], { state: { provisionResult: res } });
      },
      error: (err: unknown) => {
        this.submitting.set(false);
        const pd = this.problem(err);
        this.submitErrorTitle.set(pd.title ?? this.translate.instant('platform.tenants.create.error'));
        this.submitErrorDetail.set(pd.detail ?? null);
        this.submitTraceId.set(pd.traceId ?? pd.errorId ?? pd.requestId ?? null);
      },
    });
  }

  profileLabel(profile: TenantProvisioningProfile): string {
    const keys: Record<TenantProvisioningProfile, string> = {
      MINIMAL: 'platform.tenants.profile.minimal',
      DEFAULT_HAITI_LOTTERY: 'platform.tenants.profile.defaultHaitiLottery',
      DEMO: 'platform.tenants.profile.demo',
    };
    return this.translate.instant(keys[profile]);
  }

  profileDescription(profile: TenantProvisioningProfile): string {
    const keys: Record<TenantProvisioningProfile, string> = {
      MINIMAL: 'platform.tenants.profileDescription.minimal',
      DEFAULT_HAITI_LOTTERY: 'platform.tenants.profileDescription.defaultHaitiLottery',
      DEMO: 'platform.tenants.profileDescription.demo',
    };
    return this.translate.instant(keys[profile]);
  }

  tenantTypeLabel(type: TenantType): string {
    return this.translate.instant(`platform.tenants.type.${type.toLowerCase()}`);
  }

  provisioningDomainLabel(key: string): string {
    return this.translatedProvisioningKey('domains', key);
  }

  provisioningDataLabel(key: string): string {
    return this.translatedProvisioningKey('data', key);
  }

  provisioningWarningLabel(key: string): string {
    return this.translatedProvisioningKey('warnings', key);
  }

  readinessSectionLabel(key: string): string {
    return this.translatedProvisioningKey('readiness', key);
  }

  domainTone(status: string): AdminStatusTone {
    const s = status?.toUpperCase();
    if (s === 'OK' || s === 'DONE' || s === 'SUCCESS' || s === 'CREATED' || s === 'DEFAULT'
      || s === 'DEFAULT_LOTTERY' || s === 'DEFAULT_HAITI' || s === 'TEMPLATES_AVAILABLE'
      || s === 'SEEDED_VIA_LISTENER') return 'success';
    if (s === 'WARNING' || s === 'PARTIAL' || s === 'MISSING') return 'warning';
    if (s === 'ERROR' || s === 'FAILED') return 'danger';
    return 'neutral';
  }

  private translatedProvisioningKey(group: string, key: string): string {
    const i18nKey = `platform.tenants.provisioning.${group}.${key}`;
    const translated = this.translate.instant(i18nKey);
    return translated === i18nKey ? key : translated;
  }

  private request(): TenantProvisioningRequest {
    const v = this.form.getRawValue();
    return {
      code: v.code,
      name: v.name,
      type: v.type,
      timezone: v.timezone,
      currency: v.currency,
      // Deprecated create page (superseded by Onboarding): send the default commission.
      defaultCommissionRate: 15,
      profile: v.profile,
      initialAdminEmail: v.initialAdminEmail || null,
    };
  }

  private problem(err: unknown): ProblemLike {
    return ((err as { error?: ProblemLike })?.error ?? {}) as ProblemLike;
  }
}
