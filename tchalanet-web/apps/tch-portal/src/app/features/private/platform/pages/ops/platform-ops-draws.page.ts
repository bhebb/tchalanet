import {
  ChangeDetectionStrategy,
  Component,
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

import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { AdminCrudShellComponent } from '../../../shared/admin-ui/admin-crud-shell.component';
import { AdminDataToolbarComponent } from '../../../shared/admin-ui/admin-data-toolbar.component';
import { AdminEmptyStateComponent } from '../../../shared/admin-ui/admin-empty-state.component';
import { AdminPageShellComponent } from '../../../shared/admin-ui/admin-page-shell.component';
import { AdminStatusPillComponent, AdminStatusTone } from '../../../shared/admin-ui/admin-status-pill.component';
import {
  PlatformOpsApi,
  DrawView,
  CancelDrawRequest,
  CorrectDrawResultRequest,
  OpenTodayDrawsRequest,
  CloseDueDrawsRequest,
} from '../../platform-ops-api.service';
import { GenerateDrawsDialog } from './dialogs/generate-draws.dialog';
import { BatchOpDialog, AnyBatchResult } from './dialogs/batch-op.dialog';
import { ApplyResultsDialog } from './dialogs/apply-results.dialog';
import { DrawLifecycleActionDialog, DrawAction, ActionDialogResult, ACTION_LABELS } from './dialogs/draw-lifecycle-action.dialog';
import { lotteryAssetForSlot } from '../../../../../shared/lottery/lottery-assets';

// ── CorrectDrawResultDialog ────────────────────────────────────────────────────

@Component({
  selector: 'tch-correct-draw-result-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatCheckboxModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>Corriger le résultat — {{ data.draw.slot.key }} {{ data.draw.drawDate }}</h2>
    <mat-dialog-content>
      <p style="font-size:0.875rem;color:var(--tch-color-on-surface-variant);margin-top:0">
        Remplace le résultat appliqué par un nouveau résultat confirmé.
        Résultat actuel : <code>{{ data.draw.lastResult?.id ?? 'aucun' }}</code>
      </p>
      <form [formGroup]="form" style="display:flex;flex-direction:column;gap:0.75rem;width:100%">
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
        <div style="background:var(--tch-color-error-container);color:var(--tch-color-on-error-container);padding:0.75rem;border-radius:var(--tch-radius-sm);margin-top:0.5rem;font-size:0.875rem">{{ error() }}</div>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Annuler</button>
      <button mat-flat-button color="warn" [disabled]="form.invalid || submitting()" (click)="submit()">
        @if (submitting()) { <span class="material-symbols-outlined" style="animation:spin 0.8s linear infinite;display:inline-block;vertical-align:middle">progress_activity</span> }
        Corriger
      </button>
    </mat-dialog-actions>
  `,
  styles: [`@keyframes spin { to { transform: rotate(360deg); } }`],
})
export class CorrectDrawResultDialog {
  protected readonly data = inject<{ draw: DrawView }>(MAT_DIALOG_DATA);
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
    this.api.correctDrawResult(this.data.draw.id, req).subscribe({
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
      <form [formGroup]="form" style="display:flex;flex-direction:column;gap:0.75rem;width:100%">
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
        <div style="background:var(--tch-color-error-container);color:var(--tch-color-on-error-container);padding:0.75rem;border-radius:var(--tch-radius-sm);margin-top:0.5rem;font-size:0.875rem">{{ error() }}</div>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Fermer</button>
      <button mat-flat-button color="warn" [disabled]="form.invalid || submitting()" (click)="submit()">
        @if (submitting()) { <span class="material-symbols-outlined" style="animation:spin 0.8s linear infinite;display:inline-block;vertical-align:middle">progress_activity</span> }
        Annuler le tirage
      </button>
    </mat-dialog-actions>
  `,
  styles: [`@keyframes spin { to { transform: rotate(360deg); } }`],
})
export class CancelDrawDialog {
  protected readonly data = inject<{ draw: DrawView }>(MAT_DIALOG_DATA);
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
    this.api.cancelDraw(this.data.draw.id, req).subscribe({
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
      <form [formGroup]="form" style="display:flex;flex-direction:column;gap:0.75rem;width:100%">
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
        <div style="background:var(--tch-color-error-container);color:var(--tch-color-on-error-container);padding:0.75rem;border-radius:var(--tch-radius-sm);margin-top:0.5rem;font-size:0.875rem">{{ error() }}</div>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Annuler</button>
      <button mat-flat-button color="primary" [disabled]="form.invalid || submitting()" (click)="submit()">
        @if (submitting()) { <span class="material-symbols-outlined" style="animation:spin 0.8s linear infinite;display:inline-block;vertical-align:middle">progress_activity</span> }
        Reprogrammer
      </button>
    </mat-dialog-actions>
  `,
  styles: [`@keyframes spin { to { transform: rotate(360deg); } }`],
})
export class RescheduleDrawDialog {
  protected readonly data = inject<{ draw: DrawView }>(MAT_DIALOG_DATA);
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
      <form [formGroup]="form" style="display:flex;flex-direction:column;gap:0.75rem;width:100%">
        <mat-form-field appearance="outline">
          <mat-label>Raison {{ data.action === 'lock' || data.action === 'unlock' ? '(optionnelle)' : '(optionnelle)' }}</mat-label>
          <textarea matInput formControlName="reason" rows="2"></textarea>
        </mat-form-field>
        @if (data.action === 'archive') {
          <mat-checkbox formControlName="force">Forcer l'archivage</mat-checkbox>
        }
      </form>
      @if (error()) {
        <div style="background:var(--tch-color-error-container);color:var(--tch-color-on-error-container);padding:0.75rem;border-radius:var(--tch-radius-sm);margin-top:0.5rem;font-size:0.875rem">{{ error() }}</div>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Annuler</button>
      <button mat-flat-button color="primary" [disabled]="submitting()" (click)="submit()">
        @if (submitting()) { <span class="material-symbols-outlined" style="animation:spin 0.8s linear infinite;display:inline-block;vertical-align:middle">progress_activity</span> }
        {{ label }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`@keyframes spin { to { transform: rotate(360deg); } }`],
})
export class SimpleDrawActionDialog {
  protected readonly data = inject<{ draw: DrawView; action: SimpleDrawActionType }>(MAT_DIALOG_DATA);
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
      case 'lock':   call$ = this.api.lockDraw(this.data.draw.id, reason); break;
      case 'unlock': call$ = this.api.unlockDraw(this.data.draw.id, reason); break;
      case 'settle': call$ = this.api.settleDraw(this.data.draw.id, reason); break;
      case 'archive': call$ = this.api.archiveDraw(this.data.draw.id, reason, v.force); break;
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
    MatButtonModule,
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
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly displayedColumns = ['drawDate', 'channel', 'slot', 'status', 'scheduledAt', 'lastResult', 'actions'];
  readonly statusOptions = STATUS_OPTIONS;

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly draws = signal<DrawView[]>([]);
  readonly statusFilter = signal('');
  readonly slotKeyFilter = signal('');
  readonly page = signal(0);
  readonly totalElements = signal(0);
  readonly totalPages = signal(1);

  toneForStatus = toneForStatus;
  actionsForDraw = actionsForDraw;
  actionIcon = (kind: string) => ACTION_ICON[kind] ?? 'settings';
  actionLabel = (kind: string) => DRAW_ACTION_LABELS[kind] ?? kind;

  private currentBatchDialog: BatchOpDialog | null = null;

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.listDraws({
      status: this.statusFilter() || undefined,
      resultSlotKey: this.slotKeyFilter() || undefined,
      page: this.page(),
      size: 25,
    }).subscribe({
      next: p => {
        this.draws.set(p.items);
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
  prevPage(): void { this.page.set(this.page() - 1); this.load(); }
  nextPage(): void { this.page.set(this.page() + 1); this.load(); }

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
    switch (action.kind) {
      case 'cancel':
        this.openAndReload(CancelDrawDialog, { draw }, '460px');
        break;
      case 'lock':
      case 'unlock':
      case 'settle':
      case 'archive':
        this.openAndReload(SimpleDrawActionDialog, { draw, action: action.kind }, '440px');
        break;
      case 'reschedule':
        this.openAndReload(RescheduleDrawDialog, { draw }, '480px');
        break;
      case 'correct':
        this.openAndReload(CorrectDrawResultDialog, { draw }, '500px');
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
