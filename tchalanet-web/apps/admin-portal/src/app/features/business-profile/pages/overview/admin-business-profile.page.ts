import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { form, maxLength, required, submit } from '@angular/forms/signals';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import {
  mapHttpErrorToProblemDetail,
  webAppErrorFromProblemDetail,
  webAppErrorsFromProblemDetailFields,
} from '@tch/api';
import { RuntimeSettingsStore } from '@tch/shared-config';
import {
  BadgeStatus,
  TchErrorPanel,
  TchFieldError,
  TchFormErrorSummary,
  TchLoading,
  TchNotice,
  TchStatusBadge,
} from '@tch/ui/components';

import { AdminDetailLayoutComponent } from '@tch/ui/console';
import { AdminPageShellComponent } from '@tch/ui/console';
import { AdminSectionCardComponent } from '@tch/ui/console';
import { AdminSectionErrorTargetDirective, AdminSectionTargetError } from '@tch/ui/console';
import { AdminStatusTone } from '@tch/ui/console';
import { TchIdentityCardComponent } from '@tch/ui/console';
import {
  ConsoleAddressFormSectionComponent,
  ConsoleAddressSummaryComponent,
} from '@tch/web/console';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import {
  applyServerFieldErrors,
  clearServerFieldErrors,
  clearServerFieldErrorsOnEdit,
  ErrorViewModel,
  toErrorViewModel,
  withResolvedErrorCopies,
} from '@tch/web/errors';
import { tchMutation } from '@tch/web/async';
import {
  TenantAdminOverviewApiService,
  AddressView,
  TenantAdminOverviewView,
  TenantHeader,
} from '../../data-access/tenant-admin-overview-api.service';
import { TenantGeneralConfigApiService } from '../../data-access/tenant-general-config-api.service';
import { TenantCommercialConfigApiService } from '../../data-access/tenant-commercial-config-api.service';
import {
  TenantParametersApiService,
  TenantInternalConfig,
} from '../../../setup/data-access/tenant-parameters-api.service';

type PageState = 'loading' | 'ready' | 'error';
type FormState = 'idle' | 'submitting' | 'error' | 'success';

interface AddressFormModel {
  readonly line1: string;
  readonly line2: string;
  readonly city: string;
  readonly region: string;
  readonly country: string;
  readonly postalCode: string;
}

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
    RouterLink,
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
    ConsoleAddressFormSectionComponent,
    ConsoleAddressSummaryComponent,
    MatSelectModule,
    TchLoading,
    TchErrorPanel,
    TchFieldError,
    TchFormErrorSummary,
    TchNotice,
    TchStatusBadge,
  ],
  templateUrl: './admin-business-profile.page.html',
  styleUrls: ['./admin-business-profile.page.scss'],
})
export class AdminBusinessProfilePage implements OnInit {
  private readonly api = inject(TenantAdminOverviewApiService);
  private readonly generalConfigApi = inject(TenantGeneralConfigApiService);
  private readonly commercialConfigApi = inject(TenantCommercialConfigApiService);
  private readonly parametersApi = inject(TenantParametersApiService);
  private readonly runtimeStore = inject(RuntimeSettingsStore);
  private readonly fb = inject(FormBuilder);
  private readonly translate = inject(TranslateService);
  private readonly destroyRef = inject(DestroyRef);

  readonly pageState = signal<PageState>('loading');
  readonly pageError = signal<ErrorViewModel | null>(null);
  readonly overview = signal<TenantAdminOverviewView | null>(null);
  readonly commissionRate = signal<number | null>(null);
  readonly sectionErrors = signal<readonly AdminSectionTargetError[]>([]);

