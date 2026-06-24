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
import {
  BadgeStatus,
  TchErrorPanel,
  TchLoading,
  TchNotice,
  TchStatusBadge,
} from '@tch/ui/components';

import { AdminDetailLayoutComponent } from '../../../../shared/admin-ui/components/admin-detail-layout/admin-detail-layout.component';
import { AdminPageShellComponent } from '../../../../shared/admin-ui/admin-page-shell.component';
import { AdminSectionCardComponent } from '../../../../shared/admin-ui/admin-section-card.component';
import { AdminStatusTone } from '../../../../shared/admin-ui/admin-status-pill.component';
import { TchIdentityCardComponent } from '../../../../shared/admin-ui/components/tch-identity-card/tch-identity-card.component';
import {
  AdminOverviewApiService,
  AddressView,
  TenantAdminOverviewView,
  TenantHeader,
} from '../../../admin-overview-api.service';

type PageState = 'loading' | 'ready' | 'error';
type FormState = 'idle' | 'submitting' | 'error' | 'success';

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
    TchIdentityCardComponent,
    TchLoading,
    TchErrorPanel,
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
  readonly pageError = signal<string | null>(null);
  readonly overview = signal<TenantAdminOverviewView | null>(null);
  readonly commissionRate = signal<number | null>(null);

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
    this.identityFormState.set('idle');
    this.regionFormState.set('idle');
    this.commissionFormState.set('idle');
    this.addressFormState.set('idle');
    this.api.getOverview().subscribe({
      next: data => {
        this.overview.set(data);
        this.pageState.set('ready');
        this.prefillForms(data.header);
      },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string } })?.error;
        this.pageError.set(pd?.title ?? this.translate.instant('admin.businessProfile.error.load'));
        this.pageState.set('error');
      },
    });
    this.api.getCommissionOverview().subscribe({
      next: r => {
        this.commissionRate.set(r.tenantDefaultRate);
        this.commissionForm.patchValue({ rate: r.tenantDefaultRate });
      },
      error: () => { /* non-blocking */ },
    });
  }

  // ── Identity (name) ────────────────────────────────────────────

  openIdentityForm(): void {
    this.showIdentityForm.set(true);
    this.identityFormState.set('idle');
    this.identityFormError.set(null);
    this.showRegionForm.set(false);
    this.showAddressForm.set(false);
  }

  cancelIdentityForm(): void {
    this.showIdentityForm.set(false);
    this.identityFormError.set(null);
    this.identityForm.patchValue({ name: this.header()?.tenantName ?? '' });
  }

  submitIdentity(): void {
    if (this.identityForm.invalid) { this.identityForm.markAllAsTouched(); return; }
    this.identityFormState.set('submitting');
    this.identityFormError.set(null);
    const h = this.header()!;
    this.api.updateIdentity({
      name: this.identityForm.value.name!,
      timezone: h.timezone ?? '',
      currency: h.currency ?? '',
    }).subscribe({
      next: () => { this.identityFormState.set('success'); this.showIdentityForm.set(false); this.load(); },
      error: (err: unknown) => {
        const pd = (err as { error?: { detail?: string; title?: string } })?.error;
        this.identityFormError.set(pd?.detail ?? pd?.title ?? this.translate.instant('admin.businessProfile.error.save'));
        this.identityFormState.set('error');
      },
    });
  }

  // ── Region (timezone + currency) ────────────────────────────────

  openRegionForm(): void {
    this.showRegionForm.set(true);
    this.regionFormState.set('idle');
    this.regionFormError.set(null);
    this.showIdentityForm.set(false);
    this.showAddressForm.set(false);
  }

  cancelRegionForm(): void {
    this.showRegionForm.set(false);
    this.regionFormError.set(null);
    this.regionForm.patchValue({ timezone: this.header()?.timezone ?? '', currency: this.header()?.currency ?? '' });
  }

  submitRegion(): void {
    this.regionFormState.set('submitting');
    this.regionFormError.set(null);
    const h = this.header()!;
    const v = this.regionForm.value;
    this.api.updateIdentity({
      name: h.tenantName ?? '',
      timezone: v.timezone ?? '',
      currency: v.currency ?? '',
    }).subscribe({
      next: () => { this.regionFormState.set('success'); this.showRegionForm.set(false); this.load(); },
      error: (err: unknown) => {
        const pd = (err as { error?: { detail?: string; title?: string } })?.error;
        this.regionFormError.set(pd?.detail ?? pd?.title ?? this.translate.instant('admin.businessProfile.error.save'));
        this.regionFormState.set('error');
      },
    });
  }

  // ── Commission ─────────────────────────────────────────────────

  openCommissionForm(): void {
    this.showCommissionForm.set(true);
    this.commissionFormState.set('idle');
    this.commissionFormError.set(null);
    this.showIdentityForm.set(false);
    this.showRegionForm.set(false);
    this.showAddressForm.set(false);
  }

  cancelCommissionForm(): void {
    this.showCommissionForm.set(false);
    this.commissionFormError.set(null);
    this.commissionForm.patchValue({ rate: this.commissionRate() });
  }

  submitCommission(): void {
    if (this.commissionForm.invalid) { this.commissionForm.markAllAsTouched(); return; }
    const rate = this.commissionForm.value.rate;
    if (rate == null) return;
    this.commissionFormState.set('submitting');
    this.commissionFormError.set(null);
    this.api.updateDefaultCommissionRate(rate).subscribe({
      next: () => {
        this.commissionFormState.set('success');
        this.showCommissionForm.set(false);
        this.commissionRate.set(rate);
      },
      error: (err: unknown) => {
        const pd = (err as { error?: { detail?: string; title?: string } })?.error;
        this.commissionFormError.set(pd?.detail ?? pd?.title ?? this.translate.instant('admin.businessProfile.error.save'));
        this.commissionFormState.set('error');
      },
    });
  }

  // ── Address ────────────────────────────────────────────────────

  openAddressForm(): void {
    this.showAddressForm.set(true);
    this.addressFormState.set('idle');
    this.addressFormError.set(null);
    this.showIdentityForm.set(false);
    this.showRegionForm.set(false);
    this.showCommissionForm.set(false);
  }

  cancelAddressForm(): void {
    this.showAddressForm.set(false);
    this.addressFormError.set(null);
    this.prefillAddressForm(this.header()?.address ?? null);
  }

  submitAddress(): void {
    if (this.addressForm.invalid) { this.addressForm.markAllAsTouched(); return; }
    this.addressFormState.set('submitting');
    this.addressFormError.set(null);
    const v = this.addressForm.value;
    this.api.upsertAddress({
      line1: v.line1!, line2: v.line2 || null, city: v.city!,
      region: v.region || null, country: v.country!, postalCode: v.postalCode || null,
    }).subscribe({
      next: () => { this.addressFormState.set('success'); this.showAddressForm.set(false); this.load(); },
      error: (err: unknown) => {
        const pd = (err as { error?: { detail?: string; title?: string } })?.error;
        this.addressFormError.set(pd?.detail ?? pd?.title ?? this.translate.instant('admin.setup.address.error.save'));
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
}
