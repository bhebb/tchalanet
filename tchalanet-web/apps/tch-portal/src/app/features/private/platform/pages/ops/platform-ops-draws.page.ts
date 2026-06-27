import {
  ChangeDetectionStrategy,
  Component,
  computed,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialog, MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';

import { TchErrorPanel, TchLoading, TchSearchOption, TchSearchSelect } from '@tch/ui/components';
import { AdminCrudShellComponent } from '../../../shared/admin-ui/admin-crud-shell.component';
import { AdminDataToolbarComponent } from '../../../shared/admin-ui/admin-data-toolbar.component';
import { AdminEmptyStateComponent } from '../../../shared/admin-ui/admin-empty-state.component';
import { AdminPageShellComponent } from '../../../shared/admin-ui/admin-page-shell.component';
import { AdminStatusPillComponent, AdminStatusTone } from '../../../shared/admin-ui/admin-status-pill.component';
import {
  PlatformOpsApi,
  DrawView,
  CancelDrawRequest,
  CancelDrawsRequest,
  CorrectDrawResultRequest,
  OpenTodayDrawsRequest,
  CloseDueDrawsRequest,
} from '../../platform-ops-api.service';
import { GenerateDrawsDialog } from './dialogs/generate-draws.dialog';
import { BatchOpDialog, AnyBatchResult } from './dialogs/batch-op.dialog';
import { ApplyResultsDialog } from './dialogs/apply-results.dialog';
import { lotteryAssetForSlot } from '../../../../../shared/lottery/lottery-assets';
import { PlatformTenantsApi, TenantSummaryView } from '../../tenants/data-access/platform-tenants-api.service';
import { Observable, map } from 'rxjs';

const DRAW_DIALOG_STYLES = [`
  .draw-dialog__hint {
    margin-top: 0;
    color: var(--tch-color-on-surface-variant);
    font-size: var(--tch-font-size-body-sm);
  }

  .draw-dialog__form {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
    width: 100%;
  }

  .draw-dialog__error {
    margin-top: 0.5rem;
    padding: 0.75rem;
    border-radius: var(--tch-radius-sm);
    background: var(--tch-color-error-container);
    color: var(--tch-color-on-error-container);
    font-size: var(--tch-font-size-body-sm);
  }

  .draw-dialog__spinner {
    display: inline-block;
    vertical-align: middle;
    animation: spin 0.8s linear infinite;
  }

  @keyframes spin {
    to { transform: rotate(360deg); }
  }
`];

// ── CorrectDrawResultDialog ────────────────────────────────────────────────────

@Component({
  selector: 'tch-correct-draw-result-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatCheckboxModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>Corriger le résultat — {{ data.draw.slot.key }} {{ data.draw.drawDate }}</h2>
    <mat-dialog-content>
      <p class="draw-dialog__hint">
        Remplace le résultat appliqué par un nouveau résultat confirmé.
        Résultat actuel : <code>{{ data.draw.lastResult?.id ?? 'aucun' }}</code>
      </p>
      <form [formGroup]="form" class="draw-dialog__form">
        <mat-form-field appearance="outline">
          <mat-label>ID du résultat corrigé (DrawResultId)</mat-label>
          <input matInput formControlName="correctedDrawResultId" placeholder="uuid du nouveau résultat confirmé" />
          @if (form.controls.correctedDrawResultId.invalid && form.controls.correctedDrawResultId.touched) {
            <mat-error>Requis.</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Raison (requise)</mat-label>
          <textarea matInput formControlName="reason" rows="2"></textarea>
          @if (form.controls.reason.invalid && form.controls.reason.touched) {
            <mat-error>Requis.</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Clé d'idempotence</mat-label>
          <input matInput formControlName="idempotencyKey" />
          @if (form.controls.idempotencyKey.invalid && form.controls.idempotencyKey.touched) {
            <mat-error>Requis.</mat-error>
          }
        </mat-form-field>
        <mat-checkbox formControlName="force">Forcer (écraser même si déjà corrigé)</mat-checkbox>
      </form>
      @if (error()) {
        <div class="draw-dialog__error">{{ error() }}</div>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Annuler</button>
      <button mat-flat-button color="warn" [disabled]="form.invalid || submitting()" (click)="submit()">
        @if (submitting()) { <span class="material-symbols-outlined draw-dialog__spinner">progress_activity</span> }
        Corriger
      </button>
    </mat-dialog-actions>
  `,
  styles: DRAW_DIALOG_STYLES,
})
export class CorrectDrawResultDialog {
  protected readonly data = inject<{ draw: DrawView; tenantId: string | null }>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<CorrectDrawResultDialog>);
  private readonly api = inject(PlatformOpsApi);
  private readonly fb = inject(FormBuilder);

  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    correctedDrawResultId: [this.data.draw.lastResult?.id ?? '', Validators.required],
    reason: ['', Validators.required],
    idempotencyKey: [`correct-${this.data.draw.id}-${Date.now()}`, Validators.required],
    force: [false],
  });

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    const req: CorrectDrawResultRequest = {
      correctedDrawResultId: v.correctedDrawResultId,
      reason: v.reason,
      idempotencyKey: v.idempotencyKey,
      force: v.force,
    };
    this.submitting.set(true);
    this.error.set(null);
    this.api.correctDrawResult(this.data.draw.id, req, this.data.tenantId).subscribe({
      next: () => { this.submitting.set(false); this.dialogRef.close(true); },
      error: (err: unknown) => {
        this.submitting.set(false);
        this.error.set((err as { error?: { title?: string } })?.error?.title ?? 'Erreur.');
      },
    });
  }
}

// ── CancelDrawDialog ───────────────────────────────────────────────────────────

@Component({
  selector: 'tch-cancel-draw-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatCheckboxModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>Annuler le tirage — {{ data.draw.channel.code }} {{ data.draw.drawDate }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="draw-dialog__form">
        <mat-form-field appearance="outline">
          <mat-label>Code de raison (ex: ADMIN_REQUEST)</mat-label>
          <input matInput formControlName="reasonCode" placeholder="ADMIN_REQUEST" />
          @if (form.controls.reasonCode.invalid && form.controls.reasonCode.touched) {
            <mat-error>Requis (max 96 caractères).</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Libellé (optionnel)</mat-label>
          <textarea matInput formControlName="reasonLabel" rows="2"></textarea>
        </mat-form-field>
        <mat-checkbox formControlName="force">Forcer (même si tirage ouvert)</mat-checkbox>
      </form>
      @if (error()) {
        <div class="draw-dialog__error">{{ error() }}</div>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Fermer</button>
      <button mat-flat-button color="warn" [disabled]="form.invalid || submitting()" (click)="submit()">
        @if (submitting()) { <span class="material-symbols-outlined draw-dialog__spinner">progress_activity</span> }
        Annuler le tirage
      </button>
    </mat-dialog-actions>
  `,
  styles: DRAW_DIALOG_STYLES,
})
export class CancelDrawDialog {
  protected readonly data = inject<{ draw: DrawView; tenantId: string | null }>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<CancelDrawDialog>);
  private readonly api = inject(PlatformOpsApi);
  private readonly fb = inject(FormBuilder);

  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    reasonCode: ['', [Validators.required, Validators.maxLength(96)]],
    reasonLabel: [''],
    force: [false],
  });

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    const req: CancelDrawRequest = {
      reasonCode: v.reasonCode.toUpperCase().trim(),
      reasonLabel: v.reasonLabel || undefined,
      force: v.force,
    };
    this.submitting.set(true);
    this.error.set(null);
    this.api.cancelDraw(this.data.draw.id, req, this.data.tenantId).subscribe({
      next: () => { this.submitting.set(false); this.dialogRef.close(true); },
      error: (err: unknown) => {
        this.submitting.set(false);
        this.error.set((err as { error?: { title?: string } })?.error?.title ?? 'Erreur.');
      },
    });
  }
}

