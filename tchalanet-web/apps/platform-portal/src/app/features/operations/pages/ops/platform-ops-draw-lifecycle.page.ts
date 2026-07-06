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
  ConsoleDrawSlotIdentityComponent,
  consoleDrawIdentity,
  consoleDrawLifecycleActionLabel,
  consoleDrawStatusLabel,
  consoleDrawStatusTone,
  type ConsoleDrawSlotIdentity,
} from '@tch/web/console';
import {
  DrawView,
  PlatformOpsApi,
} from '../../data-access/platform-ops-api.service';
import {
  DrawLifecycleActionDialog,
  DrawAction,
  ActionDialogResult,
} from '../../components/dialogs/draw-lifecycle-action.dialog';

function toneForStatus(status: string): AdminStatusTone {
  return consoleDrawStatusTone(status);
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
  { value: 'SCHEDULED', label: consoleDrawStatusLabel('SCHEDULED') },
  { value: 'OPEN', label: consoleDrawStatusLabel('OPEN') },
  { value: 'LOCKED', label: consoleDrawStatusLabel('LOCKED') },
  { value: 'CLOSED', label: consoleDrawStatusLabel('CLOSED') },
  { value: 'SETTLED', label: consoleDrawStatusLabel('SETTLED') },
  { value: 'ARCHIVED', label: consoleDrawStatusLabel('ARCHIVED') },
  { value: 'CANCELLED', label: consoleDrawStatusLabel('CANCELLED') },
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
    ConsoleDrawSlotIdentityComponent,
  ],
  templateUrl: './platform-ops-draw-lifecycle.page.html',
  styleUrls: ['./platform-ops-draw-lifecycle.page.scss'],
})
export class PlatformOpsDrawLifecyclePage implements OnInit {
  private readonly api = inject(PlatformOpsApi);
  private readonly dialog = inject(MatDialog);
  private readonly translate = inject(TranslateService);

  readonly displayedColumns = ['channel', 'status', 'scheduledAt', 'openedAt', 'actions'];
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
  actionLabel = (a: DrawAction) => consoleDrawLifecycleActionLabel(a);

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
      q
        ? this.draws().filter(d => {
            const identity = this.drawIdentity(d);
            return [
              d.channel.code,
              d.channel.name,
              d.slot.key,
              identity.channelName,
              identity.providerName,
              identity.slotLabel,
            ]
              .filter(Boolean)
              .some(value => value!.toLowerCase().includes(q));
          })
        : this.draws(),
    );
  }

  openAction(draw: DrawView, action: DrawAction): void {
    if (this.dryRun()) {
      this.actionFeedback.set({
        title: 'Dry-run',
        message: `Dry-run: ${this.actionLabel(action)} serait exécuté sur ${this.drawDisplayLabel(draw)}.`,
        severity: 'info',
      });
      return;
    }

    const ref = this.dialog.open(DrawLifecycleActionDialog, {
      data: { draw, action, drawLabel: this.drawDisplayLabel(draw) },
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
          title: `${this.actionLabel(action)} exécuté`,
          message: `${this.drawDisplayLabel(draw)} a été mis à jour.`,
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

  drawIdentity(draw: DrawView): ConsoleDrawSlotIdentity {
    return consoleDrawIdentity({
      channelCode: draw.channel.code,
      channelName: draw.channel.name,
      slotKey: draw.slot.key,
      slotLabel: draw.slot.label,
      officialDateLabel: draw.drawDate,
      officialTimeLabel: hhmm(draw.slot.drawTime),
      officialTimezoneLabel: draw.slot.timezone,
      fallbackTitle: draw.channel.code,
    });
  }

  private drawDisplayLabel(draw: DrawView): string {
    const identity = this.drawIdentity(draw);
    return identity.channelName ?? identity.channelShortName ?? draw.channel.code;
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

function hhmm(time: string | null | undefined): string | null {
  if (!time) return null;
  return time.length > 5 ? time.substring(0, 5) : time;
}
