import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import {
  PlatformSettingsApi,
  SettingView,
  CreateSettingRequest,
  SettingValueType,
  SettingLevel,
  SettingExposure,
} from '../../../platform-settings-api.service';

export const VALUE_TYPES: SettingValueType[] = ['STRING', 'INT', 'LONG', 'DECIMAL', 'BOOLEAN', 'JSON'];
export const SETTING_LEVELS: SettingLevel[] = ['GLOBAL', 'TENANT', 'OUTLET', 'TERMINAL'];
export const EXPOSURES: SettingExposure[] = ['INTERNAL', 'PUBLIC_RUNTIME', 'TENANT_RUNTIME', 'ADMIN_RUNTIME'];

@Component({
  selector: 'tch-create-setting-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatIconModule, MatInputModule, MatSelectModule],
  template: `
    <h2 mat-dialog-title>Nouveau paramètre</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="form">
        <mat-form-field appearance="outline">
          <mat-label>Namespace</mat-label>
          <input matInput formControlName="namespace" placeholder="ex: pos.behavior" />
          @if (form.controls.namespace.invalid && form.controls.namespace.touched) { <mat-error>Requis.</mat-error> }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Clé</mat-label>
          <input matInput formControlName="settingKey" placeholder="ex: require_open_session" />
          @if (form.controls.settingKey.invalid && form.controls.settingKey.touched) { <mat-error>Requis.</mat-error> }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Valeur</mat-label>
          <textarea matInput formControlName="settingValue" rows="3"></textarea>
          @if (form.controls.settingValue.invalid && form.controls.settingValue.touched) { <mat-error>Requis.</mat-error> }
        </mat-form-field>
        <div class="row">
          <mat-form-field appearance="outline">
            <mat-label>Type</mat-label>
            <mat-select formControlName="valueType">
              @for (t of valueTypes; track t) { <mat-option [value]="t">{{ t }}</mat-option> }
            </mat-select>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Niveau</mat-label>
            <mat-select formControlName="level">
              @for (l of levels; track l) { <mat-option [value]="l">{{ l }}</mat-option> }
            </mat-select>
          </mat-form-field>
        </div>
        <mat-form-field appearance="outline">
          <mat-label>Exposition</mat-label>
          <mat-select formControlName="exposure">
            @for (e of exposures; track e) { <mat-option [value]="e">{{ e }}</mat-option> }
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Tenant ID (optionnel, pour niveau TENANT)</mat-label>
          <input matInput formControlName="tenantId" />
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="null" [disabled]="submitting()">Annuler</button>
      <button mat-flat-button color="primary" [disabled]="form.invalid || submitting()" (click)="submit()">
        @if (submitting()) { <span class="spin material-symbols-outlined">progress_activity</span> }
        Créer
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .form { display: flex; flex-direction: column; gap: 0.75rem; width: 100%; }
    .row { display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem; }
    .error-banner { background: var(--tch-color-error-container); color: var(--tch-color-on-error-container); padding: 0.75rem; border-radius: var(--tch-radius-sm, 0.5rem); font-size: 0.875rem; margin-top: 0.5rem; }
    .spin { animation: spin 0.8s linear infinite; display: inline-block; vertical-align: middle; }
    @keyframes spin { to { transform: rotate(360deg); } }
  `],
})
export class CreateSettingDialog {
  private readonly dialogRef = inject(MatDialogRef<CreateSettingDialog>);
  private readonly api = inject(PlatformSettingsApi);
  private readonly fb = inject(FormBuilder);

  readonly valueTypes = VALUE_TYPES;
  readonly levels = SETTING_LEVELS;
  readonly exposures = EXPOSURES;
  readonly submitting = signal(false);

  readonly form = this.fb.group({
    namespace: ['', Validators.required],
    settingKey: ['', Validators.required],
    settingValue: ['', Validators.required],
    valueType: ['STRING' as SettingValueType, Validators.required],
    level: ['GLOBAL' as SettingLevel, Validators.required],
    exposure: ['INTERNAL' as SettingExposure],
    tenantId: [''],
  });

  submit(): void {
    if (this.form.invalid || this.submitting()) return;
    const v = this.form.value;
    const req: CreateSettingRequest = {
      namespace: v.namespace!,
      settingKey: v.settingKey!,
      settingValue: v.settingValue!,
      valueType: v.valueType!,
      level: v.level!,
      exposure: v.exposure || undefined,
      tenantId: v.tenantId || undefined,
    };
    this.submitting.set(true);
    this.api.createSetting(req).subscribe({
      next: (created: SettingView) => { this.submitting.set(false); this.dialogRef.close(created); },
      error: (err: unknown) => { this.dialogRef.close({ __error: (err as { error?: { title?: string } })?.error?.title ?? 'Erreur.' }); },
    });
  }
}
