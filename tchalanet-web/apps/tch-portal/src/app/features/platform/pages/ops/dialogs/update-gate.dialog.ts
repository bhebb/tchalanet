import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import {
  PlatformOpsApi,
  GateUpdateRequest,
} from '../../../platform-ops-api.service';

@Component({
  selector: 'tch-update-gate-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ data.enable ? 'Activer' : 'Désactiver' }} gate</h2>
    <mat-dialog-content>
      <p class="update-gate-dialog__job-key"><strong>{{ data.jobKey }}</strong></p>
      <form [formGroup]="form" class="update-gate-dialog__form">
        <mat-form-field appearance="outline">
          <mat-label>Portée</mat-label>
          <mat-select formControlName="scope">
            <mat-option value="GLOBAL">Global</mat-option>
            <mat-option value="TENANT">Tenant</mat-option>
          </mat-select>
        </mat-form-field>
        @if (form.controls.scope.value === 'TENANT') {
          <mat-form-field appearance="outline">
            <mat-label>Tenant ID</mat-label>
            <input matInput formControlName="tenantId" />
            @if (form.controls.tenantId.invalid && form.controls.tenantId.touched) {
              <mat-error>Requis pour portée TENANT.</mat-error>
            }
          </mat-form-field>
        }
        <mat-form-field appearance="outline">
          <mat-label>Raison</mat-label>
          <input matInput formControlName="reason" />
          @if (form.controls.reason.invalid && form.controls.reason.touched) {
            <mat-error>Requis.</mat-error>
          }
        </mat-form-field>
      </form>
      @if (error()) {
        <div class="update-gate-dialog__error">{{ error() }}</div>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close [disabled]="submitting()">Annuler</button>
      <button mat-flat-button [color]="data.enable ? 'primary' : 'warn'"
        [disabled]="form.invalid || submitting()" (click)="submit()">
        @if (submitting()) { <span class="spin material-symbols-outlined">progress_activity</span> }
        Confirmer
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .update-gate-dialog__job-key { background: var(--tch-color-surface-container); padding: 0.5rem 0.75rem; border-radius: var(--tch-radius-sm); margin-bottom: 1rem; font-size: 0.875rem; }
    .update-gate-dialog__form { display: flex; flex-direction: column; gap: 0.75rem; width: 100%; }
    .update-gate-dialog__error { background: var(--tch-color-error-container); color: var(--tch-color-on-error-container); padding: 0.75rem; border-radius: var(--tch-radius-sm); font-size: 0.875rem; margin-top: 0.5rem; }
    .spin { animation: spin 0.8s linear infinite; display: inline-block; vertical-align: middle; }
    @keyframes spin { to { transform: rotate(360deg); } }
  `],
})
export class UpdateGateDialog {
  protected readonly data = inject<{ jobKey: string; enable: boolean }>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<UpdateGateDialog>);
  private readonly api = inject(PlatformOpsApi);
  private readonly fb = inject(FormBuilder);

  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.group({
    scope: ['GLOBAL' as 'GLOBAL' | 'TENANT'],
    tenantId: [''],
    reason: ['', Validators.required],
  });

  submit(): void {
    if (this.form.invalid || this.submitting()) return;
    const v = this.form.value;
    if (v.scope === 'TENANT' && !v.tenantId) {
      this.form.controls.tenantId.setValidators(Validators.required);
      this.form.controls.tenantId.updateValueAndValidity();
      this.form.markAllAsTouched();
      return;
    }
    const req: GateUpdateRequest = {
      scope: v.scope!,
      tenant_id: v.scope === 'TENANT' ? v.tenantId : null,
      enabled: this.data.enable,
      reason: v.reason!,
    };
    this.submitting.set(true);
    this.error.set(null);
    this.api.updateGate(this.data.jobKey, req).subscribe({
      next: () => { this.submitting.set(false); this.dialogRef.close(true); },
      error: (err: unknown) => {
        this.submitting.set(false);
        const pd = (err as { error?: { title?: string } })?.error;
        this.error.set(pd?.title ?? 'Erreur lors de la mise à jour.');
      },
    });
  }
}
