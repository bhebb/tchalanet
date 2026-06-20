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
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subject, debounceTime, takeUntil } from 'rxjs';

import { TchErrorPanel } from '@tch/ui/components';
import { AdminDetailLayoutComponent } from '../../../private/shared/admin-ui/components/admin-detail-layout/admin-detail-layout.component';
import { AdminPageShellComponent } from '../../../private/shared/admin-ui/admin-page-shell.component';
import { AdminProvisioningHealthCardComponent } from '../../../private/shared/admin-ui/components/admin-provisioning-health-card/admin-provisioning-health-card.component';
import { AdminSectionCardComponent } from '../../../private/shared/admin-ui/admin-section-card.component';
import { AdminStatusTone } from '../../../private/shared/admin-ui/admin-status-pill.component';
import { TchIdentityCardComponent } from '../../../private/shared/admin-ui/components/tch-identity-card/tch-identity-card.component';
import {
  PlatformProvisioningApi,
  TenantProvisioningPreviewView,
  TenantProvisioningProfile,
  TenantProvisioningRequest,
  TenantProvisioningResultView,
  TenantType,
} from '../../platform-provisioning-api.service';

const TENANT_TYPES: { value: TenantType; label: string }[] = [
  { value: 'BORLETTE', label: 'Borlette' },
  { value: 'RESEAU', label: 'Réseau' },
  { value: 'AMBULANT', label: 'Ambulant' },
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
    TchIdentityCardComponent,
    AdminProvisioningHealthCardComponent,
    TchErrorPanel,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatIconModule,
    MatChipsModule,
  ],
  templateUrl: './platform-tenant-provisioning.page.html',
  styleUrls: ['./platform-tenant-provisioning.page.scss'],
})
export class PlatformTenantProvisioningPage implements OnInit, OnDestroy {
  private readonly api = inject(PlatformProvisioningApi);
  private readonly snackBar = inject(MatSnackBar);
  private readonly translate = inject(TranslateService);
  private readonly fb = inject(FormBuilder);
  private readonly destroy$ = new Subject<void>();
  private readonly formRevision = signal(0);

  readonly tenantTypes = TENANT_TYPES;
  readonly profiles = PROFILES;
  readonly timezones = TIMEZONES;
  readonly currencies = CURRENCIES;

  readonly submitting = signal(false);
  readonly submitError = signal<string | null>(null);
  readonly previewLoading = signal(false);
  readonly previewError = signal<string | null>(null);
  readonly preview = signal<TenantProvisioningPreviewView | null>(null);
  readonly result = signal<TenantProvisioningResultView | null>(null);

  readonly form = this.fb.group({
    code: ['', [Validators.required, Validators.pattern(/^[a-z0-9-]{3,30}$/)]],
    name: ['', Validators.required],
    type: ['' as TenantType, Validators.required],
    timezone: ['America/Port-au-Prince', Validators.required],
    currency: ['HTG', Validators.required],
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
    return [
      { label: 'Type', value: this.form.controls.type.value || '—' },
      { label: 'Devise', value: this.form.controls.currency.value || '—' },
      { label: 'Fuseau', value: this.form.controls.timezone.value || '—' },
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

  ngOnInit(): void {
    this.form.valueChanges
      .pipe(debounceTime(500), takeUntil(this.destroy$))
      .subscribe(() => {
        this.formRevision.update(value => value + 1);
        if (this.form.valid) {
          this.loadPreview();
        }
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

  stepLabelKey(step: string): string {
    return STEP_LABEL_KEYS[step] ?? step;
  }

  private label(keyOrValue: string): string {
    return keyOrValue.startsWith('platform.')
      ? this.translate.instant(keyOrValue)
      : keyOrValue;
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
        this.snackBar.open('Tenant provisionné avec succès.', 'OK', { duration: 4000 });
      },
      error: (err: unknown) => {
        this.submitting.set(false);
        const problem = (err as { error?: { title?: string } })?.error;
        this.submitError.set(problem?.title ?? 'Erreur lors du provisionnement.');
      },
    });
  }

  private loadPreview(): void {
    this.previewLoading.set(true);
    this.previewError.set(null);

    this.api.preview(this.requestFromForm()).subscribe({
      next: preview => {
        this.preview.set(preview);
        this.previewLoading.set(false);
      },
      error: (err: unknown) => {
        const problem = (err as { error?: { title?: string } })?.error;
        this.previewError.set(problem?.title ?? 'Erreur lors du calcul du profil.');
        this.previewLoading.set(false);
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
      profile: value.profile!,
      initialAdminEmail: value.initialAdminEmail || null,
    };
  }
}