// ── RescheduleDrawDialog ───────────────────────────────────────────────────────

@Component({
  selector: 'tch-reschedule-draw-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>Reprogrammer — {{ data.draw.channel.code }} {{ data.draw.drawDate }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="draw-dialog__form">
        <mat-form-field appearance="outline">
          <mat-label>Nouvelle date/heure planifiée</mat-label>
          <input matInput formControlName="scheduledAt" type="datetime-local" />
          @if (form.controls.scheduledAt.invalid && form.controls.scheduledAt.touched) {
            <mat-error>Requis.</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Nouvelle clôture (cutoff)</mat-label>
          <input matInput formControlName="cutoffAt" type="datetime-local" />
          @if (form.controls.cutoffAt.invalid && form.controls.cutoffAt.touched) {
            <mat-error>Requis.</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Raison (requise)</mat-label>
          <input matInput formControlName="reason" />
          @if (form.controls.reason.invalid && form.controls.reason.touched) {
            <mat-error>Requis.</mat-error>
          }
        </mat-form-field>
      </form>
      @if (error()) {
        <div class="draw-dialog__error">{{ error() }}</div>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Annuler</button>
      <button mat-flat-button color="primary" [disabled]="form.invalid || submitting()" (click)="submit()">
        @if (submitting()) { <span class="material-symbols-outlined draw-dialog__spinner">progress_activity</span> }
        Reprogrammer
      </button>
    </mat-dialog-actions>
  `,
  styles: DRAW_DIALOG_STYLES,
})
export class RescheduleDrawDialog {
  protected readonly data = inject<{ draw: DrawView; tenantId: string | null }>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<RescheduleDrawDialog>);
  private readonly api = inject(PlatformOpsApi);
  private readonly fb = inject(FormBuilder);

  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    scheduledAt: ['', Validators.required],
    cutoffAt: ['', Validators.required],
    reason: ['', Validators.required],
  });

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    this.submitting.set(true);
    this.error.set(null);
    this.api.rescheduleDraw(
      this.data.draw.id,
      new Date(v.scheduledAt).toISOString(),
      new Date(v.cutoffAt).toISOString(),
      v.reason,
      undefined,
      this.data.tenantId,
    ).subscribe({
      next: () => { this.submitting.set(false); this.dialogRef.close(true); },
      error: (err: unknown) => {
        this.submitting.set(false);
        this.error.set((err as { error?: { title?: string } })?.error?.title ?? 'Erreur.');
      },
    });
  }
}

// ── SimpleDrawActionDialog (lock / unlock / settle / archive) ─────────────────

export type SimpleDrawActionType = 'lock' | 'unlock' | 'settle' | 'archive';
export type BulkDrawActionType = SimpleDrawActionType | 'cancel';

const SIMPLE_LABELS: Record<SimpleDrawActionType, string> = {
  lock: 'Verrouiller',
  unlock: 'Déverrouiller',
  settle: 'Régler',
  archive: 'Archiver',
};

@Component({
  selector: 'tch-simple-draw-action-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatCheckboxModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>{{ label }} — {{ data.draw.channel.code }} {{ data.draw.drawDate }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="draw-dialog__form">
        <mat-form-field appearance="outline">
          <mat-label>Raison {{ data.action === 'lock' || data.action === 'unlock' ? '(optionnelle)' : '(optionnelle)' }}</mat-label>
          <textarea matInput formControlName="reason" rows="2"></textarea>
        </mat-form-field>
        @if (data.action === 'archive') {
          <mat-checkbox formControlName="force">Forcer l'archivage</mat-checkbox>
        }
      </form>
      @if (error()) {
        <div class="draw-dialog__error">{{ error() }}</div>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Annuler</button>
      <button mat-flat-button color="primary" [disabled]="submitting()" (click)="submit()">
        @if (submitting()) { <span class="material-symbols-outlined draw-dialog__spinner">progress_activity</span> }
        {{ label }}
      </button>
    </mat-dialog-actions>
  `,
  styles: DRAW_DIALOG_STYLES,
})
export class SimpleDrawActionDialog {
  protected readonly data = inject<{ draw: DrawView; action: SimpleDrawActionType; tenantId: string | null }>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<SimpleDrawActionDialog>);
  private readonly api = inject(PlatformOpsApi);
  private readonly fb = inject(FormBuilder);

  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  get label() { return SIMPLE_LABELS[this.data.action]; }

  readonly form = this.fb.nonNullable.group({
    reason: [''],
    force: [false],
  });

  submit(): void {
    const v = this.form.getRawValue();
    const reason = v.reason || undefined;
    let call$;
    switch (this.data.action) {
      case 'lock':   call$ = this.api.lockDraw(this.data.draw.id, reason, this.data.tenantId); break;
      case 'unlock': call$ = this.api.unlockDraw(this.data.draw.id, reason, this.data.tenantId); break;
      case 'settle': call$ = this.api.settleDraw(this.data.draw.id, reason, this.data.tenantId); break;
      case 'archive': call$ = this.api.archiveDraw(this.data.draw.id, reason, v.force, this.data.tenantId); break;
    }
    this.submitting.set(true);
    this.error.set(null);
    call$.subscribe({
      next: () => { this.submitting.set(false); this.dialogRef.close(true); },
      error: (err: unknown) => {
        this.submitting.set(false);
        this.error.set((err as { error?: { title?: string } })?.error?.title ?? 'Erreur.');
      },
    });
  }
}

// ── BulkSimpleDrawActionDialog ───────────────────────────────────────────────

@Component({
  selector: 'tch-bulk-simple-draw-action-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatCheckboxModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>{{ label }} — {{ data.draws.length }} tirage(s)</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="draw-dialog__form">
        <mat-form-field appearance="outline">
          <mat-label>Raison</mat-label>
          <textarea matInput formControlName="reason" rows="2"></textarea>
        </mat-form-field>
        @if (data.action === 'archive') {
          <mat-checkbox formControlName="force">Forcer l'archivage</mat-checkbox>
        }
      </form>
      @if (error()) {
        <div class="draw-dialog__error">{{ error() }}</div>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close [disabled]="submitting()">Annuler</button>
      <button mat-flat-button color="primary" [disabled]="submitting()" (click)="submit()">
        @if (submitting()) { <span class="material-symbols-outlined draw-dialog__spinner">progress_activity</span> }
        Appliquer
      </button>
    </mat-dialog-actions>
  `,
  styles: DRAW_DIALOG_STYLES,
})
export class BulkSimpleDrawActionDialog {
  protected readonly data = inject<{ draws: DrawView[]; action: SimpleDrawActionType; tenantId: string | null }>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<BulkSimpleDrawActionDialog>);
  private readonly api = inject(PlatformOpsApi);
  private readonly fb = inject(FormBuilder);

  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  get label() { return SIMPLE_LABELS[this.data.action]; }

  readonly form = this.fb.nonNullable.group({
    reason: [''],
    force: [false],
  });

  submit(): void {
    const v = this.form.getRawValue();
    const reason = v.reason || undefined;
    const req = { drawIds: this.data.draws.map(draw => draw.id), reason, force: v.force };
    const call$ = this.data.action === 'lock'
      ? this.api.lockDraws(req, this.data.tenantId)
      : this.data.action === 'unlock'
        ? this.api.unlockDraws(req, this.data.tenantId)
        : this.data.action === 'settle'
          ? this.api.settleDraws(req, this.data.tenantId)
          : this.api.archiveDraws(req, this.data.tenantId);

    this.submitting.set(true);
    this.error.set(null);
    call$.subscribe({
      next: () => { this.submitting.set(false); this.dialogRef.close(true); },
      error: (err: unknown) => {
        this.submitting.set(false);
        this.error.set((err as { error?: { title?: string } })?.error?.title ?? 'Erreur.');
      },
    });
  }
}

@Component({
  selector: 'tch-bulk-cancel-draw-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatCheckboxModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>Annuler — {{ data.draws.length }} tirage(s)</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="draw-dialog__form">
        <mat-form-field appearance="outline">
          <mat-label>Code de raison</mat-label>
          <input matInput formControlName="reasonCode" placeholder="ADMIN_REQUEST" />
          @if (form.controls.reasonCode.invalid && form.controls.reasonCode.touched) {
            <mat-error>Requis.</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Libellé</mat-label>
          <textarea matInput formControlName="reasonLabel" rows="2"></textarea>
        </mat-form-field>
        <mat-checkbox formControlName="force">Forcer</mat-checkbox>
      </form>
      @if (error()) {
        <div class="draw-dialog__error">{{ error() }}</div>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close [disabled]="submitting()">Annuler</button>
      <button mat-flat-button color="warn" [disabled]="form.invalid || submitting()" (click)="submit()">
        @if (submitting()) { <span class="material-symbols-outlined draw-dialog__spinner">progress_activity</span> }
        Annuler
      </button>
    </mat-dialog-actions>
  `,
  styles: DRAW_DIALOG_STYLES,
})
export class BulkCancelDrawDialog {
  protected readonly data = inject<{ draws: DrawView[]; tenantId: string | null }>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<BulkCancelDrawDialog>);
  private readonly api = inject(PlatformOpsApi);
  private readonly fb = inject(FormBuilder);

  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    reasonCode: ['', [Validators.required, Validators.maxLength(96)]],
    reasonLabel: [''],
    force: [false],
  });

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    const req: CancelDrawsRequest = {
      drawIds: this.data.draws.map(draw => draw.id),
      reasonCode: v.reasonCode.toUpperCase().trim(),
      reasonLabel: v.reasonLabel || undefined,
      force: v.force,
    };
    this.submitting.set(true);
    this.error.set(null);
    this.api.cancelDraws(req, this.data.tenantId).subscribe({
      next: () => { this.submitting.set(false); this.dialogRef.close(true); },
      error: (err: unknown) => {
        this.submitting.set(false);
        this.error.set((err as { error?: { title?: string } })?.error?.title ?? 'Erreur.');
      },
    });
  }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

