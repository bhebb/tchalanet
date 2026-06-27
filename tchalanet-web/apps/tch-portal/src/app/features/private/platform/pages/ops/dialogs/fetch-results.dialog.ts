import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

import {
  PlatformOpsApi,
  FetchExternalResultsRequest,
  OpsLaunchResponse,
} from '../../../platform-ops-api.service';

@Component({
  selector: 'tch-fetch-results-dialog',
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
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="fetch-results-dialog__form">
        <mat-form-field appearance="outline">
          <mat-label>Date de base (YYYY-MM-DD, optionnel)</mat-label>
          <input matInput formControlName="baseDate" placeholder="2026-06-17" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Jours en arrière</mat-label>
          <input matInput type="number" formControlName="daysBack" min="0" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Slot keys (optionnel, virgule-séparé)</mat-label>
          <input matInput formControlName="slotKeys" placeholder="NY_MID, FL_EVE" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Max slots</mat-label>
          <input matInput type="number" formControlName="maxSlots" min="1" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Raison (requis si force=true)</mat-label>
          <input matInput formControlName="reason" />
          @if (form.controls.reason.invalid && form.controls.reason.touched) {
            <mat-error>Raison requise.</mat-error>
          }
        </mat-form-field>
        <mat-checkbox formControlName="force">Forcer l'écrasement</mat-checkbox>
        <mat-checkbox formControlName="dryRun">Dry-run (simuler sans écrire)</mat-checkbox>
        <mat-checkbox formControlName="includeRaw">Persister le payload brut</mat-checkbox>
      </form>

      @if (result()) {
        <div class="fetch-results-dialog__result">
          <p>{{ result()!.started }}/{{ result()!.requested }} job(s) lancé(s)</p>
          @for (launch of result()!.launches; track launch.execution_id ?? launch.tenant_id) {
            <div>
              {{ launch.tenant_id ?? 'global' }} :
              @if (launch.execution_id) { execution #{{ launch.execution_id }} } @else { {{ launch.error }} }
            </div>
          }
        </div>
      }
      @if (error()) {
        <div class="fetch-results-dialog__error">{{ error() }}</div>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="close()">{{ result() ? 'Fermer' : 'Annuler' }}</button>
      @if (!result()) {
        <button mat-flat-button color="primary" [disabled]="form.invalid || submitting()" (click)="submit()">
          @if (submitting()) { <span class="spin material-symbols-outlined">progress_activity</span> }
          Exécuter
        </button>
      }
    </mat-dialog-actions>
  `,
  styles: [`
    .fetch-results-dialog__form { display: flex; flex-direction: column; gap: 0.75rem; width: 100%; }
    .fetch-results-dialog__result { background: var(--tch-color-success-container); color: var(--tch-color-on-success-container); padding: 0.75rem; border-radius: var(--tch-radius-sm); margin-top: 0.75rem; font-size: 0.875rem; }
    .fetch-results-dialog__error { background: var(--tch-color-error-container); color: var(--tch-color-on-error-container); padding: 0.75rem; border-radius: var(--tch-radius-sm); margin-top: 0.5rem; font-size: 0.875rem; }
    .spin { animation: spin 0.8s linear infinite; display: inline-block; vertical-align: middle; }
    @keyframes spin { to { transform: rotate(360deg); } }
  `],
})
export class FetchResultsDialog {
  protected readonly data = inject<{ title: string; mode: 'fetch'; slotKeys?: string[]; onSuccess: () => void }>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<FetchResultsDialog>);
  private readonly api = inject(PlatformOpsApi);
  private readonly fb = inject(FormBuilder);

  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  readonly result = signal<OpsLaunchResponse | null>(null);

  readonly form = this.fb.group({
    baseDate: [''],
    daysBack: [0],
    slotKeys: [this.data.slotKeys ? this.data.slotKeys.join(', ') : ''],
    maxSlots: [200],
    force: [false],
    reason: [''],
    dryRun: [false],
    includeRaw: [false],
  });

  submit(): void {
    if (this.submitting() || this.result()) return;
    const v = this.form.value;
    if (v.force && !v.reason) {
      this.form.controls.reason.setValidators(Validators.required);
      this.form.controls.reason.updateValueAndValidity();
      this.form.markAllAsTouched();
      return;
    }

    const req: FetchExternalResultsRequest = {
      baseDate: v.baseDate || undefined,
      daysBack: v.daysBack ?? 0,
      slotKeys: v.slotKeys ? v.slotKeys.split(',').map(s => s.trim()).filter(Boolean) : undefined,
      force: v.force ?? false,
      dryRun: v.dryRun ?? false,
      maxSlots: v.maxSlots ?? 200,
      reason: v.reason || undefined,
      includeRaw: v.includeRaw ?? false,
    };

    this.submitting.set(true);
    this.error.set(null);

    const onNext = (res: OpsLaunchResponse) => {
      this.submitting.set(false);
      this.result.set(res);
      this.data.onSuccess();
    };
    const onError = (err: unknown) => {
      this.submitting.set(false);
      const pd = (err as { error?: { title?: string } })?.error;
      this.error.set(pd?.title ?? 'Erreur.');
    };

    this.api.fetchDrawResults(req).subscribe({ next: onNext, error: onError });
  }

  close(): void {
    this.dialogRef.close();
  }
}
