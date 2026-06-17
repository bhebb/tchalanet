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
  template: `
    <tch-admin-page-shell
      title="Créer un tenant"
      description="Provisionnez un nouveau tenant avec un profil de configuration."
    >
      <div actions>
        <a mat-button routerLink="/app/platform/tenants">
          <span class="material-symbols-outlined">arrow_back</span>
          Retour
        </a>
      </div>

      @if (result()) {
        <div class="result-panel">
          <tch-admin-section-card title="Tenant provisionné" icon="check_circle">
            <div class="result-header">
              <span class="result-code">{{ result()!.tenantCode }}</span>
              <tch-admin-status-pill
                [label]="result()!.readiness.status"
                [tone]="readinessTone()"
              />
            </div>

            @if (result()!.warnings.length > 0) {
              <div class="result-warnings">
                @for (w of result()!.warnings; track w) {
                  <div class="warning-item">
                    <span class="material-symbols-outlined">warning</span>
                    {{ w }}
                  </div>
                }
              </div>
            }

            <h3 class="result-section-title">Domaines provisionnés</h3>
            <dl class="domain-list">
              @for (entry of domainEntries(); track entry.key) {
                <dt>{{ entry.key }}</dt>
                <dd>
                  <tch-admin-status-pill
                    [label]="entry.value"
                    [tone]="domainTone(entry.value)"
                  />
                </dd>
              }
            </dl>

            @if (result()!.nextSteps.length > 0) {
              <h3 class="result-section-title">Prochaines étapes</h3>
              <ul class="next-steps">
                @for (step of result()!.nextSteps; track step) {
                  <li>{{ step }}</li>
                }
              </ul>
            }

            <div class="result-actions">
              <a mat-flat-button color="primary" routerLink="/app/platform/tenants">
                Retour à la liste
              </a>
            </div>
          </tch-admin-section-card>
        </div>
      } @else {
        <div class="create-layout">
          <form [formGroup]="form" (ngSubmit)="submit()" class="form-col">
            <tch-admin-section-card title="Identité" icon="badge">
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Code (slug)</mat-label>
                <input matInput formControlName="code" placeholder="ex: pap-central" />
                @if (form.controls.code.invalid && form.controls.code.touched) {
                  <mat-error>
                    Format requis : lettres minuscules, chiffres, tirets (3–30 caractères).
                  </mat-error>
                }
              </mat-form-field>

              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Nom</mat-label>
                <input matInput formControlName="name" placeholder="Nom du tenant" />
                @if (form.controls.name.invalid && form.controls.name.touched) {
                  <mat-error>Nom requis.</mat-error>
                }
              </mat-form-field>

              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Type</mat-label>
                <mat-select formControlName="type">
                  @for (t of tenantTypes; track t.value) {
                    <mat-option [value]="t.value">{{ t.label }}</mat-option>
                  }
                </mat-select>
                @if (form.controls.type.invalid && form.controls.type.touched) {
                  <mat-error>Type requis.</mat-error>
                }
              </mat-form-field>
            </tch-admin-section-card>

            <tch-admin-section-card title="Paramètres régionaux" icon="language">
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Fuseau horaire</mat-label>
                <mat-select formControlName="timezone">
                  @for (tz of timezones; track tz) {
                    <mat-option [value]="tz">{{ tz }}</mat-option>
                  }
                </mat-select>
                @if (form.controls.timezone.invalid && form.controls.timezone.touched) {
                  <mat-error>Fuseau horaire requis.</mat-error>
                }
              </mat-form-field>

              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Devise</mat-label>
                <mat-select formControlName="currency">
                  @for (c of currencies; track c) {
                    <mat-option [value]="c">{{ c }}</mat-option>
                  }
                </mat-select>
                @if (form.controls.currency.invalid && form.controls.currency.touched) {
                  <mat-error>Devise requise.</mat-error>
                }
              </mat-form-field>
            </tch-admin-section-card>

            <tch-admin-section-card title="Profil de provisionnement" icon="inventory_2">
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Profil</mat-label>
                <mat-select formControlName="profile">
                  @for (p of profiles; track p.value) {
                    <mat-option [value]="p.value">{{ p.label }}</mat-option>
                  }
                </mat-select>
              </mat-form-field>
              @if (selectedProfileDesc()) {
                <p class="profile-desc">{{ selectedProfileDesc() }}</p>
              }
            </tch-admin-section-card>

            <tch-admin-section-card title="Admin initial (optionnel)" icon="person_add">
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Email de l'admin initial</mat-label>
                <input
                  matInput
                  formControlName="initialAdminEmail"
                  type="email"
                  placeholder="admin@example.com"
                />
                @if (form.controls.initialAdminEmail.invalid && form.controls.initialAdminEmail.touched) {
                  <mat-error>Adresse email invalide.</mat-error>
                }
                <mat-hint>
                  Si vide, le tenant sera créé sans administrateur initial.
                </mat-hint>
              </mat-form-field>
            </tch-admin-section-card>

            @if (submitError()) {
              <tch-error-panel [title]="submitError()!" />
            }

            <div class="form-actions">
              <a mat-button routerLink="/app/platform/tenants">Annuler</a>
              <button
                mat-flat-button
                color="primary"
                type="submit"
                [disabled]="form.invalid || submitting()"
              >
                @if (submitting()) {
                  <span class="material-symbols-outlined spin" aria-hidden="true">
                    progress_activity
                  </span>
                }
                Provisionner
              </button>
            </div>
          </form>

          <aside class="preview-col">
            <div class="preview-card">
              <h3 class="preview-title">
                <span class="material-symbols-outlined">preview</span>
                Aperçu du profil
              </h3>

              @if (previewLoading()) {
                <tch-loading label="Calcul du profil..." />
              } @else if (previewError()) {
                <tch-error-panel [title]="previewError()!" />
              } @else if (preview()) {
                <div class="preview-section">
                  <span class="preview-label">Profil</span>
                  <span class="preview-value">{{ preview()!.profile }}</span>
                </div>

                @if (preview()!.includedDomains.length > 0) {
                  <div class="preview-domains">
                    <span class="preview-label">Domaines inclus</span>
                    <div class="chip-list">
                      @for (d of preview()!.includedDomains; track d) {
                        <span class="chip">{{ d }}</span>
                      }
                    </div>
                  </div>
                }

                @if (preview()!.warnings.length > 0) {
                  <div class="preview-warnings">
                    @for (w of preview()!.warnings; track w) {
                      <div class="warning-item">
                        <span class="material-symbols-outlined">warning</span>
                        {{ w }}
                      </div>
                    }
                  </div>
                }

                @if (preview()!.expectedReadinessSections.length > 0) {
                  <div class="preview-section-list">
                    <span class="preview-label">Sections de configuration attendues</span>
                    <ul class="readiness-list">
                      @for (s of preview()!.expectedReadinessSections; track s) {
                        <li>{{ s }}</li>
                      }
                    </ul>
                  </div>
                }
              } @else {
                <p class="preview-empty">
                  Remplissez le formulaire pour voir l'aperçu du provisionnement.
                </p>
              }
            </div>
          </aside>
        </div>
      }
    </tch-admin-page-shell>
  `,
  styles: [
    `
      .create-layout {
        display: grid;
        grid-template-columns: 1fr 340px;
        gap: 2rem;
        align-items: start;
      }

      @media (max-width: 960px) {
        .create-layout {
          grid-template-columns: 1fr;
        }
      }

      .form-col {
        display: flex;
        flex-direction: column;
        gap: 1rem;
      }

      .full-width {
        width: 100%;
      }

      .profile-desc {
        margin: 0.25rem 0 0;
        font-size: 0.8125rem;
        color: var(--tch-color-on-surface-variant);
      }

      .form-actions {
        display: flex;
        justify-content: flex-end;
        gap: 0.75rem;
        margin-top: 0.5rem;
      }

      .preview-card {
        border: 1px solid var(--tch-color-outline-variant);
        border-radius: 0.75rem;
        padding: 1.25rem;
        position: sticky;
        top: 1rem;
        display: flex;
        flex-direction: column;
        gap: 1rem;
      }

      .preview-title {
        margin: 0;
        font-size: 1rem;
        font-weight: 600;
        color: var(--tch-color-on-surface);
        display: flex;
        align-items: center;
        gap: 0.5rem;
      }

      .preview-title .material-symbols-outlined {
        font-size: 1.125rem;
        color: var(--tch-color-primary);
        font-family: 'Material Symbols Outlined';
      }

      .preview-section {
        display: flex;
        flex-direction: column;
        gap: 0.125rem;
      }

      .preview-label {
        font-size: 0.75rem;
        font-weight: 600;
        text-transform: uppercase;
        letter-spacing: 0.05em;
        color: var(--tch-color-on-surface-variant);
      }

      .preview-value {
        font-size: 0.875rem;
        font-weight: 500;
        color: var(--tch-color-on-surface);
      }

      .preview-domains,
      .preview-section-list {
        display: flex;
        flex-direction: column;
        gap: 0.5rem;
      }

      .chip-list {
        display: flex;
        flex-wrap: wrap;
        gap: 0.375rem;
      }

      .chip {
        display: inline-flex;
        align-items: center;
        padding: 0.25rem 0.625rem;
        border-radius: 9999px;
        background: var(--tch-color-secondary-container);
        color: var(--tch-color-on-secondary-container);
        font-size: 0.75rem;
        font-weight: 500;
      }

      .preview-warnings {
        display: flex;
        flex-direction: column;
        gap: 0.375rem;
      }

      .warning-item {
        display: flex;
        align-items: flex-start;
        gap: 0.375rem;
        font-size: 0.8125rem;
        color: var(--tch-color-on-surface-variant);
      }

      .warning-item .material-symbols-outlined {
        font-size: 1rem;
        color: var(--tch-color-secondary);
        flex-shrink: 0;
        font-family: 'Material Symbols Outlined';
      }

      .readiness-list {
        margin: 0;
        padding-left: 1.25rem;
        font-size: 0.8125rem;
        color: var(--tch-color-on-surface);
        display: flex;
        flex-direction: column;
        gap: 0.125rem;
      }

      .preview-empty {
        margin: 0;
        font-size: 0.875rem;
        color: var(--tch-color-on-surface-variant);
      }

      .spin {
        animation: spin 0.8s linear infinite;
        display: inline-block;
        vertical-align: middle;
        font-family: 'Material Symbols Outlined';
      }

      @keyframes spin {
        to {
          transform: rotate(360deg);
        }
      }

      .result-panel {
        display: flex;
        flex-direction: column;
        gap: 1rem;
      }

      .result-header {
        display: flex;
        align-items: center;
        gap: 1rem;
        margin-bottom: 1rem;
      }

      .result-code {
        font-size: 1.25rem;
        font-weight: 700;
        color: var(--tch-color-on-surface);
      }

      .result-section-title {
        margin: 1rem 0 0.5rem;
        font-size: 0.875rem;
        font-weight: 600;
        color: var(--tch-color-on-surface-variant);
        text-transform: uppercase;
        letter-spacing: 0.05em;
      }

      .result-warnings {
        display: flex;
        flex-direction: column;
        gap: 0.375rem;
        margin-bottom: 0.5rem;
      }

      .domain-list {
        display: grid;
        grid-template-columns: auto 1fr;
        gap: 0.375rem 1rem;
        margin: 0;
      }

      dt {
        font-size: 0.8125rem;
        color: var(--tch-color-on-surface-variant);
        align-self: center;
      }

      dd {
        margin: 0;
        align-self: center;
      }

      .next-steps {
        margin: 0;
        padding-left: 1.25rem;
        display: flex;
        flex-direction: column;
        gap: 0.25rem;
        font-size: 0.875rem;
        color: var(--tch-color-on-surface);
      }

      .result-actions {
        display: flex;
        justify-content: flex-end;
        margin-top: 1.5rem;
      }
    `,
  ],
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
