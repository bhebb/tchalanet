import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import {
  CatalogDrawChannelView,
  CreateDrawChannelRequest,
  PlatformCatalogApi,
} from '../../data-access/platform-catalog-api.service';

@Component({
  selector: 'tch-create-draw-channel-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  template: `
    <h2 mat-dialog-title>Nouveau canal de tirage</h2>
    <mat-dialog-content>
      <form [formGroup]="form" style="display:flex;flex-direction:column;gap:12px;padding-top:8px">
        <mat-form-field appearance="outline">
          <mat-label>Code</mat-label>
          <input matInput formControlName="code" placeholder="ex: TX_MORNING" />
          @if (form.controls.code.invalid && form.controls.code.touched) {
            <mat-error>Requis.</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Nom</mat-label>
          <input matInput formControlName="name" />
          @if (form.controls.name.invalid && form.controls.name.touched) {
            <mat-error>Requis.</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Libellé</mat-label>
          <input matInput formControlName="label" placeholder="ex: Matin" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Heure de tirage (HH:mm)</mat-label>
          <input matInput formControlName="drawTime" placeholder="10:00" />
          @if (form.controls.drawTime.invalid && form.controls.drawTime.touched) {
            <mat-error>Format HH:mm requis.</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Timezone</mat-label>
          <input matInput formControlName="timezone" placeholder="America/Port-au-Prince" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Jours (ex: MONDAY,TUESDAY)</mat-label>
          <input matInput formControlName="daysOfWeek" placeholder="MONDAY,WEDNESDAY,FRIDAY" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Délai cutoff (secondes)</mat-label>
          <input matInput type="number" formControlName="cutoffSec" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Ordre d'affichage</mat-label>
          <input matInput type="number" formControlName="sortOrder" />
        </mat-form-field>
        <mat-checkbox formControlName="active">Actif</mat-checkbox>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button mat-dialog-close>Annuler</button>
      <button
        mat-flat-button
        color="primary"
        [disabled]="form.invalid || saving()"
        (click)="save()"
      >
        Créer
      </button>
    </mat-dialog-actions>
  `,
})
export class CreateDrawChannelDialog {
  private readonly api = inject(PlatformCatalogApi);
  private readonly ref = inject(MatDialogRef<CreateDrawChannelDialog>);
  private readonly fb = inject(FormBuilder);

  readonly saving = signal(false);
  readonly form = this.fb.nonNullable.group({
    code: ['', Validators.required],
    name: ['', Validators.required],
    label: [''],
    drawTime: ['', [Validators.required, Validators.pattern(/^\d{2}:\d{2}$/)]],
    timezone: ['America/Port-au-Prince'],
    daysOfWeek: [''],
    cutoffSec: [null as number | null],
    sortOrder: [10],
    active: [true],
  });

  save(): void {
    if (this.form.invalid) return;
    this.saving.set(true);
    const v = this.form.getRawValue();
    const days = v.daysOfWeek
      ? v.daysOfWeek
          .split(',')
          .map(d => d.trim().toUpperCase())
          .filter(Boolean)
      : null;
    const req: CreateDrawChannelRequest = {
      code: v.code.toUpperCase(),
      name: v.name,
      label: v.label || null,
      drawTime: v.drawTime,
      timezone: v.timezone || null,
      daysOfWeek: days?.length ? days : null,
      cutoffSec: v.cutoffSec ?? null,
      sortOrder: v.sortOrder,
      active: v.active,
    };
    this.api.createDrawChannel(req).subscribe({
      next: (created: CatalogDrawChannelView) => this.ref.close(created),
      error: (err: unknown) => {
        this.ref.close({ __error: (err as { error?: { title?: string } })?.error?.title ?? 'Erreur.' });
      },
    });
  }
}
