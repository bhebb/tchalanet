import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ProblemDetail, webAppErrorFromProblemDetail, webAppErrorsFromProblemDetailFields } from '@tch/api';
import {
  BadgeStatus,
  TchErrorPanel,
  TchFieldError,
  TchLoading,
  TchNotice,
  TchStatusBadge,
} from '@tch/ui/components';

import { AdminDetailLayoutComponent } from '../../../../shared/admin-ui/components/admin-detail-layout/admin-detail-layout.component';
import { AdminPageShellComponent } from '../../../../shared/admin-ui/admin-page-shell.component';
import { AdminSectionCardComponent } from '../../../../shared/admin-ui/admin-section-card.component';
import {
  AdminSectionErrorTargetDirective,
  AdminSectionTargetError,
} from '../../../../shared/admin-ui/admin-section-error-target.directive';
import { AdminStatusTone } from '../../../../shared/admin-ui/admin-status-pill.component';
import { TchIdentityCardComponent } from '../../../../shared/admin-ui/components/tch-identity-card/tch-identity-card.component';
import { resolveErrorFeedbackCopy } from '../../../../../../core/api/error-feedback-copy';
import {
  applyServerFieldErrors,
  clearServerFieldErrors,
  ErrorViewModel,
  toErrorViewModel,
  withResolvedErrorCopies,
} from '../../../../../../core/api/local-error-routing';
import {
  AdminOverviewApiService,
  AddressView,
  TenantAdminOverviewView,
  TenantHeader,
} from '../../../admin-overview-api.service';

type PageState = 'loading' | 'ready' | 'error';
type FormState = 'idle' | 'submitting' | 'error' | 'success';

const IDENTITY_SECTION_TARGET = 'admin.businessProfile.identity';
const REGION_SECTION_TARGET = 'admin.businessProfile.region';
const COMMERCIAL_SECTION_TARGET = 'admin.businessProfile.commercial';
const ADDRESS_SECTION_TARGET = 'admin.businessProfile.address';

@Component({
  selector: 'tch-admin-business-profile-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    AdminPageShellComponent,
    AdminDetailLayoutComponent,
    AdminSectionCardComponent,
    AdminSectionErrorTargetDirective,
    TchIdentityCardComponent,
    TchLoading,
    TchErrorPanel,
    TchFieldError,
    TchNotice,
    TchStatusBadge,
  ],
  templateUrl: './admin-business-profile.page.html',
  styleUrls: ['./admin-business-profile.page.scss'],
})
export class AdminBusinessProfilePage implements OnInit {
  private readonly api = inject(AdminOverviewApiService);
  private readonly fb = inject(FormBuilder);
  private readonly translate = inject(TranslateService);

  readonly pageState = signal<PageState>('loading');
  readonly pageError = signal<ErrorViewModel | null>(null);
  readonly overview = signal<TenantAdminOverviewView | null>(null);
  readonly commissionRate = signal<number | null>(null);
  readonly sectionErrors = signal<readonly AdminSectionTargetError[]>([]);