function toneForStatus(status: string): AdminStatusTone {
  switch (status) {
    case 'OPEN': return 'success';
    case 'RESULTED': case 'SETTLED': return 'info';
    case 'LOCKED': return 'warning';
    case 'CANCELLED': return 'danger';
    case 'ARCHIVED': return 'neutral';
    default: return 'neutral';
  }
}

type DrawActionItem =
  | { kind: 'cancel' }
  | { kind: 'lock' }
  | { kind: 'unlock' }
  | { kind: 'reschedule' }
  | { kind: 'settle' }
  | { kind: 'archive' }
  | { kind: 'correct' };

function actionsForDraw(draw: DrawView): DrawActionItem[] {
  switch (draw.status) {
    case 'SCHEDULED': return [{ kind: 'lock' }, { kind: 'reschedule' }, { kind: 'cancel' }];
    case 'OPEN':      return [{ kind: 'lock' }, { kind: 'cancel' }];
    case 'LOCKED':    return [{ kind: 'unlock' }, { kind: 'cancel' }];
    case 'CLOSED':    return [{ kind: 'settle' }, { kind: 'cancel' }];
    case 'RESULTED':  return [{ kind: 'correct' }, { kind: 'settle' }, { kind: 'archive' }];
    case 'SETTLED':   return [{ kind: 'archive' }];
    default: return [];
  }
}

