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
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { TchErrorPanel, TchLoading, TchNotice } from '@tch/ui/components';

import { AdminPageShellComponent } from '../../../shared/admin-ui/admin-page-shell.component';
import { AdminSectionCardComponent } from '../../../shared/admin-ui/admin-section-card.component';
import { AdminEmptyStateComponent } from '../../../shared/admin-ui/admin-empty-state.component';
import {
  MeProfileResponse,
  ProfileApiService,
  UpdatePreferencesRequest,
  UpdateProfileRequest,
} from '../../data-access/profile-api.service';

type FormState = 'idle' | 'submitting' | 'error';

@Component({
  selector: 'tch-profile-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    AdminPageShellComponent,
    AdminSectionCardComponent,
    AdminEmptyStateComponent,
    TchLoading,
    TchErrorPanel,
    TchNotice,
  ],
  templateUrl: './profile.page.html',
  styleUrls: ['./profile.page.scss'],
})
export class ProfilePage implements OnInit {
  private readonly api = inject(ProfileApiService);
  private readonly fb = inject(FormBuilder);
  private readonly translate = inject(TranslateService);

  readonly loading = signal(false);
  readonly loadError = signal<string | null>(null);
  readonly profile = signal<MeProfileResponse | null>(null);

  readonly showIdentityForm = signal(false);
  readonly identityState = signal<FormState>('idle');
  readonly identityError = signal<string | null>(null);

  readonly showPrefsForm = signal(false);
  readonly prefsState = signal<FormState>('idle');
  readonly prefsError = signal<string | null>(null);

  readonly identityForm = this.fb.nonNullable.group({
    firstName: ['', Validators.maxLength(120)],
    lastName: ['', Validators.maxLength(120)],
    phone: ['', Validators.maxLength(32)],
  });

  readonly prefsForm = this.fb.nonNullable.group({
    themeMode: ['' as 'LIGHT' | 'DARK' | 'SYSTEM' | ''],
    locale: ['', Validators.maxLength(16)],
    timeZone: ['', Validators.maxLength(64)],
    currency: ['', Validators.maxLength(3)],
  });

  readonly themeModes = [
    { value: 'SYSTEM', labelKey: 'profile.pref.themeMode.system' },
    { value: 'LIGHT', labelKey: 'profile.pref.themeMode.light' },
    { value: 'DARK', labelKey: 'profile.pref.themeMode.dark' },
  ];

  readonly displayName = computed(() => {
    const p = this.profile();
    return p?.displayName || [p?.firstName, p?.lastName].filter(Boolean).join(' ') || p?.username || p?.email || '—';
  });

  readonly isSuperAdmin = computed(() => this.profile()?.roles?.includes('SUPER_ADMIN') ?? false);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.loadError.set(null);
    this.api.getMe().subscribe({
      next: data => {
        this.profile.set(data);
        this.loading.set(false);
        this.prefillForms(data);
      },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string } })?.error;
        this.loadError.set(pd?.title ?? this.translate.instant('profile.error.load'));
        this.loading.set(false);
      },
    });
  }

  openIdentityForm(): void {
    this.showIdentityForm.set(true);
    this.identityError.set(null);
  }

  cancelIdentityForm(): void {
    this.showIdentityForm.set(false);
    this.identityError.set(null);
    const p = this.profile();
    if (p) this.prefillForms(p);
  }

  submitIdentity(): void {
    if (this.identityForm.invalid) {
      this.identityForm.markAllAsTouched();
      return;
    }
    this.identityState.set('submitting');
    this.identityError.set(null);
    const v = this.identityForm.value;
    const req: UpdateProfileRequest = {
      firstName: v.firstName || null,
      lastName: v.lastName || null,
      phone: v.phone || null,
    };
    this.api.updateProfile(req).subscribe({
      next: () => {
        this.identityState.set('idle');
        this.showIdentityForm.set(false);
        this.load();
      },
      error: (err: unknown) => {
        const pd = (err as { error?: { detail?: string; title?: string } })?.error;
        this.identityError.set(pd?.detail ?? pd?.title ?? this.translate.instant('profile.error.save'));
        this.identityState.set('error');
      },
    });
  }

  openPrefsForm(): void {
    this.showPrefsForm.set(true);
    this.prefsError.set(null);
  }

  cancelPrefsForm(): void {
    this.showPrefsForm.set(false);
    this.prefsError.set(null);
    const p = this.profile();
    if (p) this.prefillForms(p);
  }

  submitPrefs(): void {
    const p = this.profile();
    if (!p) return;
    this.prefsState.set('submitting');
    this.prefsError.set(null);
    const v = this.prefsForm.value;
    const req: UpdatePreferencesRequest = {
      themeMode: (v.themeMode || null) as 'LIGHT' | 'DARK' | 'SYSTEM' | null,
      locale: v.locale || null,
      timeZone: v.timeZone || null,
      currency: v.currency || null,
    };
    this.api.updatePreferences(this.api.resolveId(p), req).subscribe({
      next: () => {
        this.prefsState.set('idle');
        this.showPrefsForm.set(false);
        this.load();
      },
      error: (err: unknown) => {
        const pd = (err as { error?: { detail?: string; title?: string } })?.error;
        this.prefsError.set(pd?.detail ?? pd?.title ?? this.translate.instant('profile.error.save'));
        this.prefsState.set('error');
      },
    });
  }

  private prefillForms(p: MeProfileResponse): void {
    this.identityForm.patchValue({
      firstName: p.firstName ?? '',
      lastName: p.lastName ?? '',
      phone: '',
    });
    this.prefsForm.patchValue({
      themeMode: (p.preferences?.themeMode ?? '') as 'LIGHT' | 'DARK' | 'SYSTEM' | '',
      locale: p.preferences?.locale ?? '',
      timeZone: p.preferences?.timeZone ?? '',
      currency: p.preferences?.currency ?? '',
    });
  }
}
