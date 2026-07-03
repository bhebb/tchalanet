import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { TranslateService } from '@ngx-translate/core';

import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { AccessService } from '@tch/core/auth';
import { TchLoading, TchErrorPanel, TchSectionError } from '@tch/ui/components';
import { CONSOLE_DRAW_RESULT_ACCESS } from '@tch/web/console';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { AdminPageShellComponent } from '@tch/ui/console';
import { AdminEmptyStateComponent } from '@tch/ui/console';
import { AdminCrudShellComponent } from '@tch/ui/console';
import { AdminDataToolbarComponent } from '@tch/ui/console';
import { AdminStatusTone } from '@tch/ui/console';
import {
  ConsoleDrawResultActionEvent,
  ConsoleDrawResultRow,
  ConsoleDrawResultsTableComponent,
  consoleDrawResultQualityLabel,
  consoleDrawResultQualityTone,
  consoleDrawResultStatusLabel,
  consoleDrawResultStatusTone,
} from '@tch/web/console';
import {
  PlatformOpsApi,
  DrawResultOpsResponse,
  HaitiLots,
  OpsDrawResultQuality,
} from '../../data-access/platform-ops-api.service';
import { FetchResultsDialog } from '../../components/dialogs/fetch-results.dialog';
import { ManualResultDialog } from '../../components/dialogs/manual-result.dialog';
import { OverrideResultDialog } from '../../components/dialogs/override-result.dialog';
import { lotteryAssetForSlot } from '../../../../../shared/lottery/lottery-assets';

const RESULT_STATUS_OPTIONS = [
  { value: '', label: 'Tous les statuts' },
  { value: 'PROVISIONAL', label: consoleDrawResultStatusLabel('PROVISIONAL') },
  { value: 'CONFIRMED', label: consoleDrawResultStatusLabel('CONFIRMED') },
  { value: 'OVERRIDDEN', label: consoleDrawResultStatusLabel('OVERRIDDEN') },
  { value: 'ERROR', label: consoleDrawResultStatusLabel('ERROR') },
];

const RESULT_QUALITY_OPTIONS: { value: OpsDrawResultQuality | ''; label: string }[] = [
  { value: '', label: 'Toutes qualités' },
  { value: 'COMPLETE', label: consoleDrawResultQualityLabel('COMPLETE') },
  { value: 'SUSPECT', label: consoleDrawResultQualityLabel('SUSPECT') },
  { value: 'INVALID', label: consoleDrawResultQualityLabel('INVALID') },
];

@Component({
  selector: 'tch-platform-ops-draw-results-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminPageShellComponent,
    AdminEmptyStateComponent,
    AdminCrudShellComponent,
    AdminDataToolbarComponent,
    ConsoleDrawResultsTableComponent,
    TchLoading,
    TchErrorPanel,
    TchSectionError,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatSelectModule,
  ],
  templateUrl: './platform-ops-draw-results.page.html',
  styleUrls: ['./platform-ops-draw-results.page.scss'],
})
export class PlatformOpsDrawResultsPage implements OnInit {
  private readonly api = inject(PlatformOpsApi);
  private readonly dialog = inject(MatDialog);
  private readonly translate = inject(TranslateService);
  private readonly access = inject(AccessService);

