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
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subject, debounceTime, takeUntil } from 'rxjs';

import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { AdminPageShellComponent } from '../../../private/shared/admin-ui/admin-page-shell.component';
import { AdminSectionCardComponent } from '../../../private/shared/admin-ui/admin-section-card.component';
import {
  AdminStatusPillComponent,
  AdminStatusTone,
} from '../../../private/shared/admin-ui/admin-status-pill.component';
import {
  PlatformProvisioningApi,
  TenantProvisioningPreviewView,
  TenantProvisioningProfile,
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

@Component({
  selector: 'tch-platform-tenant-create-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    AdminPageShellComponent,
    AdminSectionCardComponent,
    AdminStatusPillComponent,
    TchLoading,
    TchErrorPanel,
    MatButtonModule,
    MatChipsModule,
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
  private readonly fb = inject(FormBuilder);
  private readonly destroy$ = new Subject<void>();

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
    code: [
      '',
      [Validators.required, Validators.pattern(/^[a-z0-9-]{3,30}$/)],
    ],
    name: ['', Validators.required],
    type: ['' as TenantType, Validators.required],
    timezone: ['America/Port-au-Prince', Validators.required],
    currency: ['HTG', Validators.required],
    profile: ['DEFAULT_HAITI_LOTTERY' as TenantProvisioningProfile, Validators.required],
    initialAdminEmail: ['', [Validators.email]],
  });

  readonly selectedProfileDesc = computed(() => {
    const v = this.form.controls.profile.value;
    return PROFILES.find(p => p.value === v)?.description ?? null;
  });

  readonly readinessTone = computed((): AdminStatusTone => {
    const s = this.result()?.readiness.status;
    if (s === 'READY') return 'success';
    if (s === 'INCOMPLETE') return 'warning';
    if (s === 'MISSING') return 'danger';
    return 'neutral';
  });

  readonly domainEntries = computed(() => {
    const ds = this.result()?.domainStatuses ?? {};
    return Object.entries(ds).map(([key, value]) => ({ key, value }));
  });

  ngOnInit(): void {
    this.form.valueChanges
      .pipe(debounceTime(500), takeUntil(this.destroy$))
      .subscribe(() => {
        if (this.form.valid) {
          this.loadPreview();
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadPreview(): void {
    const v = this.form.value;
    const req = {
      code: v.code!,
      name: v.name!,
      type: v.type!,
      timezone: v.timezone!,
      currency: v.currency!,
      profile: v.profile!,
      initialAdminEmail: v.initialAdminEmail || null,
    };

    this.previewLoading.set(true);
    this.previewError.set(null);

    this.api.preview(req).subscribe({
      next: res => {
        this.preview.set(res);
        this.previewLoading.set(false);
      },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string } })?.error;
        this.previewError.set(pd?.title ?? 'Erreur lors du calcul du profil.');
        this.previewLoading.set(false);
      },
    });
  }

  domainTone(status: string): AdminStatusTone {
    const s = status?.toUpperCase();
    if (s === 'OK' || s === 'DONE' || s === 'SUCCESS') return 'success';
    if (s === 'WARNING' || s === 'PARTIAL') return 'warning';
    if (s === 'ERROR' || s === 'FAILED') return 'danger';
    return 'neutral';
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) return;

    const v = this.form.value;
    const req = {
      code: v.code!,
      name: v.name!,
      type: v.type!,
      timezone: v.timezone!,
      currency: v.currency!,
      profile: v.profile!,
      initialAdminEmail: v.initialAdminEmail || null,
    };

    this.submitting.set(true);
    this.submitError.set(null);

    this.api.provision(req).subscribe({
      next: res => {
        this.submitting.set(false);
        this.result.set(res);
        this.snackBar.open('Tenant provisionné avec succès.', 'OK', { duration: 4000 });
      },
      error: (err: unknown) => {
        this.submitting.set(false);
        const pd = (err as { error?: { title?: string } })?.error;
        this.submitError.set(pd?.title ?? 'Erreur lors du provisionnement.');
      },
    });
  }
}
