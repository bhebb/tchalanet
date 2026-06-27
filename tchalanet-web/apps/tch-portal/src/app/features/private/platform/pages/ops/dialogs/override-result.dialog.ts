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
import { haitiLotGameMappings } from '../../../../../../shared/results/haiti-lot-game-mapping';

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
          <mat-label>Date du tirage (YYYY-MM-DD)</mat-label>
          <input matInput formControlName="drawDate" />
          @if (form.controls.drawDate.invalid && form.controls.drawDate.touched) {
            <mat-error>Requis.</mat-error>
          }
        </mat-form-field>
        <div class="override-result-dialog__lot-map" aria-label="Mapping lots Haiti vers provider">
          @for (mapping of lotMappings(); track mapping.lotKey) {
            <div class="override-result-dialog__lot-card">
              <img [src]="mapping.imageSrc" [alt]="mapping.imageAlt" />
              <span>
                <strong>{{ mapping.label }}</strong>
                <em>{{ mapping.provider }} · {{ mapping.gameLabel }}</em>
              </span>
            </div>
          }
        </div>
        <mat-form-field appearance="outline">
          <mat-label>Lot 1</mat-label>
          <input matInput formControlName="lot1" maxlength="3" />
          @if (form.controls.lot1.invalid && form.controls.lot1.touched) {
            <mat-error>3 chiffres requis.</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Lot 2</mat-label>
          <input matInput formControlName="lot2" maxlength="2" />
          @if (form.controls.lot2.invalid && form.controls.lot2.touched) {
            <mat-error>2 chiffres requis.</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Lot 3</mat-label>
          <input matInput formControlName="lot3" maxlength="2" />
          @if (form.controls.lot3.invalid && form.controls.lot3.touched) {
            <mat-error>2 chiffres requis.</mat-error>
          }
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
    .override-result-dialog__lot-map { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 0.5rem; }
    .override-result-dialog__lot-card { display: grid; grid-template-columns: 3rem minmax(0, 1fr); gap: 0.5rem; align-items: center; min-height: 3.75rem; padding: 0.5rem; border: 1px solid var(--tch-color-outline-variant); border-radius: var(--tch-radius-sm); background: var(--tch-color-surface-container-low); }
    .override-result-dialog__lot-card img { max-width: 3rem; max-height: 2.5rem; object-fit: contain; }
    .override-result-dialog__lot-card span { display: grid; gap: 0.125rem; min-width: 0; }
    .override-result-dialog__lot-card strong { color: var(--tch-color-on-surface); font-size: var(--tch-font-size-label-md); font-weight: 700; }
    .override-result-dialog__lot-card em { color: var(--tch-color-on-surface-variant); font-size: var(--tch-font-size-label-sm); font-style: normal; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .override-result-dialog__error { background: var(--tch-color-error-container); color: var(--tch-color-on-error-container); padding: 0.75rem; border-radius: var(--tch-radius-sm); font-size: 0.875rem; margin-top: 0.5rem; }
    .spin { animation: spin 0.8s linear infinite; display: inline-block; vertical-align: middle; }
    @keyframes spin { to { transform: rotate(360deg); } }
    @media (max-width: 720px) { .override-result-dialog__lot-map { grid-template-columns: 1fr; } }
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
    drawDate: [this.drawDateFromRow(), Validators.required],
    lot1: [this.lotValue('lot1'), [Validators.required, Validators.pattern(/^\d{3}$/)]],
    lot2: [this.lotValue('lot2'), [Validators.required, Validators.pattern(/^\d{2}$/)]],
    lot3: [this.lotValue('lot3'), [Validators.required, Validators.pattern(/^\d{2}$/)]],
    reason: ['', Validators.required],
    force: [false],
  });

  lotMappings() {
    return haitiLotGameMappings({ slotKey: this.data.row.slotKey });
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) return;
    const v = this.form.value;
    const req: OverrideDrawResultRequest = {
      slotKey: this.data.row.slotKey,
      drawDate: v.drawDate!,
      lot1: v.lot1!,
      lot2: v.lot2!,
      lot3: v.lot3!,
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

  private lotValue(key: 'lot1' | 'lot2' | 'lot3'): string {
    const value = this.data.row.haitiResult;
    if (!value || typeof value !== 'object') return '';
    const lot = (value as Record<string, unknown>)[key];
    return typeof lot === 'string' ? lot.trim() : '';
  }

  private drawDateFromRow(): string {
    return this.data.row.occurredAt?.slice(0, 10) ?? '';
  }
}
