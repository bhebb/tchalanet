import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import { CreateThemeRequest, PlatformCatalogApi } from '../../data-access/platform-catalog-api.service';

@Component({
  selector: 'tch-create-theme-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatCheckboxModule],
  template: `
    <h2 mat-dialog-title>Nouveau thème</h2>
    <mat-dialog-content>
      <form [formGroup]="form" style="display:flex;flex-direction:column;gap:12px;padding-top:8px">
        <mat-form-field appearance="outline">
          <mat-label>Code</mat-label>
          <input matInput formControlName="code" placeholder="ex: DEFAULT_DARK" />
          @if (form.controls.code.invalid && form.controls.code.touched) { <mat-error>Requis.</mat-error> }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Vendeur (vendor)</mat-label>
          <input matInput formControlName="vendor" placeholder="ex: tchalanet" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Clé de libellé (labelKey)</mat-label>
          <input matInput formControlName="labelKey" placeholder="ex: theme.default_dark" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Configuration JSON</mat-label>
          <textarea matInput formControlName="config" rows="4" placeholder='{"primaryColor":"#000"}'></textarea>
        </mat-form-field>
        <mat-checkbox formControlName="active">Actif</mat-checkbox>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button mat-dialog-close>Annuler</button>
      <button mat-flat-button color="primary" [disabled]="form.invalid || saving()" (click)="save()">Créer</button>
    </mat-dialog-actions>
  `,
})
export class CreateThemeDialog {
  private readonly api = inject(PlatformCatalogApi);
  private readonly ref = inject(MatDialogRef<CreateThemeDialog>);
  private readonly fb = inject(FormBuilder);

  readonly saving = signal(false);
  readonly form = this.fb.nonNullable.group({
    code: ['', Validators.required],
    vendor: [''],
    labelKey: [''],
    config: ['{}'],
    active: [true],
  });

  save(): void {
    if (this.form.invalid) return;
    this.saving.set(true);
    const v = this.form.getRawValue();
    const req: CreateThemeRequest = {
      code: v.code.toUpperCase(),
      vendor: v.vendor || null,
      labelKey: v.labelKey || null,
      config: v.config || null,
      active: v.active,
    };
    this.api.createTheme(req).subscribe({
      next: created => this.ref.close(created),
      error: (err: unknown) => { this.ref.close({ __error: (err as { error?: { title?: string } })?.error?.title ?? 'Erreur.' }); },
    });
  }
}
