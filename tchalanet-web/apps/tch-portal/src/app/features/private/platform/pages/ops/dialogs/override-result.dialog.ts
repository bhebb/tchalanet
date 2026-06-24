import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';

import {
  PlatformOpsApi,
  DrawResultOpsResponse,
  OverrideDrawResultRequest,
} from '../../../platform-ops-api.service';

@Component({
  selector: 'tch-override-result-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
  ],
  template: `
    <h2 mat-dialog-title>Override — {{ data.row.slotKey }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="override-result-dialog__form">
        <mat-form-field appearance="outline">
          <mat-label>Tenant ID</mat-label>
          <input matInput formControlName="tenantId" />
          @if (form.controls.tenantId.invalid && form.controls.tenantId.touched) {
            <mat-error>Requis.</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Date du tirage (YYYY-MM-DD)</mat-label>
          <input matInput formControlName="drawDate" />
          @if (form.controls.drawDate.invalid && form.controls.drawDate.touched) {
            <mat-error>Requis.</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Pick 3 (ex: 1-2-3)</mat-label>
          <input matInput formControlName="pick3" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Pick 4 (ex: 1-2-3-4)</mat-label>
          <input matInput formControlName="pick4" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Raison</mat-label>
          <textarea matInput formControlName="reason" rows="2"></textarea>
          @if (form.controls.reason.invalid && form.controls.reason.touched) {
            <mat-error>Requis.</mat-error>
          }
        </mat-form-field>
        <mat-checkbox formControlName="force">Forcer (remplace états protégés)</mat-checkbox>
      </form>
      @if (error()) {
        <div class="override-result-dialog__error">{{ error() }}</div>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close [disabled]="submitting()">Annuler</button>
      <button mat-flat-button color="warn" [disabled]="form.invalid || submitting()" (click)="submit()">
        @if (submitting()) { <span class="spin material-symbols-outlined">progress_activity</span> }
        Appliquer l'override
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .override-result-dialog__form { display: flex; flex-direction: column; gap: 0.75rem; width: 100%; }
    .override-result-dialog__error { background: var(--tch-color-error-container); color: var(--tch-color-on-error-container); padding: 0.75rem; border-radius: var(--tch-radius-sm); font-size: 0.875rem; margin-top: 0.5rem; }
    .spin { animation: spin 0.8s linear infinite; display: inline-block; vertical-align: middle; }
    @keyframes spin { to { transform: rotate(360deg); } }
  `],
})
export class OverrideResultDialog {
  protected readonly data = inject<{ row: DrawResultOpsResponse; onSuccess: () => void }>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<OverrideResultDialog>);
  private readonly api = inject(PlatformOpsApi);
  private readonly snackBar = inject(MatSnackBar);
  private readonly fb = inject(FormBuilder);

  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.group({
    tenantId: ['', Validators.required],
    drawDate: ['', Validators.required],
    pick3: [''],
    pick4: [''],
    reason: ['', Validators.required],
    force: [false],
  });

  submit(): void {
    if (this.form.invalid || this.submitting()) return;
    const v = this.form.value;
    const req: OverrideDrawResultRequest = {
      tenantId: v.tenantId!,
      slotKey: this.data.row.slotKey,
      drawDate: v.drawDate!,
      pick3: v.pick3 || undefined,
      pick4: v.pick4 || undefined,
      reason: v.reason!,
      force: v.force ?? false,
    };
    this.submitting.set(true);
    this.error.set(null);
    this.api.overrideDrawResult(req).subscribe({
      next: () => {
        this.submitting.set(false);
        this.snackBar.open('Override appliqué.', 'OK', { duration: 3000 });
        this.data.onSuccess();
        this.dialogRef.close(true);
      },
      error: (err: unknown) => {
        this.submitting.set(false);
        const pd = (err as { error?: { title?: string } })?.error;
        this.error.set(pd?.title ?? 'Erreur.');
      },
    });
  }
}
