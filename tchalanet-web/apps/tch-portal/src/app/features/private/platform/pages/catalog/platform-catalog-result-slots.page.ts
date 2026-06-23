import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';

import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { AdminCrudShellComponent } from '../../../shared/admin-ui/admin-crud-shell.component';
import { AdminDataToolbarComponent } from '../../../shared/admin-ui/admin-data-toolbar.component';
import { AdminEmptyStateComponent } from '../../../shared/admin-ui/admin-empty-state.component';
import { AdminPageShellComponent } from '../../../shared/admin-ui/admin-page-shell.component';
import { AdminStatusPillComponent } from '../../../shared/admin-ui/admin-status-pill.component';
import { PlatformCatalogApi, CatalogResultSlotView, CreateResultSlotRequest } from '../../platform-catalog-api.service';

// ── Create Dialog ─────────────────────────────────────────────────────────────
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
      error: () => { this.saving.set(false); },
    });
  }
}

// ── Main Page ─────────────────────────────────────────────────────────────────
@Component({
  selector: 'tch-platform-catalog-result-slots-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminCrudShellComponent,
    AdminDataToolbarComponent,
    AdminEmptyStateComponent,
    AdminPageShellComponent,
    AdminStatusPillComponent,
    TchErrorPanel,
    TchLoading,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
  ],
  templateUrl: './platform-catalog-result-slots.page.html',
})
export class PlatformCatalogResultSlotsPage implements OnInit {
  private readonly api = inject(PlatformCatalogApi);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly displayedColumns = ['slotKey', 'provider', 'drawTime', 'daysOfWeek', 'timezone', 'active', 'actions'];
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly slots = signal<CatalogResultSlotView[]>([]);

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.listResultSlots().subscribe({
      next: list => { this.slots.set(list); this.loading.set(false); },
      error: (err: unknown) => {
        this.error.set((err as { error?: { title?: string } })?.error?.title ?? 'Erreur de chargement.');
        this.loading.set(false);
      },
    });
  }

  openCreate(): void {
    const ref = this.dialog.open(CreateResultSlotDialog, { width: '480px' });
    ref.afterClosed().subscribe((created: CatalogResultSlotView | null) => {
      if (created) { this.snackBar.open('Slot créé.', 'OK', { duration: 4000 }); this.load(); }
    });
  }

  disable(slot: CatalogResultSlotView): void {
    if (!confirm(`Désactiver le slot « ${slot.slotKey} » ?`)) return;
    this.api.disableResultSlot(slot.slotKey).subscribe({
      next: () => { this.snackBar.open('Slot désactivé.', 'OK', { duration: 4000 }); this.load(); },
      error: (err: unknown) => {
        this.snackBar.open((err as { error?: { title?: string } })?.error?.title ?? 'Erreur.', 'OK', { duration: 5000 });
      },
    });
  }

  delete(slot: CatalogResultSlotView): void {
    if (!confirm(`Supprimer le slot « ${slot.slotKey} » ?`)) return;
    this.api.deleteResultSlot(slot.id.value).subscribe({
      next: () => { this.snackBar.open('Slot supprimé.', 'OK', { duration: 4000 }); this.load(); },
      error: (err: unknown) => {
        this.snackBar.open((err as { error?: { title?: string } })?.error?.title ?? 'Erreur.', 'OK', { duration: 5000 });
      },
    });
  }
}
