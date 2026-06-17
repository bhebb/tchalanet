import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TranslatePipe } from '@ngx-translate/core';

import { TchErrorPanel } from '@tch/ui/components';
import { AdminPageShellComponent } from '../../../private/shared/admin-ui/admin-page-shell.component';
import { AdminSectionCardComponent } from '../../../private/shared/admin-ui/admin-section-card.component';
import {
  AdminStatusPillComponent,
  AdminStatusTone,
} from '../../../private/shared/admin-ui/admin-status-pill.component';
import { PlatformTenantsApi, TenantType } from '../../platform-tenants-api.service';

const TENANT_TYPES: { value: TenantType; label: string }[] = [
  { value: 'BORLETTE', label: 'Borlette' },
  { value: 'RESEAU', label: 'Réseau' },
  { value: 'AMBULANT', label: 'Ambulant' },
];

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
    AdminStatusPillComponent,
    TchErrorPanel,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
  ],
  template: `
    <tch-admin-page-shell title="Créer un tenant" description="Configurez un nouveau tenant.">
      <div actions>
        <a mat-button routerLink="/app/platform/tenants">
          <span class="material-symbols-outlined">arrow_back</span>
          Retour
        </a>
      </div>

      <div class="create-layout">
        <form [formGroup]="form" (ngSubmit)="submit()" class="form-card">
          <tch-admin-section-card title="Identité">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Code (slug)</mat-label>
              <input matInput formControlName="code" placeholder="ex: pap-central" />
              @if (form.controls.code.invalid && form.controls.code.touched) {
                <mat-error>
                  Code requis. Format: lettres minuscules, chiffres, tirets (3–30 caractères).
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

          <tch-admin-section-card title="Paramètres régionaux">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Fuseau horaire</mat-label>
              <input matInput formControlName="timezone" placeholder="ex: America/Port-au-Prince" />
              @if (form.controls.timezone.invalid && form.controls.timezone.touched) {
                <mat-error>Fuseau horaire requis.</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Devise</mat-label>
              <input matInput formControlName="currency" placeholder="ex: HTG" />
              @if (form.controls.currency.invalid && form.controls.currency.touched) {
                <mat-error>Devise requise.</mat-error>
              }
            </mat-form-field>
          </tch-admin-section-card>

          <tch-admin-section-card title="Adresse (optionnel)">
            <div formGroupName="address" class="address-fields">
              <div class="two-col">
                <mat-form-field appearance="outline">
                  <mat-label>Pays</mat-label>
                  <input matInput formControlName="country" />
                </mat-form-field>
                <mat-form-field appearance="outline">
                  <mat-label>Ville</mat-label>
                  <input matInput formControlName="city" />
                </mat-form-field>
              </div>
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Ligne 1</mat-label>
                <input matInput formControlName="line1" />
              </mat-form-field>
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Ligne 2</mat-label>
                <input matInput formControlName="line2" />
              </mat-form-field>
              <mat-form-field appearance="outline" class="half-width">
                <mat-label>Code postal</mat-label>
                <input matInput formControlName="postalCode" />
              </mat-form-field>
            </div>
          </tch-admin-section-card>

          <tch-admin-section-card title="Thème (optionnel)">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>ID du thème actif</mat-label>
              <input matInput formControlName="activeThemeId" placeholder="ID du thème" />
            </mat-form-field>
          </tch-admin-section-card>

          <tch-admin-section-card title="Activation">
            <mat-checkbox formControlName="activate">
              Activer immédiatement après création
            </mat-checkbox>
          </tch-admin-section-card>

          @if (error()) {
            <tch-error-panel [title]="error()!" />
          }

          <div class="form-actions">
            <a mat-button routerLink="/app/platform/tenants">Annuler</a>
            <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid || loading()">
              @if (loading()) {
                <span class="material-symbols-outlined spin" aria-hidden="true">progress_activity</span>
              }
              Créer le tenant
            </button>
          </div>
        </form>

        <!-- Preview card -->
        <aside class="preview-card">
          <h3 class="preview-title">Aperçu</h3>
          <dl class="preview-list">
            <dt>Code</dt>
            <dd>{{ form.controls.code.value || '—' }}</dd>
            <dt>Nom</dt>
            <dd>{{ form.controls.name.value || '—' }}</dd>
            <dt>Type</dt>
            <dd>{{ form.controls.type.value || '—' }}</dd>
            <dt>Fuseau</dt>
            <dd>{{ form.controls.timezone.value || '—' }}</dd>
            <dt>Devise</dt>
            <dd>{{ form.controls.currency.value || '—' }}</dd>
            <dt>Statut</dt>
            <dd>
              <tch-admin-status-pill [label]="previewStatus()" [tone]="previewTone()" />
            </dd>
          </dl>
        </aside>
      </div>
    </tch-admin-page-shell>
  `,
  styles: [
    `
      .create-layout {
        display: grid;
        grid-template-columns: 1fr 320px;
        gap: 2rem;
        align-items: start;
      }

      @media (max-width: 900px) {
        .create-layout {
          grid-template-columns: 1fr;
        }
      }

      .form-card {
        display: flex;
        flex-direction: column;
        gap: 0;
      }

      .full-width {
        width: 100%;
      }

      .half-width {
        width: 50%;
      }

      .two-col {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 0.75rem;
      }

      .address-fields {
        display: flex;
        flex-direction: column;
        gap: 0.75rem;
      }

      .form-actions {
        display: flex;
        justify-content: flex-end;
        gap: 0.75rem;
        margin-top: 1rem;
      }

      .preview-card {
        border: 1px solid var(--tch-color-outline-variant);
        border-radius: 0.75rem;
        padding: 1.25rem;
        position: sticky;
        top: 1rem;
      }

      .preview-title {
        margin: 0 0 0.5rem;
        font-size: 1rem;
        font-weight: 600;
        color: var(--tch-color-on-surface);
      }

      .preview-list {
        display: grid;
        grid-template-columns: auto 1fr;
        gap: 0.25rem 1rem;
        margin: 0;
      }

      dt {
        font-size: 0.8125rem;
        color: var(--tch-color-on-surface-variant);
      }

      dd {
        margin: 0;
        font-size: 0.875rem;
        font-weight: 500;
      }

      .spin {
        animation: spin 0.8s linear infinite;
        display: inline-block;
        vertical-align: middle;
      }

      @keyframes spin {
        to {
          transform: rotate(360deg);
        }
      }
    `,
  ],
})
export class PlatformTenantCreatePage {
  private readonly api = inject(PlatformTenantsApi);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly fb = inject(FormBuilder);

