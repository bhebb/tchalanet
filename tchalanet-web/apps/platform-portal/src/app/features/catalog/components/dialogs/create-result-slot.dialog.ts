import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import { CreateResultSlotRequest, PlatformCatalogApi } from '../../data-access/platform-catalog-api.service';

@Component({
  selector: 'tch-create-result-slot-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>Nouveau slot de résultat</h2>
    <mat-dialog-content>
      <form [formGroup]="form" style="display:flex;flex-direction:column;gap:12px;padding-top:8px">
        <mat-form-field appearance="outline">
          <mat-label>Clé (slotKey)</mat-label>
          <input matInput formControlName="slotKey" placeholder="ex: TX_MORNING" />
          @if (form.controls.slotKey.invalid && form.controls.slotKey.touched) { <mat-error>Requis.</mat-error> }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Fournisseur</mat-label>
          <input matInput formControlName="provider" placeholder="ex: TX" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Heure de tirage (HH:mm)</mat-label>
          <input matInput formControlName="drawTime" placeholder="10:00" />
          @if (form.controls.drawTime.invalid && form.controls.drawTime.touched) { <mat-error>Requis (format HH:mm).</mat-error> }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Jours (ex: MON,TUE,WED)</mat-label>
          <input matInput formControlName="daysOfWeek" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Timezone</mat-label>
          <input matInput formControlName="timezone" placeholder="America/Port-au-Prince" />
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button mat-dialog-close>Annuler</button>
      <button mat-flat-button color="primary" [disabled]="form.invalid || saving()" (click)="save()">Créer</button>
    </mat-dialog-actions>
  `,
})
export class CreateResultSlotDialog {
  private readonly api = inject(PlatformCatalogApi);
  private readonly ref = inject(MatDialogRef<CreateResultSlotDialog>);
  private readonly fb = inject(FormBuilder);

  readonly saving = signal(false);
  readonly form = this.fb.nonNullable.group({
    slotKey: ['', Validators.required],
    provider: [''],
    drawTime: ['', [Validators.required, Validators.pattern(/^\d{2}:\d{2}$/)]],
    daysOfWeek: [''],
    timezone: [''],
  });

  save(): void {
    if (this.form.invalid) return;
    this.saving.set(true);
    const v = this.form.getRawValue();
    const req: CreateResultSlotRequest = {
      slotKey: v.slotKey.toUpperCase(),
      provider: v.provider || null,
      drawTime: v.drawTime,
      daysOfWeek: v.daysOfWeek || null,
      timezone: v.timezone || null,
    };
    this.api.createResultSlot(req).subscribe({
      next: created => this.ref.close(created),
      error: (err: unknown) => { this.ref.close({ __error: (err as { error?: { title?: string } })?.error?.title ?? 'Erreur.' }); },
    });
  }
}