  // Identity form — name only
  readonly showIdentityForm = signal(false);
  readonly identityFormState = signal<FormState>('idle');
  readonly identityFormError = signal<string | null>(null);
  readonly identityFormSummary = signal<readonly string[]>([]);
  readonly identityForm = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(200)]],
  });

  // Region form — timezone + currency
  readonly showRegionForm = signal(false);
  readonly regionFormState = signal<FormState>('idle');
  readonly regionFormError = signal<string | null>(null);
  readonly regionFormSummary = signal<readonly string[]>([]);
  readonly regionForm = this.fb.group({
    timezone: ['', Validators.maxLength(100)],
    currency: ['', Validators.maxLength(10)],
  });

  // Commission form
  readonly showCommissionForm = signal(false);
  readonly commissionFormState = signal<FormState>('idle');
  readonly commissionFormError = signal<string | null>(null);
  readonly commissionFormSummary = signal<readonly string[]>([]);
  readonly commissionForm = this.fb.group({
    rate: [null as number | null, [Validators.required, Validators.min(0), Validators.max(100)]],
  });

  // Address form
  readonly showAddressForm = signal(false);
  readonly addressFormState = signal<FormState>('idle');
  readonly addressFormError = signal<string | null>(null);
  readonly addressFormSummary = signal<readonly string[]>([]);
  readonly addressModel = signal<AddressFormModel>({
    line1: '',
    line2: '',
    city: '',
    region: '',
    country: 'HT',
    postalCode: '',
  });
  readonly addressForm = form(this.addressModel, path => {
    required(path.line1, { message: 'common.validation.required' });
    maxLength(path.line1, 200, { message: 'common.validation.invalid' });
    maxLength(path.line2, 200, { message: 'common.validation.invalid' });
    required(path.city, { message: 'common.validation.required' });
    maxLength(path.city, 100, { message: 'common.validation.invalid' });
    maxLength(path.region, 100, { message: 'common.validation.invalid' });
    required(path.country, { message: 'common.validation.required' });
    maxLength(path.country, 2, { message: 'common.validation.invalid' });
    maxLength(path.postalCode, 20, { message: 'common.validation.invalid' });
  });

  // Locale form — supportedLanguages + fallbackLanguage
  readonly showLocaleForm = signal(false);
  readonly localeFormState = signal<FormState>('idle');
  readonly localeFormError = signal<string | null>(null);
  readonly localeFormSummary = signal<readonly string[]>([]);
  private readonly localeData = signal<TenantInternalConfig['locale'] | null>(null);
  readonly languages = this.runtimeStore.tenantLanguageOptions;
  readonly localeForm = this.fb.group({
    supportedLanguages: new FormControl<string[]>(
      [...this.runtimeStore.tenantSupportedLanguages()],
      { nonNullable: true, validators: [Validators.required] },
    ),
    fallbackLanguage: new FormControl<string>(
      this.runtimeStore.tenantSupportedLanguages()[0] ?? 'ht',
      { nonNullable: true, validators: [Validators.required] },
    ),
  });

  readonly saveLocale = tchMutation<NonNullable<TenantInternalConfig['locale']>, void>({
    run: locale =>
      this.parametersApi.updateSettingsSection('locale', locale, { suppressShellFeedback: true }),
    source: 'admin.businessProfile.locale',
    onSuccess: (_result, input) => {
      this.localeFormState.set('success');
      this.showLocaleForm.set(false);
      this.localeForm.markAsPristine();
      this.localeData.set(input);
      this.runtimeStore.applyTenantDashboardSettings({
        supportedLanguages: input.supportedLanguages,
        fallbackLanguage: input.fallbackLanguage,
      });
    },
    onError: err => {
      this.localeFormState.set('error');
      const fieldErrors = this.serverFieldErrors(err, 'admin.businessProfile.locale');
      const remaining = applyServerFieldErrors(this.localeForm, fieldErrors, {
        'locale.supportedLanguages': 'supportedLanguages',
        'admin.setup.locale.supportedLanguages': 'supportedLanguages',
        'tenant.locale.supportedLanguages': 'supportedLanguages',
        'locale.fallbackLanguage': 'fallbackLanguage',
        'admin.setup.locale.fallbackLanguage': 'fallbackLanguage',
        'tenant.locale.fallbackLanguage': 'fallbackLanguage',
      });
      if (remaining.length) {
        this.localeFormSummary.set(this.messagesForErrors(remaining));
        this.localeFormError.set(null);
      } else {
        this.localeFormError.set(
          this.errorViewModel(err, 'admin.businessProfile.locale', 'section').message,
        );
      }
    },
  });

  readonly supportedLanguagesLabel = computed(() => {
    const codes = this.localeData()?.supportedLanguages ?? this.runtimeStore.tenantSupportedLanguages();
    return codes.map(code => this.translate.instant(`admin.settings.runtime.languages.${code}`)).join(', ');
  });

  readonly fallbackLanguageLabel = computed(() => {
    const code = this.localeData()?.fallbackLanguage ?? this.runtimeStore.tenantSupportedLanguages()[0];
    return code ? this.translate.instant(`admin.settings.runtime.languages.${code}`) : null;
  });

  constructor() {
    const cleanup = [
      clearServerFieldErrorsOnEdit(this.identityForm),
      clearServerFieldErrorsOnEdit(this.regionForm),
      clearServerFieldErrorsOnEdit(this.commissionForm),
      clearServerFieldErrorsOnEdit(this.localeForm),
    ];
    this.destroyRef.onDestroy(() => cleanup.forEach(subscription => subscription.unsubscribe()));
  }

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
      {
        label: this.translate.instant('admin.businessProfile.field.type'),
        value: this.typeLabel(h.tenantType),
      },
      {
        label: this.translate.instant('admin.businessProfile.field.currency'),
        value: h.currency ?? na,
      },
      {
        label: this.translate.instant('admin.businessProfile.field.status'),
        value: this.statusLabel(h.tenantStatus),
      },
    ];
    const rate = this.commissionRate();
    if (rate != null) {
      meta.push({
        label: this.translate.instant('admin.businessProfile.field.commission'),
        value: `${rate} %`,
      });
    }
    return meta;
  });

  ngOnInit(): void {
    this.load();
  }

  load(
    options: {
      readonly resetFormFeedback?: boolean;
    } = {},
  ): void {
    this.pageState.set('loading');
    this.pageError.set(null);
    this.sectionErrors.set([]);
    if (options.resetFormFeedback !== false) {
      this.identityFormState.set('idle');
      this.identityFormSummary.set([]);
      this.regionFormState.set('idle');
      this.regionFormSummary.set([]);
      this.commissionFormState.set('idle');
      this.commissionFormSummary.set([]);
      this.addressFormState.set('idle');
      this.addressFormSummary.set([]);
    }
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
    this.commercialConfigApi.getCommissionOverview({ suppressShellFeedback: true }).subscribe({
      next: r => {
        this.commissionRate.set(r.tenantDefaultRate);
        this.commissionForm.patchValue({ rate: r.tenantDefaultRate });
        this.clearSectionError(COMMERCIAL_SECTION_TARGET);
      },
      error: (err: unknown) => {
        this.setSectionError(this.sectionErrorFromUnknown(err, COMMERCIAL_SECTION_TARGET));
      },
    });
    this.parametersApi.getTenantConfig({ suppressShellFeedback: true }).subscribe({
      next: data => {
        if (data?.locale) {
          this.localeData.set(data.locale);
          if (!this.localeForm.dirty) this.patchLocaleForm(data.locale);
        }
      },
      error: () => { /* form uses runtime defaults on failure */ },
    });
  }

  // ── Identity (name) ────────────────────────────────────────────

  openIdentityForm(): void {
    this.showIdentityForm.set(true);
    this.identityFormState.set('idle');
    this.identityFormError.set(null);
    this.identityFormSummary.set([]);
    clearServerFieldErrors(this.identityForm);
    this.showRegionForm.set(false);
    this.showAddressForm.set(false);
  }

  cancelIdentityForm(): void {
    this.showIdentityForm.set(false);
    this.identityFormError.set(null);
    this.identityFormSummary.set([]);
    clearServerFieldErrors(this.identityForm);
    this.identityForm.patchValue({ name: this.header()?.tenantName ?? '' });
  }

  submitIdentity(): void {
    clearServerFieldErrors(this.identityForm);
    if (this.identityForm.invalid) {
      this.identityForm.markAllAsTouched();
      return;
    }
    const h = this.header();
    if (!h) return;
    this.identityFormState.set('submitting');
    this.identityFormError.set(null);
    this.identityFormSummary.set([]);
    this.clearSectionError(IDENTITY_SECTION_TARGET);
    const v = this.identityForm.getRawValue();
    this.generalConfigApi
      .updateIdentity(
        {
          name: v.name ?? '',
          displayName: v.name ?? '',
          timezone: h.timezone ?? '',
          currency: h.currency ?? '',
        },
        { suppressShellFeedback: true },
      )
      .subscribe({
        next: () => {
          this.identityFormState.set('success');
          this.showIdentityForm.set(false);
          this.load({ resetFormFeedback: false });
        },
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
    this.regionFormSummary.set([]);
    clearServerFieldErrors(this.regionForm);
    this.showIdentityForm.set(false);
    this.showAddressForm.set(false);
  }

  cancelRegionForm(): void {
    this.showRegionForm.set(false);
    this.regionFormError.set(null);
    this.regionFormSummary.set([]);
    clearServerFieldErrors(this.regionForm);
    this.regionForm.patchValue({
      timezone: this.header()?.timezone ?? '',
      currency: this.header()?.currency ?? '',
    });
  }

  submitRegion(): void {
    clearServerFieldErrors(this.regionForm);
    if (this.regionForm.invalid) {
      this.regionForm.markAllAsTouched();
      return;
    }
    const h = this.header();
    if (!h) return;
    this.regionFormState.set('submitting');
    this.regionFormError.set(null);
    this.regionFormSummary.set([]);
    this.clearSectionError(REGION_SECTION_TARGET);
    const v = this.regionForm.getRawValue();
    this.generalConfigApi
      .updateIdentity(
        {
          name: h.tenantName ?? '',
          displayName: h.tenantName ?? '',
          timezone: v.timezone ?? '',
          currency: v.currency ?? '',
        },
        { suppressShellFeedback: true },
      )
      .subscribe({
        next: () => {
          this.regionFormState.set('success');
          this.showRegionForm.set(false);
          this.load({ resetFormFeedback: false });
        },
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
    this.commissionFormSummary.set([]);
    clearServerFieldErrors(this.commissionForm);
    this.showIdentityForm.set(false);
    this.showRegionForm.set(false);
    this.showAddressForm.set(false);
  }

  cancelCommissionForm(): void {
    this.showCommissionForm.set(false);
    this.commissionFormError.set(null);
    this.commissionFormSummary.set([]);
    clearServerFieldErrors(this.commissionForm);
    this.commissionForm.patchValue({ rate: this.commissionRate() });
  }

  submitCommission(): void {
    clearServerFieldErrors(this.commissionForm);
    if (this.commissionForm.invalid) {
      this.commissionForm.markAllAsTouched();
      return;
    }
    const rate = this.commissionForm.value.rate;
    if (rate == null) return;
    this.commissionFormState.set('submitting');
    this.commissionFormError.set(null);
    this.commissionFormSummary.set([]);
    this.clearSectionError(COMMERCIAL_SECTION_TARGET);
    this.commercialConfigApi
      .updateDefaultCommissionRate(rate, { suppressShellFeedback: true })
      .subscribe({
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
    this.addressFormSummary.set([]);
    this.showIdentityForm.set(false);
    this.showRegionForm.set(false);
    this.showCommissionForm.set(false);
  }

  cancelAddressForm(): void {
    this.showAddressForm.set(false);
    this.addressFormError.set(null);
    this.addressFormSummary.set([]);
    this.prefillAddressForm(this.header()?.address ?? null);
  }

  submitAddress(): void {
    submit(this.addressForm, async () => {
      this.addressFormState.set('submitting');
      this.addressFormError.set(null);
      this.addressFormSummary.set([]);
      this.clearSectionError(ADDRESS_SECTION_TARGET);
      const v = this.addressModel();
      this.generalConfigApi
        .upsertAddress(
          {
            line1: v.line1,
            line2: v.line2 || null,
            city: v.city,
            region: v.region || null,
            country: v.country,
            postalCode: v.postalCode || null,
          },
          { suppressShellFeedback: true },
        )
        .subscribe({
          next: () => {
            this.addressFormState.set('success');
            this.showAddressForm.set(false);
            this.load({ resetFormFeedback: false });
          },
          error: (err: unknown) => {
            this.handleAddressSubmitError(err);
            this.addressFormState.set('error');
          },
        });
    });
  }

  // ── Locale (supportedLanguages + fallbackLanguage) ─────────────

  openLocaleForm(): void {
    this.showLocaleForm.set(true);
    this.localeFormState.set('idle');
    this.localeFormError.set(null);
    this.localeFormSummary.set([]);
    clearServerFieldErrors(this.localeForm);
    this.showIdentityForm.set(false);
    this.showRegionForm.set(false);
    this.showCommissionForm.set(false);
    this.showAddressForm.set(false);
  }

  cancelLocaleForm(): void {
    this.showLocaleForm.set(false);
    this.localeFormError.set(null);
    this.localeFormSummary.set([]);
    clearServerFieldErrors(this.localeForm);
    this.patchLocaleForm(this.localeData());
  }

  submitLocale(): void {
    clearServerFieldErrors(this.localeForm);
    this.saveLocale.clearFeedback();
    if (this.localeForm.invalid) {
      this.localeForm.markAllAsTouched();
      return;
    }
    const v = this.localeForm.getRawValue();
    const supportedLanguages = [...new Set(v.supportedLanguages)];
    const fallbackLanguage = supportedLanguages.includes(v.fallbackLanguage)
      ? v.fallbackLanguage
      : supportedLanguages[0] ?? '';
    this.localeForm.controls.supportedLanguages.setValue(supportedLanguages);
    this.localeForm.controls.fallbackLanguage.setValue(fallbackLanguage);
    this.saveLocale.execute({
      ...(this.localeData() ?? {}),
      supportedLanguages,
      fallbackLanguage: fallbackLanguage || null,
    });
  }

  // ── Labels ─────────────────────────────────────────────────────

  statusBadge(status: string | null | undefined): BadgeStatus {
    const map: Record<string, BadgeStatus> = {
      ACTIVE: 'ready',
      DRAFT: 'pending',
      SUSPENDED: 'warning',
      REJECTED: 'blocked',
      ARCHIVED: 'missing',
    };
    return (status ? map[status] : null) ?? 'missing';
  }

  statusTone(status: string | null | undefined): AdminStatusTone {
    const map: Record<string, AdminStatusTone> = {
      ACTIVE: 'success',
      DRAFT: 'info',
      SUSPENDED: 'warning',
      REJECTED: 'danger',
      ARCHIVED: 'danger',
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

  private patchLocaleForm(locale: TenantInternalConfig['locale'] | null): void {
    const supported = locale?.supportedLanguages ?? [...this.runtimeStore.tenantSupportedLanguages()];
    const fallback = locale?.fallbackLanguage ?? supported[0] ?? 'ht';
    this.localeForm.patchValue({ fallbackLanguage: fallback });
  }

  private prefillForms(h: TenantHeader | null): void {
    this.identityForm.patchValue({ name: h?.tenantName ?? '' });
    this.regionForm.patchValue({ timezone: h?.timezone ?? '', currency: h?.currency ?? '' });
    this.prefillAddressForm(h?.address ?? null);
  }

  private prefillAddressForm(addr: AddressView | null): void {
    this.addressModel.set({
      line1: addr?.line1 ?? '',
      line2: addr?.line2 ?? '',
      city: addr?.city ?? '',
      region: addr?.region ?? '',
      country: addr?.country ?? 'HT',
      postalCode: addr?.postalCode ?? '',
    });
  }

  private handleCommissionSubmitError(err: unknown): void {
    const fieldErrors = this.serverFieldErrors(err, 'admin.businessProfile.commission');
    const remaining = applyServerFieldErrors(this.commissionForm, fieldErrors, {
      'commission.rate': 'rate',
      'admin.businessProfile.commission.rate': 'rate',
    });

    if (fieldErrors.length && !remaining.length) {
      this.commissionFormError.set(null);
      return;
    }

    if (remaining.length) {
      this.commissionFormSummary.set(this.messagesForErrors(remaining));
      this.commissionFormError.set(null);
      return;
    }

    this.commissionFormError.set(
      this.errorViewModel(err, 'admin.businessProfile.commission', 'section').message,
    );
  }

  private handleAddressSubmitError(err: unknown): void {
    const fieldErrors = this.serverFieldErrors(err, 'admin.businessProfile.address');
    if (fieldErrors.length) {
      this.addressFormSummary.set(this.messagesForErrors(fieldErrors));
      this.addressFormError.set(null);
      return;
    }

    this.addressFormError.set(this.errorViewModel(err, ADDRESS_SECTION_TARGET, 'section').message);
  }

  private handleIdentitySubmitError(err: unknown): void {
    const fieldErrors = this.serverFieldErrors(err, IDENTITY_SECTION_TARGET);
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

    if (remaining.length) {
      this.identityFormSummary.set(this.messagesForErrors(remaining));
      this.identityFormError.set(null);
      return;
    }

    this.identityFormError.set(
      this.errorViewModel(err, IDENTITY_SECTION_TARGET, 'section').message,
    );
  }

  private handleRegionSubmitError(err: unknown): void {
    const fieldErrors = this.serverFieldErrors(err, REGION_SECTION_TARGET);
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

    if (remaining.length) {
      this.regionFormSummary.set(this.messagesForErrors(remaining));
      this.regionFormError.set(null);
      return;
    }

    this.regionFormError.set(this.errorViewModel(err, REGION_SECTION_TARGET, 'section').message);
  }

  private sectionErrorFromUnknown(err: unknown, target: string): AdminSectionTargetError {
    const normalized = webAppErrorFromProblemDetail(
      mapHttpErrorToProblemDetail(err),
      target,
      'section',
    );
    const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
    return {
      target,
      severity: normalized.severity,
      title: copy.title,
      message: copy.message,
    };
  }

  private serverFieldErrors(err: unknown, target: string) {
    return withResolvedErrorCopies(
      webAppErrorsFromProblemDetailFields(mapHttpErrorToProblemDetail(err), target),
      key => this.translate.instant(key),
    );
  }

  private messagesForErrors(errors: readonly { readonly message?: string }[]): readonly string[] {
    return errors.flatMap(error => (error.message ? [error.message] : []));
  }

  private errorViewModel(
    err: unknown,
    source: string,
    surface: 'page' | 'section',
  ): ErrorViewModel {
    const normalized = webAppErrorFromProblemDetail(
      mapHttpErrorToProblemDetail(err),
      source,
      surface,
    );
    const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
    return toErrorViewModel(normalized, copy);
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
