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
import { Router, RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { EMPTY, Subject, catchError, debounceTime, filter, switchMap, takeUntil, tap } from 'rxjs';

import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { TchActionButton, TchErrorPanel, TchNotice, TchSubmitButton } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { AdminDetailLayoutComponent } from '../../../../shared/admin-ui/components/admin-detail-layout/admin-detail-layout.component';
import { AdminNextStepsCardComponent, AdminNextStep } from '../../../../shared/admin-ui/components/admin-next-steps-card/admin-next-steps-card.component';
import { AdminPageShellComponent } from '../../../../shared/admin-ui/admin-page-shell.component';
import {
  AdminProvisioningHealthCardComponent,
  AdminProvisioningHealthError,
} from '../../../../shared/admin-ui/components/admin-provisioning-health-card/admin-provisioning-health-card.component';
import { AdminSectionCardComponent } from '../../../../shared/admin-ui/admin-section-card.component';
import { AdminStatusTone } from '../../../../shared/admin-ui/admin-status-pill.component';
import { TchIdentityCardComponent } from '../../../../shared/admin-ui/components/tch-identity-card/tch-identity-card.component';
import {
  PlatformProvisioningApi,
  TenantProvisioningPreviewView,
  TenantProvisioningProfile,
  TenantProvisioningRequest,
  TenantProvisioningResultView,
  TenantType,
} from '../../data-access/platform-provisioning-api.service';

const TENANT_TYPES: { value: TenantType; label: string; icon: string; descriptionKey: string }[] = [
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

const DOMAIN_LABEL_KEYS: Record<string, string> = {
  tenant_identity: 'platform.tenantProvisioning.domain.tenantIdentity',
  pagemodels: 'platform.tenantProvisioning.domain.pagemodels',
  theme: 'platform.tenantProvisioning.domain.theme',
  settings: 'platform.tenantProvisioning.domain.settings',
  i18n: 'platform.tenantProvisioning.domain.i18n',
  games: 'platform.tenantProvisioning.domain.games',
  pricing: 'platform.tenantProvisioning.domain.pricing',
  draw_channels: 'platform.tenantProvisioning.domain.drawChannels',
  promotions_templates: 'platform.tenantProvisioning.domain.promotionsTemplates',
  limits_templates: 'platform.tenantProvisioning.domain.limitsTemplates',
  demo_users: 'platform.tenantProvisioning.domain.demoUsers',
  demo_seller_terminals: 'platform.tenantProvisioning.domain.demoSellerTerminals',
};

const STATUS_LABEL_KEYS: Record<string, string> = {
  CREATED: 'platform.tenantProvisioning.status.created',
  SEEDED_VIA_LISTENER: 'platform.tenantProvisioning.status.seeded',
  DEFAULT: 'platform.tenantProvisioning.status.default',
  DEFAULT_LOTTERY: 'platform.tenantProvisioning.status.defaultLottery',
  DEFAULT_HAITI: 'platform.tenantProvisioning.status.defaultHaiti',
  TEMPLATES_AVAILABLE: 'platform.tenantProvisioning.status.templatesAvailable',
  NONE: 'platform.tenantProvisioning.status.none',
};

const STEP_LABEL_KEYS: Record<string, string> = {
  CREATE_INITIAL_ADMIN: 'platform.tenantProvisioning.step.createInitialAdmin',
  CONFIGURE_GAMES: 'platform.tenantProvisioning.step.configureGames',
  CONFIGURE_DRAW_CHANNELS: 'platform.tenantProvisioning.step.configureDrawChannels',
  CREATE_SELLER_TERMINAL: 'platform.tenantProvisioning.step.createSellerTerminal',
  CONFIGURE_SELLER_RULES: 'platform.tenantProvisioning.step.configureSellerRules',
  CONFIGURE_LIMITS: 'platform.tenantProvisioning.step.configureLimits',
  CONFIGURE_ODDS: 'platform.tenantProvisioning.step.configureOdds',
  VERIFY_DEMO_SETUP: 'platform.tenantProvisioning.step.verifyDemoSetup',
};

// Backend warning codes → human messages. Never surface a raw code in the UI.
const WARNING_LABEL_KEYS: Record<string, string> = {
  INITIAL_ADMIN_EMAIL_MISSING: 'platform.tenantProvisioning.warning.initialAdminMissing',
  EXISTING_USER_ATTACHED: 'platform.tenantProvisioning.warning.existingUserAttached',
  TEMPORARY_CREDENTIAL_NOT_RETURNED: 'platform.tenantProvisioning.warning.temporaryCredentialNotReturned',
  TEMPORARY_PASSWORD_ISSUED: 'platform.tenantProvisioning.warning.temporaryPasswordIssued',
};

const STEP_ICONS: Record<string, string> = {
  CREATE_INITIAL_ADMIN: 'admin_panel_settings',
  CONFIGURE_GAMES: 'casino',
  CONFIGURE_DRAW_CHANNELS: 'schedule',
  CREATE_SELLER_TERMINAL: 'point_of_sale',
  CONFIGURE_SELLER_RULES: 'tune',
  CONFIGURE_LIMITS: 'speed',
  CONFIGURE_ODDS: 'percent',
  VERIFY_DEMO_SETUP: 'checklist',
};

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
    AdminNextStepsCardComponent,
    TchIdentityCardComponent,
    AdminProvisioningHealthCardComponent,
    TchActionButton,
    TchErrorPanel,
    TchNotice,
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
  private readonly router = inject(Router);
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

  readonly readinessLabel = computed(() => {
    if (this.result()) {
      return this.result()!.readiness.status;
    }
    if (this.preview()) {
      return 'Prévisualisé';
    }
    return 'À valider';
  });

  readonly readinessTone = computed((): AdminStatusTone => {
    const resultStatus = this.result()?.readiness.status;
    if (resultStatus === 'READY') return 'success';
    if (resultStatus === 'INCOMPLETE') return 'warning';
    if (resultStatus === 'MISSING') return 'danger';
    if (this.previewError()) return 'danger';
    if (this.preview()) return 'info';
    return 'neutral';
  });

  readonly healthProgress = computed(() => {
    const sections = this.result()?.readiness.sections ?? [];
    if (!sections.length) {
      return null;
    }
    const ready = sections.filter(section => this.domainTone(section.status) === 'success').length;
    return Math.round((ready / sections.length) * 100);
  });

  readonly healthItems = computed(() => {
    const domainStatuses = this.result()?.domainStatuses;
    if (domainStatuses && Object.keys(domainStatuses).length) {
      return Object.entries(domainStatuses).map(([key, value]) => ({
        label: this.label(DOMAIN_LABEL_KEYS[key] ?? key),
        status: this.label(STATUS_LABEL_KEYS[String(value)] ?? String(value)),
        tone: this.domainTone(String(value)),
      }));
    }

    const preview = this.preview();
    if (!preview) {
      return [];
    }

    const domains = preview.includedDomains.map(domain => ({
      label: this.label(DOMAIN_LABEL_KEYS[domain] ?? domain),
      status: this.label('platform.tenantProvisioning.status.planned'),
      tone: 'info' as AdminStatusTone,
    }));
    const sections = preview.expectedReadinessSections.map(section => ({
      label: this.label(DOMAIN_LABEL_KEYS[section] ?? section),
      status: this.label('platform.tenantProvisioning.status.expected'),
      tone: 'neutral' as AdminStatusTone,
    }));

    return [...domains, ...sections];
  });

  readonly previewDomainLabels = computed(() => {
    const preview = this.preview();
    if (!preview?.includedDomains?.length) return [];
    return preview.includedDomains.map(domain =>
      this.label(DOMAIN_LABEL_KEYS[domain] ?? domain),
    );
  });

  readonly nextStepsItems = computed((): AdminNextStep[] => {
    const result = this.result();
    if (!result?.nextSteps?.length) return [];
    return result.nextSteps.map(step => ({
      icon: STEP_ICONS[step] ?? 'arrow_forward',
      label: this.translate.instant(STEP_LABEL_KEYS[step] ?? step),
      routerLink: step === 'CREATE_INITIAL_ADMIN' && result.tenantId
        ? ['/app/platform/tenants', result.tenantId, 'admins', 'new']
        : undefined,
    }));
  });

  readonly successNotices = computed(() => {
    const result = this.result();
    if (!result) return null;
    const warnings = result.warnings ?? [];
    return { tenantCode: result.tenantCode, warnings };
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

  domainTone(status: string): AdminStatusTone {
    const normalized = status?.toUpperCase();
    if (['OK', 'DONE', 'READY', 'SUCCESS', 'ACTIVE'].includes(normalized)) {
      return 'success';
    }
    if (['WARNING', 'PARTIAL', 'INCOMPLETE'].includes(normalized)) {
      return 'warning';
    }
    if (['ERROR', 'FAILED', 'MISSING', 'BLOCKED'].includes(normalized)) {
      return 'danger';
    }
    if (['PENDING', 'EXPECTED', 'PREVU', 'PRÉVU'].includes(normalized)) {
      return 'info';
    }
    return 'neutral';
  }

  navigateToList(): void {
    void this.router.navigate(['/app/platform/tenants']);
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

  private label(keyOrValue: string): string {
    return keyOrValue.startsWith('platform.')
      ? this.translate.instant(keyOrValue)
      : keyOrValue;
  }

  /** Map a backend warning code to a human message; raw codes never reach the UI. */
  warningLabel(code: string): string {
    const key = WARNING_LABEL_KEYS[code];
    return key
      ? this.translate.instant(key)
      : this.translate.instant('platform.tenantProvisioning.warning.fallback');
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
