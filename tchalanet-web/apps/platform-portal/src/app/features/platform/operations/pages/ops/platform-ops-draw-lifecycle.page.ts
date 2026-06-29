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
import { MatTableModule } from '@angular/material/table';
import { TranslateService } from '@ngx-translate/core';

import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { TchErrorPanel, TchLoading, TchSectionError } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { AdminCrudShellComponent } from '@tch/ui/console';
import { AdminDataToolbarComponent } from '@tch/ui/console';
import { AdminEmptyStateComponent } from '@tch/ui/console';
import { AdminPageShellComponent } from '@tch/ui/console';
import { AdminStatusPillComponent, AdminStatusTone } from '@tch/ui/console';
import {
  DrawView,
  PlatformOpsApi,
} from '../../data-access/platform-ops-api.service';
import {
  DrawLifecycleActionDialog,
  DrawAction,
  ActionDialogResult,
  ACTION_LABELS,
} from '../../components/dialogs/draw-lifecycle-action.dialog';

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
    TchSectionError,
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
  private readonly translate = inject(TranslateService);

  readonly displayedColumns = ['channelCode', 'channelName', 'status', 'scheduledAt', 'openedAt', 'actions'];
  readonly statusOptions = STATUS_OPTIONS;

  readonly loading = signal(false);
  readonly busy = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly actionFeedback = signal<ErrorViewModel | null>(null);
  readonly draws = signal<DrawView[]>([]);
  readonly search = signal('');
  readonly statusFilter = signal('');
  readonly page = signal(0);
  readonly totalElements = signal(0);
  readonly totalPages = signal(1);
  readonly hasNext = signal(false);
  readonly hasPrevious = signal(false);
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
      .listDrawsForLifecycle({
        status: this.statusFilter() || undefined,
        page: this.page(),
        size: 20,
        suppressShellFeedback: true,
      })
      .subscribe({
        next: page => {
          this.draws.set(page.items);
          this.totalElements.set(page.totalElements);
          this.page.set(page.page);
          this.totalPages.set(page.totalPages || 1);
          this.hasNext.set(page.hasNext ?? false);
          this.hasPrevious.set(page.hasPrevious ?? false);
          this.applySearch();
          this.loading.set(false);
        },
        error: (err: unknown) => {
          this.error.set(this.errorViewModel(err, 'platform.ops.drawLifecycle.list'));
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
    if (!this.hasPrevious()) return;
    this.page.set(this.page() - 1);
    this.load();
  }

  nextPage(): void {
    if (!this.hasNext()) return;
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
      this.actionFeedback.set({
        title: 'Dry-run',
        message: `Dry-run: ${ACTION_LABELS[action]} serait exécuté sur ${draw.channel.name}.`,
        severity: 'info',
      });
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
    this.actionFeedback.set(null);
    let call$;

    switch (action) {
      case 'cancel':
        call$ = this.api.cancelDraw(draw.id, { reasonCode: result.reason ?? 'ADMIN_REQUEST' }, null, { suppressShellFeedback: true });
        break;
      case 'lock':
        call$ = this.api.lockDraw(draw.id, result.reason, null, { suppressShellFeedback: true });
        break;
      case 'unlock':
        call$ = this.api.unlockDraw(draw.id, result.reason, null, { suppressShellFeedback: true });
        break;
      case 'settle':
        call$ = this.api.settleDraw(draw.id, undefined, null, { suppressShellFeedback: true });
        break;
      case 'archive':
        call$ = this.api.archiveDraw(draw.id, undefined, undefined, null, { suppressShellFeedback: true });
        break;
      case 'reschedule':
        if (!result.newScheduledAt) {
          this.busy.set(false);
          return;
        }
        call$ = this.api.rescheduleDraw(
          draw.id,
          result.newScheduledAt,
          result.newScheduledAt,
          result.reason ?? 'reprogrammé',
          undefined,
          null,
          { suppressShellFeedback: true },
        );
        break;
    }

    call$.subscribe({
      next: () => {
        this.busy.set(false);
        this.actionFeedback.set({
          title: `${ACTION_LABELS[action]} exécuté`,
          message: `${draw.channel.name} a été mis à jour.`,
          severity: 'info',
        });
        this.load();
      },
      error: (err: unknown) => {
        this.busy.set(false);
        this.actionFeedback.set(this.errorViewModel(err, `platform.ops.drawLifecycle.${action}`));
      },
    });
  }

  private errorViewModel(err: unknown, source: string): ErrorViewModel {
    const problem = (err as { error?: ProblemDetail })?.error;
    if (problem) {
      const normalized = webAppErrorFromProblemDetail(problem, source, 'page');
      const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
      return toErrorViewModel(normalized, copy);
    }

    return {
      title: this.translate.instant('common.errors.fallback.title'),
      message: this.translate.instant('common.errors.fallback.message'),
      severity: 'error',
    };
  }
}
