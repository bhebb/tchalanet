import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';

import { PlatformOpsApi, RecordManualDrawResultRequest } from '../../../platform-ops-api.service';
import {
  CatalogResultSlotView,
  PlatformCatalogApi,
} from '../../../catalog/data-access/platform-catalog-api.service';
import { haitiLotGameMappings } from '../../../../../../shared/results/haiti-lot-game-mapping';
import { resultSlotLabel } from '../../../../../../shared/results/result-slot-label';

@Component({
  selector: 'tch-manual-result-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
  ],
  template: `
    <h2 mat-dialog-title>Saisir un résultat manuel</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="manual-result-dialog__form">
        <mat-form-field appearance="outline">
          <mat-label>Slot résultat</mat-label>
          <mat-select formControlName="slotKey">
            @for (slot of slots(); track slot.slotKey) {
              <mat-option [value]="slot.slotKey">
                {{ slotLabel(slot) }}
              </mat-option>
            }
          </mat-select>
          @if (form.controls.slotKey.invalid && form.controls.slotKey.touched) {
            <mat-error>Requis.</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Date du tirage (YYYY-MM-DD)</mat-label>
          <input matInput type="date" formControlName="drawDate" [max]="today" />
          @if (form.controls.drawDate.invalid && form.controls.drawDate.touched) {
            <mat-error>{{ form.controls.drawDate.hasError('futureDate') ? 'Pas de résultat futur.' : 'Requis.' }}</mat-error>
          }
        </mat-form-field>
        <div class="manual-result-dialog__lot-map" aria-label="Mapping lots Haiti vers provider">
          @for (mapping of lotMappings(); track mapping.lotKey) {
            <div class="manual-result-dialog__lot-card">
              <img [src]="mapping.imageSrc" [alt]="mapping.imageAlt" />
              <span>
                <strong>{{ mapping.gameLabel }}</strong>
                <em>{{ mapping.label }}</em>
              </span>
            </div>
          }
        </div>
        <mat-form-field appearance="outline">
          <mat-label>Pick3 - lot1</mat-label>
          <input matInput formControlName="lot1" maxlength="3" placeholder="123" />
          @if (form.controls.lot1.invalid && form.controls.lot1.touched) {
            <mat-error>3 chiffres requis.</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Pick4 - lot2</mat-label>
          <input matInput formControlName="lot2" maxlength="2" placeholder="45" />
          @if (form.controls.lot2.invalid && form.controls.lot2.touched) {
            <mat-error>2 chiffres requis.</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Pick4 - lot3</mat-label>
          <input matInput formControlName="lot3" maxlength="2" placeholder="67" />
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
        <mat-form-field appearance="outline">
          <mat-label>Notes</mat-label>
          <textarea matInput formControlName="notes" rows="2"></textarea>
        </mat-form-field>
        <mat-checkbox formControlName="force">Forcer si le résultat existe déjà</mat-checkbox>
      </form>
      @if (error()) {
        <div class="manual-result-dialog__error">{{ error() }}</div>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close [disabled]="submitting()">Annuler</button>
      <button mat-flat-button color="primary" [disabled]="form.invalid || submitting()" (click)="submit()">
        Saisir
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .manual-result-dialog__form { display: flex; flex-direction: column; gap: 0.75rem; width: 100%; }
    .manual-result-dialog__lot-map { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 0.5rem; }
    .manual-result-dialog__lot-card { display: grid; grid-template-columns: 3rem minmax(0, 1fr); gap: 0.5rem; align-items: center; min-height: 3.75rem; padding: 0.5rem; border: 1px solid var(--tch-color-outline-variant); border-radius: var(--tch-radius-sm); background: var(--tch-color-surface-container-low); }
    .manual-result-dialog__lot-card img { max-width: 3rem; max-height: 2.5rem; object-fit: contain; }
    .manual-result-dialog__lot-card span { display: grid; gap: 0.125rem; min-width: 0; }
    .manual-result-dialog__lot-card strong { color: var(--tch-color-on-surface); font-size: var(--tch-font-size-label-md); font-weight: 700; }
    .manual-result-dialog__lot-card em { color: var(--tch-color-on-surface-variant); font-size: var(--tch-font-size-label-sm); font-style: normal; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .manual-result-dialog__error { background: var(--tch-color-error-container); color: var(--tch-color-on-error-container); padding: 0.75rem; border-radius: var(--tch-radius-sm); font-size: 0.875rem; margin-top: 0.5rem; }
    @media (max-width: 720px) { .manual-result-dialog__lot-map { grid-template-columns: 1fr; } }
  `],
})
export class ManualResultDialog implements OnInit {
  private readonly dialogRef = inject(MatDialogRef<ManualResultDialog>);
  private readonly api = inject(PlatformOpsApi);
  private readonly catalogApi = inject(PlatformCatalogApi);
  private readonly snackBar = inject(MatSnackBar);
  private readonly fb = inject(FormBuilder);

  readonly today = todayIsoDate();
  readonly slots = signal<CatalogResultSlotView[]>([]);
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.group({
    slotKey: ['', Validators.required],
    drawDate: [this.today, [Validators.required, notFutureDateValidator]],
    lot1: ['', [Validators.required, Validators.pattern(/^\d{3}$/)]],
    lot2: ['', [Validators.required, Validators.pattern(/^\d{2}$/)]],
    lot3: ['', [Validators.required, Validators.pattern(/^\d{2}$/)]],
    reason: ['', Validators.required],
    notes: [''],
    force: [false],
  });

  ngOnInit(): void {
    this.catalogApi.listResultSlots().subscribe({
      next: slots => {
        const sorted = [...slots].sort((a, b) => a.slotKey.localeCompare(b.slotKey));
        this.slots.set(sorted);
        if (!this.form.controls.slotKey.value && sorted.length === 1) {
          this.form.patchValue({ slotKey: sorted[0].slotKey });
        }
      },
      error: err => {
        this.error.set((err as { error?: { title?: string } })?.error?.title ?? 'Slots indisponibles.');
      },
    });
  }

  slotLabel(slot: CatalogResultSlotView): string {
    return resultSlotLabel(slot);
  }

  lotMappings() {
    const slotKey = this.form.controls.slotKey.value;
    const slot = this.slots().find(item => item.slotKey === slotKey);
    return haitiLotGameMappings(slot ?? { slotKey });
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) return;
    const v = this.form.value;
    const req: RecordManualDrawResultRequest = {
      slotKey: v.slotKey!,
      drawDate: v.drawDate!,
      recordedBy: 'platform-ops',
      lot1: v.lot1!,
      lot2: v.lot2!,
      lot3: v.lot3!,
      reason: v.reason!,
      notes: v.notes || undefined,
      force: v.force ?? false,
    };
    this.submitting.set(true);
    this.error.set(null);
    this.api.manualDrawResult(req).subscribe({
      next: () => {
        this.submitting.set(false);
        this.snackBar.open('Résultat manuel saisi.', 'OK', { duration: 3000 });
        this.dialogRef.close(true);
      },
      error: err => {
        this.submitting.set(false);
        this.error.set((err as { error?: { title?: string } })?.error?.title ?? 'Erreur.');
      },
    });
  }
}

function todayIsoDate(): string {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function notFutureDateValidator(control: AbstractControl): { futureDate: true } | null {
  const value = control.value;
  return typeof value === 'string' && value > todayIsoDate() ? { futureDate: true } : null;
}