  // Identity form — name only
  readonly showIdentityForm = signal(false);
  readonly identityFormState = signal<FormState>('idle');
  readonly identityFormError = signal<string | null>(null);
  readonly identityForm = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(200)]],
  });

  // Region form — timezone + currency
  readonly showRegionForm = signal(false);
  readonly regionFormState = signal<FormState>('idle');
  readonly regionFormError = signal<string | null>(null);
  readonly regionForm = this.fb.group({
    timezone: ['', Validators.maxLength(100)],
    currency: ['', Validators.maxLength(10)],
  });

  // Commission form
  readonly showCommissionForm = signal(false);
  readonly commissionFormState = signal<FormState>('idle');
  readonly commissionFormError = signal<string | null>(null);
  readonly commissionForm = this.fb.group({
    rate: [null as number | null, [Validators.required, Validators.min(0), Validators.max(100)]],
  });

  // Address form
  readonly showAddressForm = signal(false);
  readonly addressFormState = signal<FormState>('idle');
  readonly addressFormError = signal<string | null>(null);
  readonly addressForm = this.fb.group({
    line1: ['', [Validators.required, Validators.maxLength(200)]],
    line2: ['', Validators.maxLength(200)],
    city: ['', [Validators.required, Validators.maxLength(100)]],
    region: ['', Validators.maxLength(100)],
    country: ['HT', [Validators.required, Validators.maxLength(2)]],
    postalCode: ['', Validators.maxLength(20)],
  });

  readonly header = computed(() => this.overview()?.header ?? null);
  readonly hasAddress = computed(() => {
    const a = this.header()?.address;
    return !!(a && (a.line1 || a.city || a.country));
  });
  readonly loading = computed(() => this.pageState() === 'loading');
  readonly error = computed(() => (this.pageState() === 'error' ? this.pageError() : null));

  readonly identityMeta = computed(() => {
    const h = this.header();
    if (!h) return [];
    const na = this.translate.instant('common.not_available');
    const meta = [
      { label: this.translate.instant('admin.businessProfile.field.type'), value: this.typeLabel(h.tenantType) },
      { label: this.translate.instant('admin.businessProfile.field.currency'), value: h.currency ?? na },
      { label: this.translate.instant('admin.businessProfile.field.status'), value: this.statusLabel(h.tenantStatus) },
    ];
    const rate = this.commissionRate();
    if (rate != null) {
      meta.push({ label: this.translate.instant('admin.businessProfile.field.commission'), value: `${rate} %` });
    }
    return meta;
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.pageState.set('loading');
    this.pageError.set(null);
    this.sectionErrors.set([]);
    this.identityFormState.set('idle');
    this.regionFormState.set('idle');
    this.commissionFormState.set('idle');
    this.addressFormState.set('idle');
    this.api.getOverview({ suppressShellFeedback: true }).subscribe({
      next: data => {
        this.overview.set(data);
        this.pageState.set('ready');
        this.prefillForms(data.header);
      },
      error: (err: unknown) => {
        this.pageError.set(this.errorViewModel(err, 'admin.businessProfile.overview', 'page'));
        this.pageState.set('error');
      },
    });
    this.api.getCommissionOverview({ suppressShellFeedback: true }).subscribe({
      next: r => {
        this.commissionRate.set(r.tenantDefaultRate);
        this.commissionForm.patchValue({ rate: r.tenantDefaultRate });
        this.clearSectionError(COMMERCIAL_SECTION_TARGET);
      },
      error: (err: unknown) => {
        this.setSectionError(this.sectionErrorFromUnknown(err, COMMERCIAL_SECTION_TARGET));
      },
    });
  }

  // ── Identity (name) ────────────────────────────────────────────

  openIdentityForm(): void {
    this.showIdentityForm.set(true);
    this.identityFormState.set('idle');
    this.identityFormError.set(null);
    clearServerFieldErrors(this.identityForm);
    this.showRegionForm.set(false);
    this.showAddressForm.set(false);
  }

  cancelIdentityForm(): void {
    this.showIdentityForm.set(false);
    this.identityFormError.set(null);
    clearServerFieldErrors(this.identityForm);
    this.identityForm.patchValue({ name: this.header()?.tenantName ?? '' });
  }

  submitIdentity(): void {
    clearServerFieldErrors(this.identityForm);
    if (this.identityForm.invalid) { this.identityForm.markAllAsTouched(); return; }
    const h = this.header();
    if (!h) return;
    this.identityFormState.set('submitting');
    this.identityFormError.set(null);
    this.clearSectionError(IDENTITY_SECTION_TARGET);
    const v = this.identityForm.getRawValue();
    this.api.updateIdentity({
      name: v.name ?? '',
      timezone: h.timezone ?? '',
      currency: h.currency ?? '',
    }, { suppressShellFeedback: true }).subscribe({
      next: () => { this.identityFormState.set('success'); this.showIdentityForm.set(false); this.load(); },
      error: (err: unknown) => {
        this.handleIdentitySubmitError(err);
        this.identityFormState.set('error');
      },
    });
  }

  // ── Region (timezone + currency) ────────────────────────────────

  openRegionForm(): void {
    this.showRegionForm.set(true);
    this.regionFormState.set('idle');
    this.regionFormError.set(null);
    clearServerFieldErrors(this.regionForm);
    this.showIdentityForm.set(false);
    this.showAddressForm.set(false);
  }

  cancelRegionForm(): void {
    this.showRegionForm.set(false);
    this.regionFormError.set(null);
    clearServerFieldErrors(this.regionForm);
    this.regionForm.patchValue({ timezone: this.header()?.timezone ?? '', currency: this.header()?.currency ?? '' });
  }

  submitRegion(): void {
    clearServerFieldErrors(this.regionForm);
    if (this.regionForm.invalid) { this.regionForm.markAllAsTouched(); return; }
    const h = this.header();
    if (!h) return;
    this.regionFormState.set('submitting');
    this.regionFormError.set(null);
    this.clearSectionError(REGION_SECTION_TARGET);
    const v = this.regionForm.getRawValue();
    this.api.updateIdentity({
      name: h.tenantName ?? '',
      timezone: v.timezone ?? '',
      currency: v.currency ?? '',
    }, { suppressShellFeedback: true }).subscribe({
      next: () => { this.regionFormState.set('success'); this.showRegionForm.set(false); this.load(); },
      error: (err: unknown) => {
        this.handleRegionSubmitError(err);
        this.regionFormState.set('error');
      },
    });
  }

  // ── Commission ─────────────────────────────────────────────────

  openCommissionForm(): void {
    this.showCommissionForm.set(true);
    this.commissionFormState.set('idle');
    this.commissionFormError.set(null);
    clearServerFieldErrors(this.commissionForm);
    this.showIdentityForm.set(false);
    this.showRegionForm.set(false);
    this.showAddressForm.set(false);
  }

  cancelCommissionForm(): void {
    this.showCommissionForm.set(false);
    this.commissionFormError.set(null);
    clearServerFieldErrors(this.commissionForm);
    this.commissionForm.patchValue({ rate: this.commissionRate() });
  }

  submitCommission(): void {
    clearServerFieldErrors(this.commissionForm);
    if (this.commissionForm.invalid) { this.commissionForm.markAllAsTouched(); return; }
    const rate = this.commissionForm.value.rate;
    if (rate == null) return;
    this.commissionFormState.set('submitting');
    this.commissionFormError.set(null);
    this.clearSectionError(COMMERCIAL_SECTION_TARGET);
    this.api.updateDefaultCommissionRate(rate, { suppressShellFeedback: true }).subscribe({
      next: () => {
        this.commissionFormState.set('success');
        this.showCommissionForm.set(false);
        this.commissionRate.set(rate);
      },
      error: (err: unknown) => {
        this.handleCommissionSubmitError(err);
        this.commissionFormState.set('error');
      },
    });
  }

  // ── Address ────────────────────────────────────────────────────

  openAddressForm(): void {
    this.showAddressForm.set(true);
    this.addressFormState.set('idle');
    this.addressFormError.set(null);
    clearServerFieldErrors(this.addressForm);
    this.showIdentityForm.set(false);
    this.showRegionForm.set(false);
    this.showCommissionForm.set(false);
  }

  cancelAddressForm(): void {
    this.showAddressForm.set(false);
    this.addressFormError.set(null);
    clearServerFieldErrors(this.addressForm);
    this.prefillAddressForm(this.header()?.address ?? null);
  }

  submitAddress(): void {
    clearServerFieldErrors(this.addressForm);
    if (this.addressForm.invalid) { this.addressForm.markAllAsTouched(); return; }
    this.addressFormState.set('submitting');
    this.addressFormError.set(null);
    this.clearSectionError(ADDRESS_SECTION_TARGET);
    const v = this.addressForm.getRawValue();
    this.api.upsertAddress({
      line1: v.line1 ?? '',
      line2: v.line2 || null,
      city: v.city ?? '',
      region: v.region || null,
      country: v.country ?? '',
      postalCode: v.postalCode || null,
    }, { suppressShellFeedback: true }).subscribe({
      next: () => { this.addressFormState.set('success'); this.showAddressForm.set(false); this.load(); },
      error: (err: unknown) => {
        this.handleAddressSubmitError(err);
        this.addressFormState.set('error');
      },
    });
  }

  // ── Labels ─────────────────────────────────────────────────────

  statusBadge(status: string | null | undefined): BadgeStatus {
    const map: Record<string, BadgeStatus> = {
      ACTIVE: 'ready', DRAFT: 'pending', SUSPENDED: 'warning', REJECTED: 'blocked', ARCHIVED: 'missing',
    };
    return (status ? map[status] : null) ?? 'missing';
  }

  statusTone(status: string | null | undefined): AdminStatusTone {
    const map: Record<string, AdminStatusTone> = {
      ACTIVE: 'success', DRAFT: 'info', SUSPENDED: 'warning', REJECTED: 'danger', ARCHIVED: 'danger',
    };
    return (status ? map[status] : null) ?? 'neutral';
  }

  statusLabel(status: string | null | undefined): string {
    if (!status) return this.translate.instant('common.not_available');
    const key = `platform.tenants.status.${status.toLowerCase()}`;
    const t = this.translate.instant(key);
    return t === key ? status : t;
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

  private prefillForms(h: TenantHeader | null): void {
    this.identityForm.patchValue({ name: h?.tenantName ?? '' });
    this.regionForm.patchValue({ timezone: h?.timezone ?? '', currency: h?.currency ?? '' });
    this.prefillAddressForm(h?.address ?? null);
  }

  private prefillAddressForm(addr: AddressView | null): void {
    this.addressForm.patchValue({
      line1: addr?.line1 ?? '', line2: addr?.line2 ?? '', city: addr?.city ?? '',
      region: addr?.region ?? '', country: addr?.country ?? 'HT', postalCode: addr?.postalCode ?? '',
    });
  }

  private handleCommissionSubmitError(err: unknown): void {
    const problem = (err as { error?: ProblemDetail })?.error;
    if (problem) {
      const fieldErrors = withResolvedErrorCopies(
        webAppErrorsFromProblemDetailFields(problem, 'admin.businessProfile.commission'),
        key => this.translate.instant(key),
      );
      const remaining = applyServerFieldErrors(this.commissionForm, fieldErrors, {
        'commission.rate': 'rate',
        'admin.businessProfile.commission.rate': 'rate',
      });

      if (fieldErrors.length && !remaining.length) {
        this.commissionFormError.set(null);
        return;
      }
    }

    this.commissionFormError.set(this.errorViewModel(err, 'admin.businessProfile.commission', 'section').message);
  }

  private handleAddressSubmitError(err: unknown): void {
    const problem = (err as { error?: ProblemDetail })?.error;
    if (problem) {
      const fieldErrors = withResolvedErrorCopies(
        webAppErrorsFromProblemDetailFields(problem, 'admin.businessProfile.address'),
        key => this.translate.instant(key),
      );
      const remaining = applyServerFieldErrors(this.addressForm, fieldErrors, {
        'address.line1': 'line1',
        'admin.businessProfile.address.line1': 'line1',
        'tenant.address.line1': 'line1',
        'address.line2': 'line2',
        'admin.businessProfile.address.line2': 'line2',
        'tenant.address.line2': 'line2',
        'address.city': 'city',
        'admin.businessProfile.address.city': 'city',
        'tenant.address.city': 'city',
        'address.region': 'region',
        'admin.businessProfile.address.region': 'region',
        'tenant.address.region': 'region',
        'address.country': 'country',
        'admin.businessProfile.address.country': 'country',
        'tenant.address.country': 'country',
        'address.postalCode': 'postalCode',
        'admin.businessProfile.address.postalCode': 'postalCode',
        'tenant.address.postalCode': 'postalCode',
      });

      if (fieldErrors.length && !remaining.length) {
        this.addressFormError.set(null);
        return;
      }
    }

    this.addressFormError.set(this.errorViewModel(err, ADDRESS_SECTION_TARGET, 'section').message);
  }

  private handleIdentitySubmitError(err: unknown): void {
    const problem = (err as { error?: ProblemDetail })?.error;
    if (problem) {
      const fieldErrors = withResolvedErrorCopies(
        webAppErrorsFromProblemDetailFields(problem, IDENTITY_SECTION_TARGET),
        key => this.translate.instant(key),
      );
      const remaining = applyServerFieldErrors(this.identityForm, fieldErrors, {
        name: 'name',
        'tenant.name': 'name',
        'identity.name': 'name',
        'admin.businessProfile.identity.name': 'name',
      });

      if (fieldErrors.length && !remaining.length) {
        this.identityFormError.set(null);
        return;
      }
    }

    this.identityFormError.set(this.errorViewModel(err, IDENTITY_SECTION_TARGET, 'section').message);
  }

  private handleRegionSubmitError(err: unknown): void {
    const problem = (err as { error?: ProblemDetail })?.error;
    if (problem) {
      const fieldErrors = withResolvedErrorCopies(
        webAppErrorsFromProblemDetailFields(problem, REGION_SECTION_TARGET),
        key => this.translate.instant(key),
      );
      const remaining = applyServerFieldErrors(this.regionForm, fieldErrors, {
        timezone: 'timezone',
        currency: 'currency',
        'tenant.timezone': 'timezone',
        'tenant.currency': 'currency',
        'identity.timezone': 'timezone',
        'identity.currency': 'currency',
        'admin.businessProfile.region.timezone': 'timezone',
        'admin.businessProfile.region.currency': 'currency',
      });

      if (fieldErrors.length && !remaining.length) {
        this.regionFormError.set(null);
        return;
      }
    }

    this.regionFormError.set(this.errorViewModel(err, REGION_SECTION_TARGET, 'section').message);
  }

  private sectionErrorFromUnknown(err: unknown, target: string): AdminSectionTargetError {
    const problem = (err as { error?: ProblemDetail })?.error;
    if (problem) {
      const normalized = webAppErrorFromProblemDetail(problem, target, 'section');
      const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
      return {
        target,
        severity: normalized.severity,
        title: copy.title,
        message: copy.message,
      };
    }

    return {
      target,
      severity: 'warn',
      title: this.translate.instant('common.errors.categories.service_unavailable.title'),
      message: this.translate.instant('common.errors.categories.service_unavailable.message'),
    };
  }

  private errorViewModel(
    err: unknown,
    source: string,
    surface: 'page' | 'section',
  ): ErrorViewModel {
    const problem = (err as { error?: ProblemDetail })?.error;
    if (problem) {
      const normalized = webAppErrorFromProblemDetail(problem, source, surface);
      const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
      return toErrorViewModel(normalized, copy);
    }

    return {
      title: this.translate.instant('common.errors.fallback.title'),
      message: this.translate.instant('common.errors.fallback.message'),
      severity: 'error',
    };
  }

  private setSectionError(error: AdminSectionTargetError): void {
    this.sectionErrors.update(errors => [
      ...errors.filter(item => item.target !== error.target),
      error,
    ]);
  }

  private clearSectionError(target: string): void {
    this.sectionErrors.update(errors => errors.filter(item => item.target !== target));
  }
}