function todayIsoDate(): string {
  return relativeIsoDate(0);
}

function relativeIsoDate(offsetDays: number): string {
  const now = new Date();
  now.setDate(now.getDate() + offsetDays);
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

const MAX_BULK_DRAW_ACTIONS = 50;

const ACTION_ICON: Record<string, string> = {
  cancel: 'cancel',
  lock: 'lock',
  unlock: 'lock_open',
  reschedule: 'schedule',
  settle: 'paid',
  archive: 'inventory_2',
  correct: 'edit',
};

const STATUS_OPTIONS = [
  { value: '', label: 'Tous les statuts' },
  { value: 'SCHEDULED', label: 'Planifié' },
  { value: 'OPEN', label: 'Ouvert' },
  { value: 'LOCKED', label: 'Verrouillé' },
  { value: 'CLOSED', label: 'Fermé' },
  { value: 'RESULTED', label: 'Résultat appliqué' },
  { value: 'SETTLED', label: 'Réglé' },
  { value: 'CANCELLED', label: 'Annulé' },
  { value: 'ARCHIVED', label: 'Archivé' },
];

const DRAW_ACTION_LABELS: Record<string, string> = {
  cancel: 'Annuler',
  lock: 'Verrouiller',
  unlock: 'Déverrouiller',
  reschedule: 'Reprogrammer',
  settle: 'Régler',
  archive: 'Archiver',
  correct: 'Corriger résultat',
};

// ── Main Page ─────────────────────────────────────────────────────────────────

@Component({
  selector: 'tch-platform-ops-draws-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    AdminCrudShellComponent,
    AdminDataToolbarComponent,
    AdminEmptyStateComponent,
    AdminPageShellComponent,
    AdminStatusPillComponent,
    TchErrorPanel,
    TchLoading,
    TchSearchSelect,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatMenuModule,
    MatSelectModule,
    MatTableModule,
    MatTooltipModule,
  ],
  templateUrl: './platform-ops-draws.page.html',
  styleUrls: ['./platform-ops-draws.page.scss'],
})
export class PlatformOpsDrawsPage implements OnInit {
  private readonly api = inject(PlatformOpsApi);
  private readonly tenantsApi = inject(PlatformTenantsApi);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly displayedColumns = ['select', 'drawDate', 'channel', 'slot', 'status', 'scheduledAt', 'lastResult', 'actions'];
  readonly statusOptions = STATUS_OPTIONS;

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly draws = signal<DrawView[]>([]);
  readonly selectedTenant = signal<TenantSummaryView | null>(null);
  readonly statusFilter = signal('');
  readonly slotKeyFilter = signal('');
  readonly fromFilter = signal(relativeIsoDate(-1));
  readonly toFilter = signal(todayIsoDate());
  readonly deletedVisibility = signal<'active' | 'deleted' | 'all'>('active');
  readonly page = signal(0);
  readonly totalElements = signal(0);
  readonly totalPages = signal(1);
  readonly selectedIds = signal<ReadonlySet<string>>(new Set<string>());
  readonly selectedDraws = computed(() => this.draws().filter(draw => this.selectedIds().has(draw.id)));
  readonly commonBulkActions = computed(() => this.resolveCommonBulkActions(this.selectedDraws()));
  readonly bulkSelectionMessage = computed(() => this.resolveBulkSelectionMessage(this.selectedDraws()));
  readonly maxBulkDrawActions = MAX_BULK_DRAW_ACTIONS;

