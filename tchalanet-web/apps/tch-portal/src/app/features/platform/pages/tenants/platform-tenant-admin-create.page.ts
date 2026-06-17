import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';

import { AdminPageShellComponent } from '../../../private/shared/admin-ui/admin-page-shell.component';
import { PlatformTenantsApi, TenantSummaryView } from '../../platform-tenants-api.service';

@Component({
  selector: 'tch-platform-tenant-admin-create-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    AdminPageShellComponent,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
  ],
  template: `
    <tch-admin-page-shell
      [title]="'Créer un admin tenant'"
      [description]="tenant() ? 'Tenant ciblé : ' + tenant()!.name + ' (' + tenant()!.code + ')' : ''"
    >
      <div actions>
        <a mat-button [routerLink]="['..']">
          <span class="material-symbols-outlined">arrow_back</span>
          Retour
        </a>
      </div>

      @if (loadingTenant()) {
        <div class="loading-state">
          <span class="material-symbols-outlined spin">progress_activity</span>
          Chargement...
        </div>
      } @else {
        <form [formGroup]="form" (ngSubmit)="submit()" class="form-card">
          <section class="form-section">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Email</mat-label>
              <input matInput formControlName="email" type="email" />
              @if (form.controls.email.invalid && form.controls.email.touched) {
                <mat-error>Email valide requis.</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Nom affiché</mat-label>
              <input matInput formControlName="displayName" />
              @if (form.controls.displayName.invalid && form.controls.displayName.touched) {
                <mat-error>Nom affiché requis.</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Téléphone (optionnel)</mat-label>
              <input matInput formControlName="phoneNumber" type="tel" />
            </mat-form-field>

            <div class="role-info">
              <span class="material-symbols-outlined">info</span>
              Rôle assigné : <strong>TENANT_ADMIN</strong>
            </div>

            <mat-checkbox formControlName="sendInvite">
              Envoyer une invitation par email
            </mat-checkbox>
          </section>

          @if (error()) {
            <div class="error-panel">
              <span class="material-symbols-outlined">error</span>
              {{ error() }}
              @if (traceId()) {
                <span class="trace-id">ID: {{ traceId() }}</span>
              }
            </div>
          }

          <div class="form-actions">
            <a mat-button [routerLink]="['..']">Annuler</a>
            <button
              mat-flat-button
              color="primary"
              type="submit"
              [disabled]="form.invalid || loading()"
            >
              @if (loading()) {
                <span class="material-symbols-outlined spin" aria-hidden="true">progress_activity</span>
              }
              Créer l'administrateur
            </button>
          </div>
        </form>
      }
    </tch-admin-page-shell>
  `,
  styles: [
    `
      .loading-state {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        padding: 2rem;
        color: var(--tch-color-on-surface-variant);
      }

      .form-card {
        max-width: 560px;
        display: flex;
        flex-direction: column;
        gap: 1rem;
      }

      .form-section {
        border: 1px solid var(--tch-color-outline-variant);
        border-radius: 0.75rem;
        padding: 1.25rem;
        display: flex;
        flex-direction: column;
        gap: 0.75rem;
      }

      .full-width {
        width: 100%;
      }

      .role-info {
        display: flex;
        align-items: center;
        gap: 0.375rem;
        font-size: 0.875rem;
        color: var(--tch-color-on-surface-variant);
        padding: 0.5rem 0;
      }

      .form-actions {
        display: flex;
        justify-content: flex-end;
        gap: 0.75rem;
      }

      .error-panel {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        padding: 1rem;
        border-radius: 0.5rem;
        background: var(--tch-color-error-container, #ffdad6);
        color: var(--tch-color-on-error-container, #410002);
      }

      .trace-id {
        font-size: 0.75rem;
        opacity: 0.7;
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
export class PlatformTenantAdminCreatePage implements OnInit {
  private readonly api = inject(PlatformTenantsApi);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(false);
  readonly loadingTenant = signal(false);
  readonly error = signal<string | null>(null);
  readonly traceId = signal<string | null>(null);
  readonly tenant = signal<TenantSummaryView | null>(null);

  readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    displayName: ['', Validators.required],
    phoneNumber: [''],
    sendInvite: [true],
  });

  private get tenantId(): string {
    return this.route.snapshot.paramMap.get('tenantId')!;
  }

  ngOnInit(): void {
    this.loadingTenant.set(true);
    this.api.getTenant(this.tenantId).subscribe({
      next: t => {
        this.tenant.set(t);
        this.loadingTenant.set(false);
      },
      error: () => this.loadingTenant.set(false),
    });
  }

  submit(): void {
    if (this.form.invalid || this.loading()) return;

    const v = this.form.value;
    this.loading.set(true);
    this.error.set(null);

    this.api
      .createTenantAdmin(this.tenantId, {
        email: v.email!,
        displayName: v.displayName!,
        phoneNumber: v.phoneNumber || null,
        roleCodes: ['TENANT_ADMIN'],
        sendInvite: v.sendInvite ?? true,
      })
      .subscribe({
        next: () => {
          this.loading.set(false);
          this.snackBar.open('Administrateur créé avec succès.', 'OK', { duration: 4000 });
          void this.router.navigate(['..'], { relativeTo: this.route });
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
