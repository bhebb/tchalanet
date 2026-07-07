import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialogModule, MatDialogRef, MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';

import { AdminCrudShellComponent } from '@tch/ui/console';
import { AdminDataToolbarComponent } from '@tch/ui/console';
import { AdminEmptyStateComponent } from '@tch/ui/console';
import { AdminPageShellComponent } from '@tch/ui/console';
import { AdminStatusPillComponent } from '@tch/ui/console';
import {
  TchAsyncReadyDirective,
  TchAsyncViewComponent,
  resourceErrorVm,
} from '@tch/web/async';
import { PlatformCatalogApi, CatalogResultSlotView, CreateResultSlotRequest, UpdateResultSlotRequest } from '../../data-access/platform-catalog-api.service';
import { FetchResultsDialog } from '../../../operations/components/dialogs/fetch-results.dialog';
import { ApplyResultsDialog } from '../../../operations/components/dialogs/apply-results.dialog';

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
    TchAsyncReadyDirective,
    TchAsyncViewComponent,
    MatButtonModule,
    MatCheckboxModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    ReactiveFormsModule,
    MatTableModule,
  ],
  templateUrl: './platform-catalog-result-slots.page.html',
  styleUrls: ['./platform-catalog-result-slots.page.scss'],
})
export class PlatformCatalogResultSlotsPage {
  private readonly api = inject(PlatformCatalogApi);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly fb = inject(FormBuilder);

  readonly displayedColumns = ['select', 'slotKey', 'provider', 'drawTime', 'daysOfWeek', 'timezone', 'active', 'actions'];
  readonly q = signal('');
  readonly selectedSlotKeys = signal<string[]>([]);
  readonly selectedSlot = signal<CatalogResultSlotView | null>(null);
  readonly savingIdentity = signal(false);
  readonly savingSource = signal(false);
  readonly savingProjection = signal(false);
  readonly slots = this.api.listResultSlotsResource({ suppressShellFeedback: true });
  readonly slotsError = resourceErrorVm(this.slots, 'platform.catalog.resultSlots');
  readonly slotRows = computed(() => this.slots.value() ?? []);
  readonly filteredSlots = computed(() => {
    const term = this.q().toLowerCase().trim();
    const rows = this.slotRows();
    return term ? rows.filter(s => s.slotKey.toLowerCase().includes(term)) : rows;
  });
  readonly hasSlots = computed(() => this.slotRows().length > 0);
  readonly sourceSummary = computed(() => summarizeSourceConfig(this.selectedSlot()?.sourceCfg));
  readonly projectionSummary = computed(() => summarizeProjectionConfig(this.selectedSlot()?.projectionCfg));

  readonly identityForm = this.fb.nonNullable.group({
    provider: ['', Validators.required],
    drawTime: ['', [Validators.required, Validators.pattern(/^\d{2}:\d{2}$/)]],
    daysOfWeek: ['', Validators.required],
    timezone: ['', Validators.required],
    labelKey: [''],
    active: [true],
  });

  readonly sourceForm = this.fb.nonNullable.group({
    sourceJson: ['', Validators.required],
  });

  readonly projectionForm = this.fb.nonNullable.group({
    projectionJson: ['', Validators.required],
  });

  private showError(msg: string): void {
    this.snackBar.open(msg, 'OK', { duration: 5000 });
  }