  toneForStatus = toneForStatus;
  actionsForDraw = actionsForDraw;
  actionIcon = (kind: string) => ACTION_ICON[kind] ?? 'settings';
  actionLabel = (kind: string) => DRAW_ACTION_LABELS[kind] ?? kind;

  private currentBatchDialog: BatchOpDialog | null = null;

  readonly searchTenants = (query: string): Observable<readonly TchSearchOption<TenantSummaryView>[]> =>
    this.tenantsApi.listTenants({ q: query, page: 0, size: 12, status: 'ACTIVE' }).pipe(
      map(page => page.items.map(tenant => this.toTenantOption(tenant))),
    );

  ngOnInit(): void { this.load(); }

  load(): void {
    const tenantId = this.selectedTenantId();
    this.loading.set(true);
    this.error.set(null);
    this.api.listDraws({
      status: this.statusFilter() || undefined,
      resultSlotKey: this.slotKeyFilter() || undefined,
      from: this.fromFilter() || undefined,
      to: this.toFilter() || undefined,
      deletedVisibility: this.deletedVisibility(),
      page: this.page(),
      size: 25,
    }, tenantId).subscribe({
      next: p => {
        this.draws.set(p.items);
        this.selectedIds.set(new Set());
        this.totalElements.set(p.totalElements);
        this.totalPages.set(p.totalPages || 1);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.error.set((err as { error?: { title?: string } })?.error?.title ?? 'Erreur de chargement.');
        this.loading.set(false);
      },
    });
  }

