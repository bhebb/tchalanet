import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
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
import { PlatformCatalogApi, CatalogResultSlotView, CreateResultSlotRequest, UpdateResultSlotRequest } from '../../platform-catalog-api.service';
import { FetchResultsDialog } from '../ops/dialogs/fetch-results.dialog';
import { ApplyResultsDialog } from '../ops/dialogs/apply-results.dialog';

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
      error: (err: unknown) => { this.ref.close({ __error: (err as { error?: { title?: string } })?.error?.title ?? 'Erreur.' }); },
    });
  }
}

// ── Edit Dialog ───────────────────────────────────────────────────────────────
@Component({
  selector: 'tch-edit-result-slot-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>Modifier — {{ slot().slotKey }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" style="display:flex;flex-direction:column;gap:12px;padding-top:8px">
        <mat-form-field appearance="outline">
          <mat-label>Fournisseur</mat-label>
          <input matInput formControlName="provider" placeholder="ex: TX" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Heure de tirage (HH:mm)</mat-label>
          <input matInput formControlName="drawTime" placeholder="10:00" />
          @if (form.controls.drawTime.invalid && form.controls.drawTime.touched) { <mat-error>Format HH:mm requis.</mat-error> }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Jours (ex: MON,TUE,WED)</mat-label>
          <input matInput formControlName="daysOfWeek" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Timezone</mat-label>
          <input matInput formControlName="timezone" placeholder="America/Port-au-Prince" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Clé de libellé</mat-label>
          <input matInput formControlName="labelKey" />
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button mat-dialog-close>Annuler</button>
      <button mat-flat-button color="primary" [disabled]="form.invalid || saving()" (click)="save()">Enregistrer</button>
    </mat-dialog-actions>
  `,
})
export class EditResultSlotDialog {
  private readonly api = inject(PlatformCatalogApi);
  private readonly ref = inject(MatDialogRef<EditResultSlotDialog>);
  private readonly fb = inject(FormBuilder);

  readonly saving = signal(false);
  readonly slot = signal<CatalogResultSlotView>({} as CatalogResultSlotView);
  readonly form = this.fb.nonNullable.group({
    provider: [''],
    drawTime: ['', [Validators.required, Validators.pattern(/^\d{2}:\d{2}$/)]],
    daysOfWeek: [''],
    timezone: [''],
    labelKey: [''],
  });

  init(s: CatalogResultSlotView): void {
    this.slot.set(s);
    this.form.patchValue({
      provider: s.provider ?? '',
      drawTime: s.drawTime?.substring(0, 5) ?? '',
      daysOfWeek: s.daysOfWeek ?? '',
      timezone: s.timezone ?? '',
      labelKey: s.labelKey ?? '',
    });
  }

  save(): void {
    if (this.form.invalid) return;
    this.saving.set(true);
    const v = this.form.getRawValue();
    const req: UpdateResultSlotRequest = {
      provider: v.provider || null,
      drawTime: v.drawTime,
      daysOfWeek: v.daysOfWeek || null,
      timezone: v.timezone || null,
      labelKey: v.labelKey || null,
    };
    this.api.updateResultSlot(this.slot().id, req).subscribe({
      next: updated => this.ref.close(updated),
      error: (err: unknown) => { this.ref.close({ __error: (err as { error?: { title?: string } })?.error?.title ?? 'Erreur.' }); },
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
    MatCheckboxModule,
    MatIconModule,
    MatTableModule,
  ],
  templateUrl: './platform-catalog-result-slots.page.html',
})
export class PlatformCatalogResultSlotsPage implements OnInit {
  private readonly api = inject(PlatformCatalogApi);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly displayedColumns = ['select', 'slotKey', 'provider', 'drawTime', 'daysOfWeek', 'timezone', 'active', 'actions'];
  readonly q = signal('');
  readonly selectedSlotKeys = signal<string[]>([]);
  readonly filteredSlots = computed(() => {
    const term = this.q().toLowerCase().trim();
    return term ? this.slots().filter(s => s.slotKey.toLowerCase().includes(term)) : this.slots();
  });
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly slots = signal<CatalogResultSlotView[]>([]);

  private showError(msg: string): void {
    this.error.set(msg);
    setTimeout(() => window.scrollTo({ top: 0, behavior: 'smooth' }), 50);
  }

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

  onSearch(term: string): void {
    this.q.set(term);
    this.selectedSlotKeys.set([]);
  }

  isSelected(slotKey: string): boolean {
    return this.selectedSlotKeys().includes(slotKey);
  }

  toggleSlot(slotKey: string): void {
    const current = this.selectedSlotKeys();
    this.selectedSlotKeys.set(
      current.includes(slotKey) ? current.filter(k => k !== slotKey) : [...current, slotKey],
    );
  }

  toggleAll(): void {
    const visible = this.filteredSlots().map(s => s.slotKey);
    const allVisibleSelected = visible.every(k => this.selectedSlotKeys().includes(k));
    this.selectedSlotKeys.set(allVisibleSelected ? [] : visible);
  }

  openFetch(): void {
    const keys = this.selectedSlotKeys();
    this.dialog.open(FetchResultsDialog, {
      data: {
        title: `Fetch résultats — ${keys.length} slot(s)`,
        mode: 'fetch' as const,
        slotKeys: keys,
        onSuccess: () => {},
      },
      width: '560px',
    });
  }

  openApply(): void {
    this.dialog.open(ApplyResultsDialog, {
      data: { slotKeys: this.selectedSlotKeys() },
      width: '480px',
    });
  }

  openCreate(): void {
    const ref = this.dialog.open(CreateResultSlotDialog, { width: '480px' });
    ref.afterClosed().subscribe((created: CatalogResultSlotView | { __error: string } | null) => {
      if (created && '__error' in created) { this.showError(created.__error); return; }
      if (created) { this.snackBar.open('Slot créé.', 'OK', { duration: 4000 }); this.load(); }
    });
  }

  openEdit(slot: CatalogResultSlotView): void {
    const ref = this.dialog.open(EditResultSlotDialog, { width: '480px' });
    (ref.componentInstance as EditResultSlotDialog).init(slot);
    ref.afterClosed().subscribe((updated: CatalogResultSlotView | { __error: string } | null) => {
      if (updated && '__error' in updated) { this.showError(updated.__error); return; }
      if (updated) { this.snackBar.open('Slot mis à jour.', 'OK', { duration: 4000 }); this.load(); }
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
    this.api.deleteResultSlot(slot.id).subscribe({
      next: () => { this.snackBar.open('Slot supprimé.', 'OK', { duration: 4000 }); this.load(); },
      error: (err: unknown) => {
        this.snackBar.open((err as { error?: { title?: string } })?.error?.title ?? 'Erreur.', 'OK', { duration: 5000 });
      },
    });
  }
}
