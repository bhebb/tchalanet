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
import {
  MAT_DIALOG_DATA,
  MatDialog,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';

import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { AdminCrudShellComponent } from '../../../private/shared/admin-ui/admin-crud-shell.component';
import { AdminDataToolbarComponent } from '../../../private/shared/admin-ui/admin-data-toolbar.component';
import { AdminEmptyStateComponent } from '../../../private/shared/admin-ui/admin-empty-state.component';
import { AdminPageShellComponent } from '../../../private/shared/admin-ui/admin-page-shell.component';
import { AdminStatusPillComponent, AdminStatusTone } from '../../../private/shared/admin-ui/admin-status-pill.component';
import {
  DrawSummaryResponse,
  PlatformOpsApi,
} from '../../platform-ops-api.service';

type DrawAction = 'cancel' | 'lock' | 'unlock' | 'settle' | 'archive' | 'reschedule';

function toneForStatus(status: string): AdminStatusTone {
  switch (status) {
    case 'OPEN':
    case 'SETTLED':
      return 'success';
    case 'LOCKED':
      return 'warning';
    case 'CANCELLED':
      return 'danger';
    case 'SCHEDULED':
    case 'CLOSED':
    case 'ARCHIVED':
    default:
      return 'neutral';
  }
}

function actionsForStatus(status: string): DrawAction[] {
  switch (status) {
    case 'SCHEDULED':
      return ['lock', 'reschedule', 'cancel'];
    case 'OPEN':
      return ['lock', 'cancel'];
    case 'LOCKED':
      return ['unlock', 'settle', 'cancel'];
    case 'CLOSED':
      return ['settle', 'cancel'];
    case 'SETTLED':
      return ['archive'];
    default:
      return [];
  }
}

const ACTION_LABELS: Record<DrawAction, string> = {
  cancel: 'Annuler',
  lock: 'Verrouiller',
  unlock: 'Déverrouiller',
  settle: 'Régler',
  archive: 'Archiver',
  reschedule: 'Reprogrammer',
};

// ── Lifecycle Action Dialog ────────────────────────────────────────────────────

interface ActionDialogData {
  draw: DrawSummaryResponse;
  action: DrawAction;
}

interface ActionDialogResult {
  reason?: string;
  newScheduledAt?: string;
}

@Component({
  selector: 'tch-draw-lifecycle-action-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ actionLabel }} — {{ data.draw.channelName }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="dialog-form">
        @if (data.action === 'reschedule') {
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Nouvelle date/heure</mat-label>
            <input matInput formControlName="newScheduledAt" type="datetime-local" />
            @if (form.controls['newScheduledAt'].invalid && form.controls['newScheduledAt'].touched) {
              <mat-error>Date/heure requise.</mat-error>
            }
          </mat-form-field>
        }
        @if (needsReason) {
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>{{ data.action === 'cancel' ? 'Raison (requise)' : 'Raison (optionnelle)' }}</mat-label>
            <textarea matInput formControlName="reason" rows="3"></textarea>
            @if (form.controls['reason'].invalid && form.controls['reason'].touched) {
              <mat-error>Raison requise.</mat-error>
            }
          </mat-form-field>
        }
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="null">Annuler</button>
      <button mat-flat-button color="primary" [disabled]="form.invalid" (click)="confirm()">
        {{ actionLabel }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      .dialog-form { display: flex; flex-direction: column; gap: 0.75rem; min-width: 380px; }
      .full-width { width: 100%; }
    `,
  ],
})
export class DrawLifecycleActionDialog {
  protected readonly data = inject<ActionDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<DrawLifecycleActionDialog>);
  private readonly fb = inject(FormBuilder);

  get actionLabel(): string {
    return ACTION_LABELS[this.data.action];
  }

  get needsReason(): boolean {
    return ['cancel', 'lock', 'unlock'].includes(this.data.action);
  }

  readonly form = this.fb.group({
    reason: [
      '',
      this.data.action === 'cancel' ? [Validators.required, Validators.minLength(3)] : [],
    ],
    newScheduledAt: [
      '',
      this.data.action === 'reschedule' ? [Validators.required] : [],
    ],
  });

  confirm(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.value;
    const result: ActionDialogResult = {};
    if (v.reason) result.reason = v.reason;
    if (v.newScheduledAt) result.newScheduledAt = v.newScheduledAt;
    this.dialogRef.close(result);
  }
}

// ── Main Page ─────────────────────────────────────────────────────────────────

const STATUS_OPTIONS = [
  { value: '', label: 'Tous les statuts' },
  { value: 'SCHEDULED', label: 'Planifié' },
  { value: 'OPEN', label: 'Ouvert' },
  { value: 'LOCKED', label: 'Verrouillé' },
  { value: 'CLOSED', label: 'Fermé' },
  { value: 'SETTLED', label: 'Réglé' },
  { value: 'ARCHIVED', label: 'Archivé' },
  { value: 'CANCELLED', label: 'Annulé' },
];

@Component({
  selector: 'tch-platform-ops-draw-lifecycle-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    ReactiveFormsModule,
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
    MatSelectModule,
    MatTableModule,
  ],
  template: `
    <tch-admin-page-shell
      title="Cycle de vie des tirages"
      description="Gérez l'état de chaque tirage : verrouillage, règlement, archivage, annulation."
    >
      <div actions>
        <label class="dryrun-toggle">
          <input type="checkbox" [checked]="dryRun()" (change)="dryRun.set(!dryRun())" />
          Dry-run
        </label>
      </div>

      <tch-admin-crud-shell>
        <div toolbar>
          <tch-admin-data-toolbar
            searchPlaceholder="Filtrer par canal..."
            [searchValue]="search()"
            (searchChange)="onSearch($event)"
          >
            <mat-form-field appearance="outline" style="min-width:180px">
              <mat-label>Statut</mat-label>
              <mat-select [value]="statusFilter()" (valueChange)="onStatusChange($event)">
                @for (opt of statusOptions; track opt.value) {
                  <mat-option [value]="opt.value">{{ opt.label }}</mat-option>
                }
              </mat-select>
            </mat-form-field>
            <button mat-stroked-button (click)="load()">
              <span class="material-symbols-outlined">refresh</span>
            </button>
          </tch-admin-data-toolbar>
        </div>

        <div content>
          @if (loading()) {
            <tch-loading label="Chargement..." />
          } @else if (error()) {
            <tch-error-panel [title]="error()!" [showRetry]="true" retryLabel="Réessayer" (retry)="load()" />
          } @else if (draws().length === 0) {
            <tch-admin-empty-state icon="event_busy" title="Aucun tirage" message="Aucun tirage trouvé pour ce filtre." />
          } @else {
            <div class="table-container">
              <table mat-table [dataSource]="filteredDraws()">
                <ng-container matColumnDef="channelCode">
                  <th mat-header-cell *matHeaderCellDef>Code canal</th>
                  <td mat-cell *matCellDef="let row">{{ row.channelCode }}</td>
                </ng-container>
                <ng-container matColumnDef="channelName">
                  <th mat-header-cell *matHeaderCellDef>Canal</th>
                  <td mat-cell *matCellDef="let row">{{ row.channelName }}</td>
                </ng-container>
                <ng-container matColumnDef="status">
                  <th mat-header-cell *matHeaderCellDef>Statut</th>
                  <td mat-cell *matCellDef="let row">
                    <tch-admin-status-pill [label]="row.status" [tone]="toneForStatus(row.status)" />
                  </td>
                </ng-container>
                <ng-container matColumnDef="scheduledAt">
                  <th mat-header-cell *matHeaderCellDef>Planifié</th>
                  <td mat-cell *matCellDef="let row">{{ row.scheduledAt | date: 'short' }}</td>
                </ng-container>
                <ng-container matColumnDef="openedAt">
                  <th mat-header-cell *matHeaderCellDef>Ouvert</th>
                  <td mat-cell *matCellDef="let row">{{ row.openedAt ? (row.openedAt | date: 'short') : '—' }}</td>
                </ng-container>
                <ng-container matColumnDef="actions">
                  <th mat-header-cell *matHeaderCellDef></th>
                  <td mat-cell *matCellDef="let row">
                    <div class="action-row">
                      @for (action of actionsForStatus(row.status); track action) {
                        <button
                          mat-stroked-button
                          [color]="action === 'cancel' ? 'warn' : 'primary'"
                          (click)="openAction(row, action)"
                          [disabled]="busy()"
                        >
                          {{ actionLabel(action) }}
                        </button>
                      }
                    </div>
                  </td>
                </ng-container>
                <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
                <tr mat-row *matRowDef="let row; columns: displayedColumns"></tr>
              </table>
            </div>
          }
        </div>

        <div footer>
          <span class="footer-count">{{ totalElements() }} tirage(s)</span>
          <div class="pagination">
            <button mat-icon-button [disabled]="page() === 0" (click)="prevPage()">
              <span class="material-symbols-outlined">chevron_left</span>
            </button>
            <span>Page {{ page() + 1 }} / {{ totalPages() }}</span>
            <button mat-icon-button [disabled]="page() + 1 >= totalPages()" (click)="nextPage()">
              <span class="material-symbols-outlined">chevron_right</span>
            </button>
          </div>
        </div>
      </tch-admin-crud-shell>
    </tch-admin-page-shell>
  `,
  styles: [
    `
      .table-container { overflow-x: auto; }
      table { width: 100%; }
      .action-row { display: flex; gap: 0.5rem; flex-wrap: wrap; }
      .footer-count { font-size: 0.875rem; color: var(--tch-color-on-surface-variant); }
      .pagination { display: flex; align-items: center; gap: 0.5rem; font-size: 0.875rem; }
      .dryrun-toggle { display: flex; align-items: center; gap: 0.375rem; font-size: 0.875rem; cursor: pointer; }
    `,
  ],
})
export class PlatformOpsDrawLifecyclePage implements OnInit {
  private readonly api = inject(PlatformOpsApi);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly displayedColumns = ['channelCode', 'channelName', 'status', 'scheduledAt', 'openedAt', 'actions'];
  readonly statusOptions = STATUS_OPTIONS;

  readonly loading = signal(false);
  readonly busy = signal(false);
  readonly error = signal<string | null>(null);
  readonly draws = signal<DrawSummaryResponse[]>([]);
  readonly search = signal('');
  readonly statusFilter = signal('');
  readonly page = signal(0);
  readonly totalElements = signal(0);
  readonly totalPages = signal(1);
  readonly dryRun = signal(false);

  readonly filteredDraws = signal<DrawSummaryResponse[]>([]);

  toneForStatus = toneForStatus;
  actionsForStatus = actionsForStatus;
  actionLabel = (a: DrawAction) => ACTION_LABELS[a];

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api
      .listDrawsForLifecycle({ status: this.statusFilter() || undefined, page: this.page(), size: 20 })
      .subscribe({
        next: page => {
          this.draws.set(page.content);
          this.totalElements.set(page.totalElements);
          this.totalPages.set(page.totalPages || 1);
          this.applySearch();
          this.loading.set(false);
        },
        error: (err: unknown) => {
          const pd = (err as { error?: { title?: string } })?.error;
          this.error.set(pd?.title ?? 'Erreur de chargement.');
          this.loading.set(false);
        },
      });
  }

  onSearch(v: string): void {
    this.search.set(v);
    this.applySearch();
  }

  onStatusChange(v: string): void {
    this.statusFilter.set(v);
    this.page.set(0);
    this.load();
  }

  prevPage(): void {
    this.page.set(this.page() - 1);
    this.load();
  }

  nextPage(): void {
    this.page.set(this.page() + 1);
    this.load();
  }

  private applySearch(): void {
    const q = this.search().toLowerCase();
    this.filteredDraws.set(
      q ? this.draws().filter(d => d.channelCode.toLowerCase().includes(q) || d.channelName.toLowerCase().includes(q)) : this.draws(),
    );
  }

  openAction(draw: DrawSummaryResponse, action: DrawAction): void {
    if (this.dryRun()) {
      this.snackBar.open(`DryRun: ${ACTION_LABELS[action]} serait exécuté sur ${draw.channelName}`, 'OK', { duration: 4000 });
      return;
    }

    const ref = this.dialog.open(DrawLifecycleActionDialog, {
      data: { draw, action } satisfies ActionDialogData,
      width: '460px',
    });

    ref.afterClosed().subscribe((result: ActionDialogResult | null) => {
      if (result === null || result === undefined) return;
      this.executeAction(draw, action, result);
    });
  }

  private executeAction(draw: DrawSummaryResponse, action: DrawAction, result: ActionDialogResult): void {
    this.busy.set(true);
    let call$;

    switch (action) {
      case 'cancel':
        call$ = this.api.cancelDraw(draw.drawId, result.reason!);
        break;
      case 'lock':
        call$ = this.api.lockDraw(draw.drawId, result.reason);
        break;
      case 'unlock':
        call$ = this.api.unlockDraw(draw.drawId, result.reason);
        break;
      case 'settle':
        call$ = this.api.settleDraw(draw.drawId);
        break;
      case 'archive':
        call$ = this.api.archiveDraw(draw.drawId);
        break;
      case 'reschedule':
        call$ = this.api.rescheduleDraw(draw.drawId, result.newScheduledAt!);
        break;
    }

    call$.subscribe({
      next: () => {
        this.busy.set(false);
        this.snackBar.open(`${ACTION_LABELS[action]} exécuté avec succès.`, 'OK', { duration: 4000 });
        this.load();
      },
      error: (err: unknown) => {
        this.busy.set(false);
        const pd = (err as { error?: { title?: string } })?.error;
        this.snackBar.open(pd?.title ?? 'Erreur lors de l\'opération.', 'OK', { duration: 5000 });
      },
    });
  }
}
