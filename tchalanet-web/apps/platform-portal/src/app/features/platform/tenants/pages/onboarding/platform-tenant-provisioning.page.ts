import {
  ChangeDetectionStrategy,
  Component,
  OnDestroy,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { EMPTY, Subject, catchError, debounceTime, filter, switchMap, takeUntil, tap } from 'rxjs';

import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { TchActionButton, TchErrorPanel, TchSubmitButton } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { AdminDetailLayoutComponent } from '@tch/ui/console';
import { AdminPageShellComponent } from '@tch/ui/console';
import { AdminProvisioningHealthError } from '@tch/ui/console';
import { AdminSectionCardComponent } from '@tch/ui/console';
import {
  PlatformProvisioningApi,
  TenantProvisioningPreviewView,
  TenantProvisioningProfile,
  TenantProvisioningRequest,
  TenantProvisioningResultView,
  TenantType,
} from '../../data-access/platform-provisioning-api.service';
import {
  TenantTypeOption,
  TenantTypePickerComponent,
} from './components/tenant-type-picker/tenant-type-picker.component';
import { TenantProvisioningAsideComponent } from './components/tenant-provisioning-aside/tenant-provisioning-aside.component';
import { TenantProvisioningProfileSummaryComponent } from './components/tenant-provisioning-profile-summary/tenant-provisioning-profile-summary.component';
import { TenantProvisioningSuccessComponent } from './components/tenant-provisioning-success/tenant-provisioning-success.component';

const TENANT_TYPES: TenantTypeOption[] = [
  { value: 'BORLETTE', label: 'Borlette', icon: 'storefront', descriptionKey: 'platform.tenantProvisioning.typeDescription.borlette' },
  { value: 'RESEAU', label: 'Réseau', icon: 'hub', descriptionKey: 'platform.tenantProvisioning.typeDescription.reseau' },
  { value: 'AMBULANT', label: 'Ambulant', icon: 'directions_walk', descriptionKey: 'platform.tenantProvisioning.typeDescription.ambulant' },
];

const PROFILES: { value: TenantProvisioningProfile; label: string; description: string }[] = [
  { value: 'MINIMAL', label: 'Minimal', description: 'Tenant vide — configuration manuelle.' },
  {
    value: 'DEFAULT_HAITI_LOTTERY',
    label: 'Haïti Loterie (défaut)',
    description: 'Catalogue Borlette standard : jeux, tirages, commissions.',
  },
  { value: 'DEMO', label: 'Démo', description: 'Données de démonstration pré-remplies.' },
];

const TIMEZONES = ['America/Port-au-Prince', 'America/New_York', 'UTC'];
const CURRENCIES = ['HTG', 'USD'];

const READINESS_PREVIEW_TARGET = 'platform.tenantProvisioning.readiness.preview';

@Component({
  selector: 'tch-platform-tenant-provisioning-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    TranslatePipe,
    ReactiveFormsModule,
    AdminPageShellComponent,
    AdminSectionCardComponent,
    AdminDetailLayoutComponent,
    TenantProvisioningAsideComponent,
    TenantTypePickerComponent,
    TenantProvisioningProfileSummaryComponent,
    TenantProvisioningSuccessComponent,
    TchActionButton,
    TchErrorPanel,
    TchSubmitButton,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './platform-tenant-provisioning.page.html',
  styleUrls: ['./platform-tenant-provisioning.page.scss'],
})
export class PlatformTenantProvisioningPage implements OnInit, OnDestroy {
  private readonly api = inject(PlatformProvisioningApi);
  private readonly translate = inject(TranslateService);
  private readonly fb = inject(FormBuilder);
  private readonly destroy$ = new Subject<void>();
  private readonly formRevision = signal(0);

  readonly tenantTypes = TENANT_TYPES;
  readonly profiles = PROFILES;
  readonly timezones = TIMEZONES;
  readonly currencies = CURRENCIES;

  readonly submitting = signal(false);
  readonly submitted = signal(false);
  readonly submitError = signal<string | null>(null);
  readonly previewLoading = signal(false);
  readonly previewError = signal<AdminProvisioningHealthError | null>(null);
  readonly preview = signal<TenantProvisioningPreviewView | null>(null);
  readonly result = signal<TenantProvisioningResultView | null>(null);

  readonly form = this.fb.group({
    code: ['', [Validators.required, Validators.pattern(/^[a-z0-9-]{3,30}$/)]],
    name: ['', Validators.required],
    type: ['' as TenantType, Validators.required],
    timezone: ['America/Port-au-Prince', Validators.required],
    currency: ['HTG', Validators.required],
    defaultCommissionRate: [
      15,
      [Validators.required, Validators.min(0), Validators.max(100)],
    ],
    profile: ['DEFAULT_HAITI_LOTTERY' as TenantProvisioningProfile, Validators.required],
    initialAdminEmail: ['', [Validators.email]],
  });

  readonly selectedProfileDesc = computed(() => {
    this.formRevision();
    const value = this.form.controls.profile.value;
    return PROFILES.find(profile => profile.value === value)?.description ?? null;
  });

  readonly identityMeta = computed(() => {
    this.formRevision();
    const commission = this.form.controls.defaultCommissionRate.value;
    return [
      { label: 'Type', value: this.form.controls.type.value || '—' },
      { label: 'Devise', value: this.form.controls.currency.value || '—' },
      { label: 'Fuseau', value: this.form.controls.timezone.value || '—' },
      {
        label: 'Commission',
        value: commission === null || commission === undefined ? '—' : `${commission} %`,
      },
      { label: 'Profil', value: this.form.controls.profile.value || '—' },
    ];
  });

  ngOnInit(): void {
    this.form.valueChanges
      .pipe(
        // Keep the live identity card in sync on every keystroke...
        tap(() => this.formRevision.update(value => value + 1)),
        debounceTime(500),
        // ...but only preview a complete, valid request.
        filter(() => this.form.valid && !this.submitted()),
        tap(() => {
          this.previewLoading.set(true);
          this.previewError.set(null);
        }),
        // switchMap cancels an in-flight preview when the form changes again → no stale result.
        switchMap(() =>
          this.api.preview(this.requestFromForm()).pipe(
            catchError((err: unknown) => {
              this.previewError.set(this.previewErrorFromUnknown(err));
              this.previewLoading.set(false);
              return EMPTY;
            }),
          ),
        ),
        takeUntil(this.destroy$),
      )
      .subscribe(preview => {
        this.preview.set(preview);
        this.previewLoading.set(false);
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.submitError.set(null);

    this.api.provision(this.requestFromForm()).subscribe({
      next: result => {
        this.submitting.set(false);
        this.result.set(result);
        this.submitted.set(true);
        this.form.disable();
      },
      error: (err: unknown) => {
        this.submitting.set(false);
        const problem = (err as { error?: { title?: string } })?.error;
        this.submitError.set(problem?.title ?? 'Erreur lors du provisionnement.');
      },
    });
  }

  private requestFromForm(): TenantProvisioningRequest {
    const value = this.form.value;
    return {
      code: value.code!,
      name: value.name!,
      type: value.type!,
      timezone: value.timezone!,
      currency: value.currency!,
      defaultCommissionRate: value.defaultCommissionRate!,
      profile: value.profile!,
      initialAdminEmail: value.initialAdminEmail || null,
    };
  }

  selectTenantType(value: TenantType): void {
    this.form.controls.type.setValue(value);
    this.form.controls.type.markAsTouched();
  }

  private previewErrorFromUnknown(err: unknown): AdminProvisioningHealthError {
    const problem = (err as { error?: ProblemDetail })?.error;
    if (problem) {
      const normalized = webAppErrorFromProblemDetail(problem, READINESS_PREVIEW_TARGET, 'section');
      const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
      return {
        severity: normalized.severity,
        title: copy.title,
        message: copy.message,
      };
    }

    return {
      severity: 'warn',
      title: this.translate.instant('common.errors.categories.service_unavailable.title'),
      message: this.translate.instant('common.errors.categories.service_unavailable.message'),
    };
  }

  /** Route to the freshly-created tenant's detail page, when the id is known. */
  tenantDetailLink(): string[] | null {
    const id = this.result()?.tenantId;
    return id ? ['/app/platform/tenants', id] : null;
  }

  tenantAdminsLink(): string[] | null {
    const id = this.result()?.tenantId;
    return id ? ['/app/platform/tenants', id, 'admins'] : null;
  }

  /** Reset for "create another tenant" — explicit, never a silent reset. */
  startAnother(): void {
    this.result.set(null);
    this.preview.set(null);
    this.previewError.set(null);
    this.submitError.set(null);
    this.submitted.set(false);
    this.form.enable();
    this.form.reset({
      code: '',
      name: '',
      type: '' as TenantType,
      timezone: 'America/Port-au-Prince',
      currency: 'HTG',
      defaultCommissionRate: 15,
      profile: 'DEFAULT_HAITI_LOTTERY' as TenantProvisioningProfile,
      initialAdminEmail: '',
    });
    this.formRevision.update(value => value + 1);
  }
}