  readonly tenantTypes = TENANT_TYPES;
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly traceId = signal<string | null>(null);

  readonly form = this.fb.group({
    code: [
      '',
      [Validators.required, Validators.pattern(/^[a-z0-9][a-z0-9-]{2,29}$/)],
    ],
    name: ['', Validators.required],
    type: ['' as TenantType, Validators.required],
    timezone: ['', Validators.required],
    currency: ['', Validators.required],
    activeThemeId: [''],
    activate: [false],
    address: this.fb.group({
      country: [''],
      city: [''],
      line1: [''],
      line2: [''],
      postalCode: [''],
    }),
  });

  readonly previewStatus = computed(() => (this.form.controls.activate.value ? 'ACTIVE' : 'DRAFT'));
  readonly previewTone = computed((): AdminStatusTone => this.form.controls.activate.value ? 'success' : 'neutral');

  submit(): void {
    if (this.form.invalid || this.loading()) return;

    const v = this.form.value;
    const addr = v.address;
    const hasAddr = addr && Object.values(addr).some(val => !!val);

    const req = {
      code: v.code!,
      name: v.name!,
      type: v.type!,
      timezone: v.timezone!,
      currency: v.currency!,
      activeThemeId: v.activeThemeId || null,
      activate: v.activate ?? false,
      address: hasAddr ? addr : null,
    };

    this.loading.set(true);
    this.error.set(null);

    this.api.createTenant(req).subscribe({
      next: () => {
        this.loading.set(false);
        this.snackBar.open('Tenant créé avec succès.', 'OK', { duration: 4000 });
        void this.router.navigate(['/app/platform/tenants']);
      },
      error: (err: unknown) => {
        this.loading.set(false);
        const pd = (err as { error?: { title?: string; errorId?: string; requestId?: string } })
          ?.error;
        this.error.set(pd?.title ?? 'Erreur lors de la création.');
        this.traceId.set(pd?.errorId ?? pd?.requestId ?? null);
      },
    });
  }
}
