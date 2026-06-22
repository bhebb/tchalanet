import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import {
  PlatformSettingsApi,
  SettingView,
  UpdateSettingRequest,
  SettingExposure,
} from '../../../platform-settings-api.service';
import { EXPOSURES } from './create-setting.dialog';

@Component({
  selector: 'tch-edit-setting-dialog',
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
    <h2 mat-dialog-title>Modifier le paramètre</h2>
    <mat-dialog-content>
      <div class="edit-setting-dialog__meta">
        <span class="edit-setting-dialog__label">Namespace</span><span>{{ data.namespace }}</span>
        <span class="edit-setting-dialog__label">Clé</span><span class="mono">{{ data.settingKey }}</span>
        <span class="edit-setting-dialog__label">Type</span><span>{{ data.valueType }}</span>
        <span class="edit-setting-dialog__label">Niveau</span><span>{{ data.level }}</span>
      </div>
      <form [formGroup]="form" class="edit-setting-dialog__form">
        <mat-form-field appearance="outline">
          <mat-label>Valeur</mat-label>
          <textarea matInput formControlName="settingValue" rows="4"></textarea>
          @if (form.controls.settingValue.invalid && form.controls.settingValue.touched) {
            <mat-error>Requis.</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Exposition</mat-label>
          <mat-select formControlName="exposure">
            @for (e of exposures; track e) { <mat-option [value]="e">{{ e }}</mat-option> }
          </mat-select>
        </mat-form-field>
      </form>
      @if (error()) {
        <div class="edit-setting-dialog__error">{{ error() }}</div>
      }
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
    .edit-setting-dialog__meta { display: grid; grid-template-columns: auto 1fr; gap: 0.25rem 0.75rem; margin-bottom: 1rem; font-size: 0.875rem; }
    .edit-setting-dialog__label { color: var(--tch-color-on-surface-variant); font-weight: 500; }
    .mono { font-family: monospace; }
    .edit-setting-dialog__form { display: flex; flex-direction: column; gap: 0.75rem; width: 100%; }
    .edit-setting-dialog__error { background: var(--tch-color-error-container); color: var(--tch-color-on-error-container); padding: 0.75rem; border-radius: var(--tch-radius-sm, 0.5rem); font-size: 0.875rem; margin-top: 0.5rem; }
    .spin { animation: spin 0.8s linear infinite; display: inline-block; vertical-align: middle; }
    @keyframes spin { to { transform: rotate(360deg); } }
  `],
})
export class EditSettingDialog {
  protected readonly data = inject<SettingView>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<EditSettingDialog>);
  private readonly api = inject(PlatformSettingsApi);
  private readonly fb = inject(FormBuilder);

  readonly exposures = EXPOSURES;
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.group({
    settingValue: [this.data.settingValue, Validators.required],
    exposure: [this.data.exposure as SettingExposure],
  });

  submit(): void {
    if (this.form.invalid || this.submitting()) return;
    const v = this.form.value;
    const req: UpdateSettingRequest = {
      settingValue: v.settingValue!,
      exposure: v.exposure || undefined,
    };
    this.submitting.set(true);
    this.error.set(null);
    this.api.updateSetting(this.data.id.value, req).subscribe({
      next: (updated) => { this.submitting.set(false); this.dialogRef.close(updated); },
      error: (err: unknown) => {
        this.submitting.set(false);
        const pd = (err as { error?: { title?: string } })?.error;
        this.error.set(pd?.title ?? 'Erreur lors de la mise à jour.');
      },
    });
  }
}