  onStatusChange(v: string): void { this.statusFilter.set(v); this.page.set(0); this.load(); }
  onSearch(v: string): void { this.slotKeyFilter.set(v); this.page.set(0); this.load(); }
  onFromChange(v: string): void { this.fromFilter.set(v); this.page.set(0); this.load(); }
  onToChange(v: string): void { this.toFilter.set(v); this.page.set(0); this.load(); }
  onDeletedVisibilityChange(v: 'active' | 'deleted' | 'all'): void { this.deletedVisibility.set(v); this.page.set(0); this.load(); }
  prevPage(): void { this.page.set(this.page() - 1); this.load(); }
  nextPage(): void { this.page.set(this.page() + 1); this.load(); }

  selectTenant(option: TchSearchOption | null): void {
    const tenant = option?.data as TenantSummaryView | undefined;
    this.selectedTenant.set(tenant ?? null);
    this.page.set(0);
    this.load();
  }

  resetTenant(): void {
    this.selectedTenant.set(null);
    this.page.set(0);
    this.load();
  }

  selectedTenantId(): string | null {
    const tenant = this.selectedTenant();
    return tenant?.id ?? tenant?.tenantId ?? null;
  }

  selectedTenantLabel(): string {
    const tenant = this.selectedTenant();
    return tenant ? `${tenant.name} (${tenant.code})` : 'Tous tenants';
  }

