import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

import {
  PlatformOpsApi,
  ApplyExternalResultsRequest,
} from '../../../platform-ops-api.service';

@Component({
  selector: 'tch-apply-results-dialog',
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
    <h2 mat-dialog-title>Appliquer les résultats</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="apply-results-dialog__form">
        <mat-form-field appearance="outline">
          <mat-label>Slot keys (optionnel, virgule-séparé)</mat-label>
          <input matInput formControlName="slotKeys" placeholder="NY_MID, FL_EVE" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Date de base (YYYY-MM-DD, optionnel)</mat-label>
          <input matInput formControlName="baseDate" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Jours en arrière</mat-label>
          <input matInput type="number" formControlName="daysBack" min="0" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Max slots</mat-label>
          <input matInput type="number" formControlName="maxSlots" min="1" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Raison (optionnel)</mat-label>
          <input matInput formControlName="reason" />
        </mat-form-field>
        <mat-checkbox formControlName="dryRun">Dry-run</mat-checkbox>
        <mat-checkbox formControlName="force">Forcer l'écrasement</mat-checkbox>
      </form>
      @if (result()) {
        <div class="apply-results-dialog__result">
          Insérés: {{ result()!.inserted }} · Mis à jour: {{ result()!.updated }} · Non trouvés: {{ result()!.notFound }} · Erreurs: {{ result()!.errors }}
        </div>
      }
      @if (error()) {
        <div class="apply-results-dialog__error">{{ error() }}</div>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="dialogRef.close()">{{ result() ? 'Fermer' : 'Annuler' }}</button>
      @if (!result()) {
        <button mat-flat-button color="primary" [disabled]="submitting()" (click)="submit()">
          @if (submitting()) { <span class="spin material-symbols-outlined">progress_activity</span> }
          Appliquer
        </button>
      }
    </mat-dialog-actions>
  `,
  styles: [`
    .apply-results-dialog__form { display: flex; flex-direction: column; gap: 0.75rem; width: 100%; }
    .apply-results-dialog__result { background: var(--tch-color-success-container); color: var(--tch-color-on-success-container); padding: 0.75rem; border-radius: var(--tch-radius-sm); font-size: 0.875rem; margin-top: 0.75rem; }
    .apply-results-dialog__error { background: var(--tch-color-error-container); color: var(--tch-color-on-error-container); padding: 0.75rem; border-radius: var(--tch-radius-sm); font-size: 0.875rem; margin-top: 0.5rem; }
    .spin { animation: spin 0.8s linear infinite; display: inline-block; vertical-align: middle; }
    @keyframes spin { to { transform: rotate(360deg); } }
  `],
})
export class ApplyResultsDialog {
  readonly dialogRef = inject(MatDialogRef<ApplyResultsDialog>);
  protected readonly data = inject<{ slotKeys?: string[] } | null>(MAT_DIALOG_DATA, { optional: true });
  private readonly api = inject(PlatformOpsApi);
  private readonly fb = inject(FormBuilder);

  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  readonly result = signal<{ inserted: number; updated: number; notFound: number; errors: number } | null>(null);

  readonly form = this.fb.group({
    slotKeys: [this.data?.slotKeys ? this.data.slotKeys.join(', ') : ''],
    baseDate: [''],
    daysBack: [0],
    maxSlots: [200],
    reason: [''],
    dryRun: [true],
    force: [false],
  });

  submit(): void {
    if (this.submitting() || this.result()) return;
    const v = this.form.value;
    const req: ApplyExternalResultsRequest = {
      baseDate: v.baseDate || undefined,
      daysBack: v.daysBack ?? 0,
      slotKeys: v.slotKeys ? v.slotKeys.split(',').map((s: string) => s.trim()).filter(Boolean) : undefined,
      maxSlots: v.maxSlots ?? 200,
      dryRun: v.dryRun ?? true,
      force: v.force ?? false,
      reason: v.reason || undefined,
    };
    this.submitting.set(true);
    this.error.set(null);
    this.api.applyDrawResults(req).subscribe({
      next: (res) => { this.submitting.set(false); this.result.set(res); },
      error: (err: unknown) => {
        this.submitting.set(false);
        const pd = (err as { error?: { title?: string } })?.error;
        this.error.set(pd?.title ?? 'Erreur.');
      },
    });
  }
}