  readonly canManageResults = computed(() =>
    this.access.can([
      ...CONSOLE_DRAW_RESULT_ACCESS.manual,
      ...CONSOLE_DRAW_RESULT_ACCESS.fetch,
      ...CONSOLE_DRAW_RESULT_ACCESS.confirm,
      ...CONSOLE_DRAW_RESULT_ACCESS.override,
    ]),
  );
  readonly canFetchResults = computed(() => this.access.can(CONSOLE_DRAW_RESULT_ACCESS.fetch));
  readonly canEnterManualResults = computed(() => this.access.can(CONSOLE_DRAW_RESULT_ACCESS.manual));
  readonly canConfirmResults = computed(() => this.access.can(CONSOLE_DRAW_RESULT_ACCESS.confirm));
  readonly canOverrideResults = computed(() => this.access.can(CONSOLE_DRAW_RESULT_ACCESS.override));
  readonly loading = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly actionFeedback = signal<ErrorViewModel | null>(null);
  readonly page = signal<{
    items: DrawResultOpsResponse[];
    totalElements: number;
    totalPages: number;
    page: number;
    size: number;
  } | null>(null);
  readonly pageIndex = signal(0);
  readonly pageSize = signal(20);
  readonly actionLoading = signal(false);
  readonly slotKeyFilter = signal('');
  readonly statusFilter = signal('');
  readonly qualityFilter = signal<OpsDrawResultQuality | ''>('');
  readonly fromFilter = signal('');
  readonly toFilter = signal('');
  readonly statusOptions = RESULT_STATUS_OPTIONS;
  readonly qualityOptions = RESULT_QUALITY_OPTIONS;
  readonly rows = computed<readonly ConsoleDrawResultRow[]>(() =>
    (this.page()?.items ?? []).map(row => this.toConsoleRow(row)),
  );

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.listDrawResults({
      slotKey: this.slotKeyFilter() || undefined,
      status: this.statusFilter() || undefined,
      quality: this.qualityFilter() || undefined,
      from: this.fromFilter() || undefined,
      to: this.toFilter() || undefined,
      page: this.pageIndex(),
      size: this.pageSize(),
      sort: 'occurredAt,DESC',
    }, { suppressShellFeedback: true }).subscribe({
      next: v => { this.page.set(v); this.loading.set(false); },
      error: (err: unknown) => {
        this.error.set(this.errorViewModel(err, 'platform.ops.drawResults.list'));
        this.loading.set(false);
      },
    });
  }

  onPage(e: PageEvent): void {
    this.pageIndex.set(e.pageIndex);
    this.pageSize.set(e.pageSize);
    this.load();
  }

  onSlotSearch(v: string): void { this.slotKeyFilter.set(v.trim()); this.pageIndex.set(0); this.load(); }
  onStatusChange(v: string): void { this.statusFilter.set(v); this.pageIndex.set(0); this.load(); }
  onQualityChange(v: OpsDrawResultQuality | ''): void { this.qualityFilter.set(v); this.pageIndex.set(0); this.load(); }
  onFromChange(v: string): void { this.fromFilter.set(v); this.pageIndex.set(0); this.load(); }
  onToChange(v: string): void { this.toFilter.set(v); this.pageIndex.set(0); this.load(); }

  resetFilters(): void {
    this.slotKeyFilter.set('');
    this.statusFilter.set('');
    this.qualityFilter.set('');
    this.fromFilter.set('');
    this.toFilter.set('');
    this.pageIndex.set(0);
    this.load();
  }

  openFetch(mode: 'fetch'): void {
    this.dialog.open(FetchResultsDialog, {
      data: {
        title: 'Fetch résultats externes',
        mode,
        onSuccess: () => this.load(),
      },
      width: '500px',
    });
  }

  openManual(): void {
    const ref = this.dialog.open(ManualResultDialog, { width: '520px' });
    ref.afterClosed().subscribe(done => {
      if (done) this.load();
    });
  }

  openOverride(row: DrawResultOpsResponse): void {
    this.dialog.open(OverrideResultDialog, {
      data: { row, onSuccess: () => this.load() },
      width: '500px',
    });
  }

  confirmResult(row: DrawResultOpsResponse): void {
    this.actionLoading.set(true);
    this.actionFeedback.set(null);
    this.api.confirmDrawResult(row.id, { suppressShellFeedback: true }).subscribe({
      next: () => {
        this.actionLoading.set(false);
        this.actionFeedback.set({
          title: 'Résultat confirmé',
          message: 'Le résultat a été confirmé.',
          severity: 'info',
        });
        this.load();
      },
      error: (err: unknown) => {
        this.actionLoading.set(false);
        this.actionFeedback.set(this.errorViewModel(err, 'platform.ops.drawResults.confirm'));
      },
    });
  }

  onResultAction(event: ConsoleDrawResultActionEvent): void {
    const row = this.page()?.items.find(item => item.id === event.row.id);
    if (!row) return;
    switch (event.action.id) {
      case 'confirm':
        this.confirmResult(row);
        break;
      case 'override':
        this.openOverride(row);
        break;
    }
  }

  statusTone(status: string): AdminStatusTone {
    return consoleDrawResultStatusTone(status);
  }

  qualityTone(quality: string): AdminStatusTone {
    return consoleDrawResultQualityTone(quality);
  }

  lotteryAsset(slotKey: string): string | null {
    return lotteryAssetForSlot(slotKey);
  }

  haitiLots(row: DrawResultOpsResponse): HaitiLots {
    const value = row.haitiResult;
    if (!value || typeof value !== 'object') return {};
    return value as HaitiLots;
  }

  private toConsoleRow(row: DrawResultOpsResponse): ConsoleDrawResultRow {
    return {
      id: row.id,
      title: row.slotKey,
      subtitle: row.slotKey,
      meta: row.occurredAt,
      logoUrl: this.lotteryAsset(row.slotKey),
      logoAlt: row.slotKey,
      slotKey: row.slotKey,
      numbers: this.resultNumbers(row),
      statusLabel: consoleDrawResultStatusLabel(row.status),
      statusTone: this.statusTone(row.status),
      qualityLabel: consoleDrawResultQualityLabel(row.quality),
      qualityTone: this.qualityTone(row.quality),
      sourceLabel: row.source || '—',
      fetchedAtLabel: row.fetchedAt ?? '—',
      actions: this.resultActions(row),
    };
  }

  private resultActions(row: DrawResultOpsResponse) {
    const actions = [];
    if (this.canConfirmResults() && row.status === 'PROVISIONAL') {
      actions.push({
        id: 'confirm',
        label: 'Confirmer',
        icon: 'check_circle',
        tone: 'primary' as const,
      });
    }
    if (this.canOverrideResults()) {
      actions.push({
        id: 'override',
        label: 'Override',
        icon: 'edit',
        variant: 'icon' as const,
      });
    }
    return actions;
  }

  private resultNumbers(row: DrawResultOpsResponse): string[] {
    const lots = this.haitiLots(row);
    return [lots.lot1, lots.lot2, lots.lot3, lots.lot4]
      .map(value => typeof value === 'string' || typeof value === 'number' ? String(value).trim() : '')
      .filter(Boolean);
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