  isSelected(draw: DrawView): boolean {
    return this.selectedIds().has(draw.id);
  }

  toggleDraw(draw: DrawView, checked: boolean): void {
    const next = new Set(this.selectedIds());
    if (checked) next.add(draw.id);
    else next.delete(draw.id);
    this.selectedIds.set(next);
  }

  allPageSelected(): boolean {
    const rows = this.draws();
    return rows.length > 0 && rows.every(row => this.selectedIds().has(row.id));
  }

  somePageSelected(): boolean {
    const rows = this.draws();
    return rows.some(row => this.selectedIds().has(row.id)) && !this.allPageSelected();
  }

  togglePage(checked: boolean): void {
    const next = new Set(this.selectedIds());
    for (const draw of this.draws()) {
      if (checked) next.add(draw.id);
      else next.delete(draw.id);
    }
    this.selectedIds.set(next);
  }

  clearSelection(): void {
    this.selectedIds.set(new Set());
  }

  openBulkAction(action: BulkDrawActionType): void {
    const draws = this.selectedDraws();
    if (!draws.length) return;
    if (draws.length > MAX_BULK_DRAW_ACTIONS) {
      this.snackBar.open(`Maximum ${MAX_BULK_DRAW_ACTIONS} tirages par action. Réduisez la sélection.`, 'OK', { duration: 6000 });
      return;
    }
    const tenantId = this.singleTenantSelectionId();
    if (!tenantId) {
      this.snackBar.open('Sélection multi-tenant: utilisez une action ops par journée/tenant.', 'OK', { duration: 5000 });
      return;
    }
    if (action === 'cancel') {
      const ref = this.dialog.open(BulkCancelDrawDialog, {
        data: { draws, tenantId },
        width: '460px',
      });
      ref.afterClosed().subscribe((done: boolean | null) => {
        if (done) {
          this.snackBar.open('Opération effectuée.', 'OK', { duration: 4000 });
          this.load();
        }
      });
      return;
    }
    const ref = this.dialog.open(BulkSimpleDrawActionDialog, {
      data: { draws, action, tenantId },
      width: '460px',
    });
    ref.afterClosed().subscribe((done: boolean | null) => {
      if (done) {
        this.snackBar.open('Opération effectuée.', 'OK', { duration: 4000 });
        this.load();
      }
    });
  }

  private toTenantOption(tenant: TenantSummaryView): TchSearchOption<TenantSummaryView> {
    return {
      id: tenant.id ?? tenant.tenantId ?? tenant.code,
      title: tenant.name,
      subtitle: tenant.code,
      badge: tenant.status,
      icon: 'apartment',
      data: tenant,
    };
  }

  private resolveCommonBulkActions(draws: DrawView[]): BulkDrawActionType[] {
    if (draws.length === 0 || draws.length > MAX_BULK_DRAW_ACTIONS || !this.singleTenantSelectionId()) return [];
    const allowed = draws.map(draw => actionsForDraw(draw)
      .map(action => action.kind)
      .filter((kind): kind is BulkDrawActionType => kind === 'cancel' || kind === 'lock' || kind === 'unlock' || kind === 'settle' || kind === 'archive'));
    return allowed.reduce<BulkDrawActionType[]>((common, current) => common.filter(kind => current.includes(kind)), allowed[0] ?? []);
  }