  load(): void {
    this.slots.reload();
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
      onSuccess: () => this.load(),
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

  openConfig(slot: CatalogResultSlotView): void {
    this.selectSlot(slot);
  }

  selectSlot(slot: CatalogResultSlotView): void {
    this.selectedSlot.set(slot);
    this.identityForm.reset({
      provider: slot.provider ?? '',
      drawTime: slot.drawTime?.substring(0, 5) ?? '',
      daysOfWeek: slot.daysOfWeek ?? '',
      timezone: slot.timezone ?? '',
      labelKey: slot.labelKey ?? '',
      active: slot.active,
    });
    this.sourceForm.reset({
      sourceJson: formatJson(slot.sourceCfg),
    });
    this.projectionForm.reset({
      projectionJson: formatJson(slot.projectionCfg),
    });
  }

  closeConfig(): void {
    this.selectedSlot.set(null);
  }

  saveIdentity(): void {
    const slot = this.selectedSlot();
    if (!slot || this.identityForm.invalid || this.savingIdentity()) return;

    this.savingIdentity.set(true);
    const v = this.identityForm.getRawValue();
    const req: UpdateResultSlotRequest = {
      provider: v.provider || null,
      drawTime: v.drawTime,
      daysOfWeek: v.daysOfWeek || null,
      timezone: v.timezone || null,
      labelKey: v.labelKey || null,
      active: v.active,
    };
    this.api.updateResultSlot(slot.id, req).subscribe({
      next: updated => this.handleSlotUpdated(updated, 'Slot mis à jour.'),
      error: err => this.handleSaveError(err, this.savingIdentity),
    });
  }

  saveSourceConfig(): void {
    const slot = this.selectedSlot();
    if (!slot || this.sourceForm.invalid || this.savingSource()) return;

    const parsed = parseJson(this.sourceForm.controls.sourceJson.value, this.snackBar);
    if (parsed === INVALID_JSON) return;

    this.savingSource.set(true);
    this.api.updateResultSlotSourceConfig(slot.id, { sourceCfg: parsed }).subscribe({
      next: updated => this.handleSlotUpdated(updated, 'Configuration source mise à jour.'),
      error: err => this.handleSaveError(err, this.savingSource),
    });
  }

  saveProjectionConfig(): void {
    const slot = this.selectedSlot();
    if (!slot || this.projectionForm.invalid || this.savingProjection()) return;

    const parsed = parseJson(this.projectionForm.controls.projectionJson.value, this.snackBar);
    if (parsed === INVALID_JSON) return;

    this.savingProjection.set(true);
    this.api.updateResultSlotProjectionConfig(slot.id, { projectionCfg: parsed }).subscribe({
      next: updated => this.handleSlotUpdated(updated, 'Projection mise à jour.'),
      error: err => this.handleSaveError(err, this.savingProjection),
    });
  }

  private handleSlotUpdated(updated: CatalogResultSlotView, message: string): void {
    this.savingIdentity.set(false);
    this.savingSource.set(false);
    this.savingProjection.set(false);
    this.snackBar.open(message, 'OK', { duration: 4000 });
    this.selectSlot(updated);
    this.load();
  }

  private handleSaveError(err: unknown, pending: { set(value: boolean): void }): void {
    pending.set(false);
    this.showError((err as { error?: { title?: string; detail?: string } })?.error?.detail
      ?? (err as { error?: { title?: string } })?.error?.title
      ?? 'Erreur.');
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

const INVALID_JSON = Symbol('INVALID_JSON');

function parseJson(raw: string, snackBar: MatSnackBar): unknown | typeof INVALID_JSON {
  try {
    const parsed = JSON.parse(raw);
    if (parsed === null || Array.isArray(parsed) || typeof parsed !== 'object') {
      snackBar.open('Le JSON doit être un objet.', 'OK', { duration: 5000 });
      return INVALID_JSON;
    }
    return parsed;
  } catch {
    snackBar.open('JSON invalide.', 'OK', { duration: 5000 });
    return INVALID_JSON;
  }
}

function formatJson(value: unknown): string {
  if (value === null || value === undefined) return '{}';
  return JSON.stringify(value, null, 2);
}

function summarizeSourceConfig(value: unknown): string[] {
  const cfg = asRecord(value);
  if (!cfg) return ['Aucune source configurée'];
  const out = [`Provider slot: ${text(cfg['provider_slot_code']) || 'non configuré'}`];
  for (const key of ['pick3', 'pick4']) {
    const game = asRecord(cfg[key]);
    if (!game) {
      out.push(`${key}: absent`);
      continue;
    }
    out.push(`${key}: ${text(game['game_code']) || 'sans code'} · ${game['active'] === false ? 'inactif' : 'actif'}`);
  }
  return out;
}

function summarizeProjectionConfig(value: unknown): string[] {
  const cfg = asRecord(value);
  const rules = asRecord(cfg?.['rules']);
  if (!rules) return ['Aucune projection configurée'];
  return ['lot1', 'lot2', 'lot3', 'lot4'].map(lot => `${lot}: ${text(rules[lot]) || 'non configuré'}`);
}

function asRecord(value: unknown): Record<string, unknown> | null {
  return value && typeof value === 'object' && !Array.isArray(value)
    ? value as Record<string, unknown>
    : null;
}

function text(value: unknown): string {
  return typeof value === 'string' ? value : '';
}
