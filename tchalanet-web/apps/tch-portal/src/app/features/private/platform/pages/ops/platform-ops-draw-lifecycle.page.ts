import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';

import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { AdminCrudShellComponent } from '../../../shared/admin-ui/admin-crud-shell.component';
import { AdminDataToolbarComponent } from '../../../shared/admin-ui/admin-data-toolbar.component';
import { AdminEmptyStateComponent } from '../../../shared/admin-ui/admin-empty-state.component';
import { AdminPageShellComponent } from '../../../shared/admin-ui/admin-page-shell.component';
import { AdminStatusPillComponent, AdminStatusTone } from '../../../shared/admin-ui/admin-status-pill.component';
import {
  DrawView,
  PlatformOpsApi,
} from '../../platform-ops-api.service';
import {
  DrawLifecycleActionDialog,
  DrawAction,
  ActionDialogResult,
  ACTION_LABELS,
} from './dialogs/draw-lifecycle-action.dialog';

function toneForStatus(status: string): AdminStatusTone {
  switch (status) {
    case 'OPEN':
    case 'SETTLED':
      return 'success';
    case 'LOCKED':
      return 'warning';
    case 'CANCELLED':
      return 'danger';
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
  templateUrl: './platform-ops-draw-lifecycle.page.html',
  styleUrls: ['./platform-ops-draw-lifecycle.page.scss'],
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
  readonly draws = signal<DrawView[]>([]);
  readonly search = signal('');
  readonly statusFilter = signal('');
  readonly page = signal(0);
  readonly totalElements = signal(0);
  readonly totalPages = signal(1);
  readonly dryRun = signal(false);
  readonly filteredDraws = signal<DrawView[]>([]);

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
          this.draws.set(page.items);
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
      q ? this.draws().filter(d => d.channel.code.toLowerCase().includes(q) || d.channel.name.toLowerCase().includes(q)) : this.draws(),
    );
  }

  openAction(draw: DrawView, action: DrawAction): void {
    if (this.dryRun()) {
      this.snackBar.open(`DryRun: ${ACTION_LABELS[action]} serait exécuté sur ${draw.channel.name}`, 'OK', { duration: 4000 });
      return;
    }

    const ref = this.dialog.open(DrawLifecycleActionDialog, {
      data: { draw, action },
      width: '460px',
    });

    ref.afterClosed().subscribe((result: ActionDialogResult | null) => {
      if (result === null || result === undefined) return;
      this.executeAction(draw, action, result);
    });
  }

  private executeAction(draw: DrawView, action: DrawAction, result: ActionDialogResult): void {
    this.busy.set(true);
    let call$;

    switch (action) {
      case 'cancel':
        call$ = this.api.cancelDraw(draw.id, { reasonCode: result.reason ?? 'ADMIN_REQUEST' });
        break;
      case 'lock':
        call$ = this.api.lockDraw(draw.id, result.reason);
        break;
      case 'unlock':
        call$ = this.api.unlockDraw(draw.id, result.reason);
        break;
      case 'settle':
        call$ = this.api.settleDraw(draw.id);
        break;
      case 'archive':
        call$ = this.api.archiveDraw(draw.id);
        break;
      case 'reschedule':
        call$ = this.api.rescheduleDraw(draw.id, result.newScheduledAt!, result.newScheduledAt!, result.reason ?? 'reprogrammé');
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
        this.snackBar.open(pd?.title ?? "Erreur lors de l'opération.", 'OK', { duration: 5000 });
      },
    });
  }
}