  private resolveBulkSelectionMessage(draws: DrawView[]): string {
    if (draws.length > MAX_BULK_DRAW_ACTIONS) {
      return `Maximum ${MAX_BULK_DRAW_ACTIONS} tirages par action. Réduisez la sélection.`;
    }
    if (draws.length > 0 && !this.singleTenantSelectionId()) {
      return 'Sélection multi-tenant: choisissez un tenant ou réduisez la sélection.';
    }
    return 'Aucune action commune pour ces statuts.';
  }

  private singleTenantSelectionId(): string | null {
    const tenantIds = new Set(this.selectedDraws().map(draw => draw.tenantId).filter(Boolean));
    return tenantIds.size === 1 ? [...tenantIds][0] : null;
  }

  // ── Bulk ops ──────────────────────────────────────────────────────────────

  openGenerate(): void {
    this.dialog.open(GenerateDrawsDialog, { width: '520px' });
  }

  openOpenToday(): void {
    this.openBatch('Ouvrir les tirages du jour', true, (tenantCodes, dryRun, limit) => {
      const req: OpenTodayDrawsRequest = { tenantCodes, limit, dryRun };
      this.api.openTodayDraws(req).subscribe({
        next: res => this.currentBatchDialog?.setResult(res as AnyBatchResult),
        error: (err: unknown) => this.currentBatchDialog?.setError((err as { error?: { title?: string } })?.error?.title ?? 'Erreur.'),
      });
    });
  }

  openCloseDue(): void {
    this.openBatch('Fermer les tirages échus', true, (tenantCodes, dryRun, limit) => {
      const req: CloseDueDrawsRequest = { tenantCodes, limit, dryRun };
      this.api.closeDueDraws(req).subscribe({
        next: res => this.currentBatchDialog?.setResult(res as AnyBatchResult),
        error: (err: unknown) => this.currentBatchDialog?.setError((err as { error?: { title?: string } })?.error?.title ?? 'Erreur.'),
      });
    });
  }

  openApply(): void {
    this.dialog.open(ApplyResultsDialog, { width: '480px' });
  }

  private openBatch(
    title: string,
    hasLimit: boolean,
    execute: (tenantCodes: string[], dryRun: boolean, limit?: number) => void,
  ): void {
    const ref = this.dialog.open(BatchOpDialog, { data: { title, hasLimit, execute }, width: '500px' });
    this.currentBatchDialog = ref.componentInstance;
    ref.afterClosed().subscribe(() => { this.currentBatchDialog = null; });
  }

  // ── Row actions ──────────────────────────────────────────────────────────

  openRowAction(draw: DrawView, action: DrawActionItem): void {
    const tenantId = this.selectedTenantId() ?? draw.tenantId ?? null;
    switch (action.kind) {
      case 'cancel':
        this.openAndReload(CancelDrawDialog, { draw, tenantId }, '460px');
        break;
      case 'lock':
      case 'unlock':
      case 'settle':
      case 'archive':
        this.openAndReload(SimpleDrawActionDialog, { draw, action: action.kind, tenantId }, '440px');
        break;
      case 'reschedule':
        this.openAndReload(RescheduleDrawDialog, { draw, tenantId }, '480px');
        break;
      case 'correct':
        this.openAndReload(CorrectDrawResultDialog, { draw, tenantId }, '500px');
        break;
    }
  }

  private openAndReload(component: unknown, data: unknown, width: string): void {
    const ref = this.dialog.open(component as Parameters<MatDialog['open']>[0], { data, width });
    ref.afterClosed().subscribe((done: boolean | null) => {
      if (done) {
        this.snackBar.open('Opération effectuée.', 'OK', { duration: 4000 });
        this.load();
      }
    });
  }

  lotSummary(draw: DrawView): string {
    const r = draw.lastResult;
    if (!r) return '—';
    const parts = [r.lot1, r.lot2, r.lot3, r.lot4].filter(Boolean);
    return parts.length ? parts.join(' · ') : r.status;
  }

  lotteryAsset(slotKey: string): string | null {
    return lotteryAssetForSlot(slotKey);
  }
}
