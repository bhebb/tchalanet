import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import {
  I18nOverrideView,
  I18nSurface,
  PlatformI18nApi,
  UpdateI18nOverrideRequest,
} from '../../../platform-i18n-api.service';
import { SURFACES } from './create-i18n-override.dialog';

@Component({
  selector: 'tch-edit-i18n-override-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatIconModule, MatInputModule, MatSelectModule],
  template: `
    <h2 mat-dialog-title>Modifier la traduction</h2>
    <mat-dialog-content>
      <div class="meta">
        <span class="label">Locale</span><span>{{ data.locale }}</span>
        <span class="label">Clé</span><span class="mono">{{ data.i18nKey }}</span>
        <span class="label">Niveau</span><span>{{ data.level }}</span>
        <span class="label">Surface</span><span>{{ data.surface }}</span>
      </div>
      <form [formGroup]="form" class="form">
        <mat-form-field appearance="outline">
          <mat-label>Valeur</mat-label>
          <textarea matInput formControlName="i18nValue" rows="4"></textarea>
          @if (form.controls.i18nValue.invalid && form.controls.i18nValue.touched) { <mat-error>Requis.</mat-error> }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Surface</mat-label>
          <mat-select formControlName="surface">
            <mat-option value="">— inchangé —</mat-option>
            @for (s of surfaces; track s) { <mat-option [value]="s">{{ s }}</mat-option> }
          </mat-select>
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="null" [disabled]="submitting()">Annuler</button>
      <button mat-flat-button color="primary" [disabled]="form.invalid || submitting()" (click)="submit()">
        @if (submitting()) { <span class="spin material-symbols-outlined">progress_activity</span> }
        Enregistrer
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .meta { display: grid; grid-template-columns: auto 1fr; gap: 0.25rem 0.75rem; margin-bottom: 1rem; font-size: 0.875rem; }
    .label { color: var(--tch-color-on-surface-variant); font-weight: 500; }
    .mono { font-family: monospace; }
    .form { display: flex; flex-direction: column; gap: 0.75rem; width: 100%; }
    .error-banner { background: var(--tch-color-error-container); color: var(--tch-color-on-error-container); padding: 0.75rem; border-radius: var(--tch-radius-sm, 0.5rem); font-size: 0.875rem; margin-top: 0.5rem; }
    .spin { animation: spin 0.8s linear infinite; display: inline-block; vertical-align: middle; }
    @keyframes spin { to { transform: rotate(360deg); } }
  `],
})
export class EditI18nOverrideDialog {
  protected readonly data = inject<I18nOverrideView>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<EditI18nOverrideDialog>);
  private readonly api = inject(PlatformI18nApi);
  private readonly fb = inject(FormBuilder);

  readonly surfaces = SURFACES;
  readonly submitting = signal(false);

  readonly form = this.fb.group({
    i18nValue: [this.data.i18nValue, Validators.required],
    surface: [this.data.surface as I18nSurface | ''],
  });

  submit(): void {
    if (this.form.invalid || this.submitting()) return;
    const v = this.form.value;
    const req: UpdateI18nOverrideRequest = {
      i18nValue: v.i18nValue!,
      surface: (v.surface || undefined) as I18nSurface | undefined,
    };
    this.submitting.set(true);
    this.api.updateOverride(this.data.id, req).subscribe({
      next: (updated: I18nOverrideView) => { this.submitting.set(false); this.dialogRef.close(updated); },
      error: (err: unknown) => { this.dialogRef.close({ __error: (err as { error?: { title?: string } })?.error?.title ?? 'Erreur.' }); },
    });
  }
}
