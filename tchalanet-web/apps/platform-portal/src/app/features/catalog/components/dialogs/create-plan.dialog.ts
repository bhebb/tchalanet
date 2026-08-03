import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { CreatePlanRequest, PlatformCatalogApi } from '../../data-access/platform-catalog-api.service';

@Component({
  selector: 'tch-create-plan-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatCheckboxModule, MatSelectModule],
  template: `
    <h2 mat-dialog-title>Nouveau plan</h2>
    <mat-dialog-content>
      <form [formGroup]="form" style="display:flex;flex-direction:column;gap:12px;padding-top:8px">
        <mat-form-field appearance="outline">
          <mat-label>Code</mat-label>
          <input matInput formControlName="code" placeholder="ex: STARTER" />
          @if (form.controls.code.invalid && form.controls.code.touched) { <mat-error>Requis.</mat-error> }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Nom</mat-label>
          <input matInput formControlName="name" />
          @if (form.controls.name.invalid && form.controls.name.touched) { <mat-error>Requis.</mat-error> }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Description</mat-label>
          <textarea matInput formControlName="description" rows="2"></textarea>
        </mat-form-field>
        <div style="display:flex;gap:12px">
          <mat-form-field appearance="outline" style="flex:1">
            <mat-label>Prix</mat-label>
            <input matInput type="number" formControlName="priceAmount" />
          </mat-form-field>
          <mat-form-field appearance="outline" style="flex:1">
            <mat-label>Devise</mat-label>
            <input matInput formControlName="currency" placeholder="HTG" />
          </mat-form-field>
        </div>
        <mat-form-field appearance="outline">
          <mat-label>Période de facturation</mat-label>
          <mat-select formControlName="billingPeriod">
            <mat-option value="">—</mat-option>
            <mat-option value="MONTHLY">Mensuel</mat-option>
            <mat-option value="ANNUAL">Annuel</mat-option>
          </mat-select>
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
export class CreatePlanDialog {
  private readonly api = inject(PlatformCatalogApi);
  private readonly ref = inject(MatDialogRef<CreatePlanDialog>);
  private readonly fb = inject(FormBuilder);

  readonly saving = signal(false);
  readonly form = this.fb.nonNullable.group({
    code: ['', Validators.required],
    name: ['', Validators.required],
    description: [''],
    priceAmount: [0],
    currency: ['HTG'],
    billingPeriod: ['MONTHLY'],
    active: [true],
  });

  save(): void {
    if (this.form.invalid) return;
    this.saving.set(true);
    const v = this.form.getRawValue();
    const req: CreatePlanRequest = {
      code: v.code.toUpperCase(),
      name: v.name,
      description: v.description || null,
      priceAmount: v.priceAmount,
      currency: v.currency || null,
      billingPeriod: v.billingPeriod || null,
      active: v.active,
    };
    this.api.createPlan(req).subscribe({
      next: created => this.ref.close(created),
      error: (err: unknown) => { this.ref.close({ __error: (err as { error?: { title?: string } })?.error?.title ?? 'Erreur.' }); },
    });
  }
}
