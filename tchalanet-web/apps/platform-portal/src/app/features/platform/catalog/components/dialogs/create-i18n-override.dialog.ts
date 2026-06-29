import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import {
  I18nOverrideLevel,
  I18nOverrideView,
  I18nSurface,
  PlatformI18nApi,
  CreateI18nOverrideRequest,
} from '../../data-access/platform-i18n-api.service';

export const COMMON_LOCALES = ['fr', 'en', 'ht', 'es'];
export const LEVELS: I18nOverrideLevel[] = ['GLOBAL', 'TENANT'];
export const SURFACES: I18nSurface[] = [
  'PUBLIC_HOME', 'PUBLIC_RESULTS', 'PUBLIC_TICKET_CHECK', 'COMMON_PUBLIC_ERROR',
  'AUTH', 'CASHIER', 'TENANT_ADMIN', 'PLATFORM_ADMIN', 'COMMON_PRIVATE_ERROR', 'INTERNAL',
];

@Component({
  selector: 'tch-create-i18n-override-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatIconModule, MatInputModule, MatSelectModule, TranslatePipe],
  template: `
    <h2 mat-dialog-title>{{ 'platform.i18nOverrides.dialog.createTitle' | translate }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="form">
        <div class="row">
          <mat-form-field appearance="outline">
            <mat-label>{{ 'platform.i18nOverrides.filter.locale' | translate }}</mat-label>
            <mat-select formControlName="locale">
              @for (loc of locales; track loc) { <mat-option [value]="loc">{{ loc }}</mat-option> }
            </mat-select>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>{{ 'platform.i18nOverrides.filter.level' | translate }}</mat-label>
            <mat-select formControlName="level">
              @for (l of levels; track l) { <mat-option [value]="l">{{ l }}</mat-option> }
            </mat-select>
          </mat-form-field>
        </div>
        <mat-form-field appearance="outline">
          <mat-label>{{ 'platform.i18nOverrides.column.surface' | translate }}</mat-label>
          <mat-select formControlName="surface">
            <mat-option value="">{{ 'platform.i18nOverrides.dialog.defaultSurface' | translate }}</mat-option>
            @for (s of surfaces; track s) { <mat-option [value]="s">{{ s }}</mat-option> }
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>{{ 'platform.i18nOverrides.dialog.keyLabel' | translate }}</mat-label>
          <input matInput formControlName="i18nKey" [placeholder]="'platform.i18nOverrides.dialog.keyPlaceholder' | translate" />
          @if (form.controls.i18nKey.invalid && form.controls.i18nKey.touched) { <mat-error>{{ 'platform.i18nOverrides.dialog.required' | translate }}</mat-error> }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>{{ 'platform.i18nOverrides.dialog.valueLabel' | translate }}</mat-label>
          <textarea matInput formControlName="i18nValue" rows="4"></textarea>
          @if (form.controls.i18nValue.invalid && form.controls.i18nValue.touched) { <mat-error>{{ 'platform.i18nOverrides.dialog.required' | translate }}</mat-error> }
        </mat-form-field>
        @if (form.controls.level.value === 'TENANT') {
          <mat-form-field appearance="outline">
            <mat-label>{{ 'platform.i18nOverrides.dialog.tenantIdLabel' | translate }}</mat-label>
            <input matInput formControlName="tenantId" />
          </mat-form-field>
        }
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="null" [disabled]="submitting()">{{ 'platform.i18nOverrides.action.cancel' | translate }}</button>
      <button mat-flat-button color="primary" [disabled]="form.invalid || submitting()" (click)="submit()">
        @if (submitting()) { <span class="spin material-symbols-outlined">progress_activity</span> }
        {{ 'platform.i18nOverrides.action.create' | translate }}
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
export class CreateI18nOverrideDialog {
  private readonly dialogRef = inject(MatDialogRef<CreateI18nOverrideDialog>);
  private readonly api = inject(PlatformI18nApi);
  private readonly fb = inject(FormBuilder);
  private readonly translate = inject(TranslateService);

  readonly locales = COMMON_LOCALES;
  readonly levels = LEVELS;
  readonly surfaces = SURFACES;
  readonly submitting = signal(false);

  readonly form = this.fb.group({
    locale: ['fr', Validators.required],
    level: ['GLOBAL' as I18nOverrideLevel, Validators.required],
    surface: ['' as I18nSurface | ''],
    i18nKey: ['', Validators.required],
    i18nValue: ['', Validators.required],
    tenantId: [''],
  });

  submit(): void {
    if (this.form.invalid || this.submitting()) return;
    const v = this.form.value;
    const req: CreateI18nOverrideRequest = {
      locale: v.locale!,
      level: v.level!,
      surface: (v.surface || undefined) as I18nSurface | undefined,
      i18nKey: v.i18nKey!,
      i18nValue: v.i18nValue!,
      tenantId: v.tenantId || undefined,
    };
    this.submitting.set(true);
    this.api.createOverride(req).subscribe({
      next: (created: I18nOverrideView) => { this.submitting.set(false); this.dialogRef.close(created); },
      error: (err: unknown) => { this.dialogRef.close({ __error: (err as { error?: { title?: string } })?.error?.title ?? this.translate.instant('platform.i18nOverrides.feedback.genericError') }); },
    });
  }
}
