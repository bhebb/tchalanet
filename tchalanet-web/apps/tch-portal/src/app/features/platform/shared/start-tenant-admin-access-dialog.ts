import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Router } from '@angular/router';

import { TenantAdminAccessStore } from '../../../core/tenant-admin-access/tenant-admin-access.store';
import { PlatformTenantAdminAccessApi } from '../platform-tenant-admin-access-api.service';
import type { TenantStatus } from '../platform-tenants-api.service';

export interface StartTenantAdminAccessDialogData {
  tenantId: string;
  tenantName: string;
  tenantCode: string;
  tenantStatus: TenantStatus;
}

@Component({
  selector: 'tch-start-tenant-admin-access-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    ReactiveFormsModule,
  ],
  template: `
    <h2 mat-dialog-title>Accéder au tenant {{ data.tenantName }}</h2>
    <mat-dialog-content>
      <div class="mode-badge" [class.readonly]="mode === 'SUPPORT_READONLY'">
        <span class="material-symbols-outlined" aria-hidden="true">
          {{ mode === 'SUPPORT_READONLY' ? 'visibility' : 'admin_panel_settings' }}
        </span>
        @if (mode === 'SUPPORT_OVERRIDE') {
          Mode <strong>SUPPORT_OVERRIDE</strong> — le tenant est ACTIVE. Vous pourrez effectuer des
          actions.
        } @else {
          Mode <strong>SUPPORT_READONLY</strong> — le tenant est {{ data.tenantStatus }}. Consultation
          uniquement.
        }
      </div>

      <form [formGroup]="form" class="dialog-form">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Raison d'accès</mat-label>
          <textarea
            matInput
            formControlName="reason"
            rows="3"
            placeholder="Décrivez la raison de cet accès (min. 10 caractères)"
          ></textarea>
          @if (form.controls.reason.invalid && form.controls.reason.touched) {
            <mat-error>La raison est obligatoire (min. 10 caractères).</mat-error>
          }
        </mat-form-field>

        <mat-checkbox formControlName="confirmed">
          Je confirme vouloir accéder au tenant <strong>{{ data.tenantCode }}</strong> en mode
          {{ mode }}.
        </mat-checkbox>
      </form>

      @if (error()) {
        <div class="error-panel">
          <span class="material-symbols-outlined" aria-hidden="true">error</span>
          {{ error() }}
          @if (traceId()) {
            <span class="trace-id">ID: {{ traceId() }}</span>
          }
        </div>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close [disabled]="loading()">Annuler</button>
      <button
        mat-flat-button
        color="warn"
        [disabled]="form.invalid || loading()"
        (click)="submit()"
      >
        @if (loading()) {
          <span class="material-symbols-outlined spin" aria-hidden="true">progress_activity</span>
        }
        Accéder
      </button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      .mode-badge {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        padding: 0.625rem 0.875rem;
        border-radius: 0.5rem;
        background: var(--tch-color-warning-container, #fff3cd);
        color: var(--tch-color-on-warning-container, #92400e);
        margin-bottom: 1rem;
        font-size: 0.875rem;
      }

      .mode-badge.readonly {
        background: var(--tch-color-secondary-container, #e8def8);
        color: var(--tch-color-on-secondary-container, #21005d);
      }

      .dialog-form {
        display: flex;
        flex-direction: column;
        gap: 1rem;
      }

      .full-width {
        width: 100%;
      }

      .error-panel {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        padding: 0.625rem 0.875rem;
        border-radius: 0.5rem;
        background: var(--tch-color-error-container, #ffdad6);
        color: var(--tch-color-on-error-container, #410002);
        margin-top: 0.75rem;
        font-size: 0.875rem;
      }

      .trace-id {
        font-size: 0.75rem;
        opacity: 0.7;
        margin-left: 0.25rem;
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
export class StartTenantAdminAccessDialog {
  protected readonly data = inject<StartTenantAdminAccessDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<StartTenantAdminAccessDialog>);
  private readonly api = inject(PlatformTenantAdminAccessApi);
  private readonly store = inject(TenantAdminAccessStore);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  protected readonly mode =
    this.data.tenantStatus === 'ACTIVE' ? 'SUPPORT_OVERRIDE' : 'SUPPORT_READONLY';

  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly traceId = signal<string | null>(null);

  readonly form = this.fb.group({
    reason: ['', [Validators.required, Validators.minLength(10)]],
    confirmed: [false, [Validators.requiredTrue]],
  });

  submit(): void {
    if (this.form.invalid || this.loading()) return;
    this.loading.set(true);
    this.error.set(null);
    this.traceId.set(null);

    this.api
      .startAdminAccess(this.data.tenantId, {
        reason: this.form.controls.reason.value!,
        mode: this.mode,
      })
      .subscribe({
        next: session => {
          this.store.startSession(session);
          this.loading.set(false);
          this.dialogRef.close(session);
          void this.router.navigate(['/app/admin']);
        },
        error: (err: unknown) => {
          this.loading.set(false);
          const pd = (err as { error?: { title?: string; errorId?: string; requestId?: string } })
            ?.error;
          this.error.set(pd?.title ?? 'Une erreur est survenue.');
          this.traceId.set(pd?.errorId ?? pd?.requestId ?? null);
        },
      });
  }
}
