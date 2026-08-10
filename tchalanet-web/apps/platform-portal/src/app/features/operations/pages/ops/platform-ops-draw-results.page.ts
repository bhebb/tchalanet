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
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

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
  consoleDrawResultRowViewModel,
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
import {
  CatalogResultSlotView,
  PlatformCatalogApi,
} from '../../../catalog/data-access/platform-catalog-api.service';
import { FetchResultsDialog } from '../../components/dialogs/fetch-results.dialog';
import { ManualResultDialog } from '../../components/dialogs/manual-result.dialog';
import { OverrideResultDialog } from '../../components/dialogs/override-result.dialog';
import { SourceResultDialog } from '../../components/dialogs/source-result.dialog';

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
    TranslatePipe,
  ],
  templateUrl: './platform-ops-draw-results.page.html',
  styleUrls: ['./platform-ops-draw-results.page.scss'],
})
export class PlatformOpsDrawResultsPage implements OnInit {
  private readonly api = inject(PlatformOpsApi);
  private readonly catalogApi = inject(PlatformCatalogApi);
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
  readonly canEnterManualResults = computed(() =>
    this.access.can(CONSOLE_DRAW_RESULT_ACCESS.manual),
  );
  readonly canConfirmResults = computed(() => this.access.can(CONSOLE_DRAW_RESULT_ACCESS.confirm));
  readonly canOverrideResults = computed(() =>
    this.access.can(CONSOLE_DRAW_RESULT_ACCESS.override),
  );
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
  readonly resultSlotsByKey = signal<Record<string, CatalogResultSlotView>>({});
  readonly statusOptions = RESULT_STATUS_OPTIONS;
  readonly qualityOptions = RESULT_QUALITY_OPTIONS;
  readonly rows = computed<readonly ConsoleDrawResultRow[]>(() =>
    (this.page()?.items ?? []).map(row => this.toConsoleRow(row)),
  );

  ngOnInit(): void {
    this.loadResultSlots();
    this.load();
  }

  loadResultSlots(): void {
    this.catalogApi.listResultSlots().subscribe({
      next: slots => {
        this.resultSlotsByKey.set(
          Object.fromEntries(slots.map(slot => [normalizeSlotKey(slot.slotKey), slot])),
        );
      },
      error: () => {
        this.resultSlotsByKey.set({});
      },
    });
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api
      .listDrawResults(
        {
          slotKey: this.slotKeyFilter() || undefined,
          status: this.statusFilter() || undefined,
          quality: this.qualityFilter() || undefined,
          from: this.fromFilter() || undefined,
          to: this.toFilter() || undefined,
          page: this.pageIndex(),
          size: this.pageSize(),
          sort: 'occurredAt,DESC',
        },
        { suppressShellFeedback: true },
      )
      .subscribe({
        next: v => {
          this.page.set(v);
          this.loading.set(false);
        },
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

  onSlotSearch(v: string): void {
    this.slotKeyFilter.set(v.trim());
    this.pageIndex.set(0);
    this.load();
  }
  onStatusChange(v: string): void {
    this.statusFilter.set(v);
    this.pageIndex.set(0);
    this.load();
  }
  onQualityChange(v: OpsDrawResultQuality | ''): void {
    this.qualityFilter.set(v);
    this.pageIndex.set(0);
    this.load();
  }
  onFromChange(v: string): void {
    this.fromFilter.set(v);
    this.pageIndex.set(0);
    this.load();
  }
  onToChange(v: string): void {
    this.toFilter.set(v);
    this.pageIndex.set(0);
    this.load();
  }

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

  openSource(rowVm: ConsoleDrawResultRow): void {
    const row = this.page()?.items.find(item => item.id === rowVm.id);
    if (!row) return;

    this.dialog.open(SourceResultDialog, {
      data: { row },
      width: '760px',
      maxWidth: 'calc(100vw - 32px)',
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

  haitiLots(row: DrawResultOpsResponse): HaitiLots {
    const value = row.haitiResult;
    if (!value || typeof value !== 'object') return {};
    return value as HaitiLots;
  }

  private toConsoleRow(row: DrawResultOpsResponse): ConsoleDrawResultRow {
    const slot = this.resultSlot(row.slotKey);
    const slotKey = slot?.slotKey ?? row.slotKey;
    const providerTimezone = slot?.timezone?.trim() || undefined;
    const providerDateLabel = this.formatDate(row.occurredAt, providerTimezone);
    const localDateLabel = this.formatLocalDate(row.occurredAt, providerTimezone);
    return consoleDrawResultRowViewModel({
      id: row.id,
      identityInput: {
        providerCode: slot?.provider,
        slotKey,
        officialDateLabel: providerDateLabel,
        officialTimeLabel: this.formatProviderTimeValue(row.occurredAt, providerTimezone, slot?.drawTime),
        officialTimezoneLabel: providerTimezone,
        localDateLabel,
        localTimeLabel: this.formatLocalTimeValue(row.occurredAt),
        fallbackTitle: slotKey,
      },
      labelKey: undefined,
      subtitle: slotKey,
      meta: row.occurredAt,
      slotKey,
      numbers: this.resultNumbers(row),
      statusLabel: consoleDrawResultStatusLabel(row.status),
      statusTone: this.statusTone(row.status),
      qualityLabel: consoleDrawResultQualityLabel(row.quality),
      qualityTone: this.qualityTone(row.quality),
      sourceLabel: row.source || '—',
      sourceFlags: this.sourceFlags(row),
      hasSourceDetails: this.hasSourceDetails(row.sourceResult),
      occurredDateLabel: providerDateLabel,
      providerDateLabel,
      providerTimeLabel: this.formatProviderTime(row.occurredAt, providerTimezone, slot?.drawTime),
      localDateLabel,
      localTimeLabel: this.formatLocalTime(row.occurredAt, providerTimezone),
      fetchedAtLabel: this.formatFetchedAt(row.fetchedAt),
      actions: this.resultActions(row),
    });
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

  private resultNumbers(row: DrawResultOpsResponse): Array<string | null> {
    const lots = this.haitiLots(row);
    const values = [lots.lot1, lots.lot2, lots.lot3, lots.lot4].map(value =>
      typeof value === 'string' || typeof value === 'number' ? String(value).trim() || null : null,
    );

    return values.some(Boolean) ? values : [];
  }

  private formatDate(value: string | null | undefined, timeZone?: string): string {
    const date = this.toDate(value);
    return date
      ? new Intl.DateTimeFormat('fr-CA', { dateStyle: 'medium', timeZone }).format(date)
      : '—';
  }

  private formatProviderTime(
    value: string | null | undefined,
    providerTimezone?: string,
    slotDrawTime?: string | null,
  ): string | undefined {
    return this.withTimezone(this.formatProviderTimeValue(value, providerTimezone, slotDrawTime), providerTimezone);
  }

  private formatProviderTimeValue(
    value: string | null | undefined,
    providerTimezone?: string,
    slotDrawTime?: string | null,
  ): string | undefined {
    const drawTime = this.formatSlotDrawTime(slotDrawTime);
    return drawTime ?? (providerTimezone ? this.formatTime(value, providerTimezone) : undefined);
  }

  private formatLocalTime(
    value: string | null | undefined,
    _providerTimezone?: string,
  ): string | undefined {
    const time = this.formatLocalTimeValue(value);
    if (!time) return undefined;
    return this.withTimezone(time, this.localTimezone());
  }

  private formatLocalTimeValue(value: string | null | undefined): string | undefined {
    return this.formatTime(value);
  }

  private formatLocalDate(
    value: string | null | undefined,
    providerTimezone?: string,
  ): string | undefined {
    const providerDate = this.dateKey(value, providerTimezone);
    const localDate = this.dateKey(value);
    if (!providerDate || !localDate || providerDate === localDate) return undefined;
    return this.formatDate(value);
  }

  private formatFetchedAt(value: string | null | undefined): string | undefined {
    const date = this.toDate(value);
    if (!date) return undefined;

    const dateLabel = this.formatDate(value);
    const timeLabel = this.withTimezone(this.formatTime(value), this.localTimezone());
    return timeLabel ? `${dateLabel} · ${timeLabel}` : dateLabel;
  }

  private formatTime(value: string | null | undefined, timeZone?: string): string | undefined {
    const date = this.toDate(value);
    if (!date) return undefined;
    return new Intl.DateTimeFormat('fr-CA', {
      hour: '2-digit',
      minute: '2-digit',
      timeZone,
    }).format(date);
  }

  private formatSlotDrawTime(value: string | null | undefined): string | undefined {
    const normalized = value?.trim().match(/^(\d{1,2}):(\d{2})/);
    if (!normalized) return undefined;
    return `${normalized[1].padStart(2, '0')} h ${normalized[2]}`;
  }

  private withTimezone(
    value: string | undefined,
    timezone: string | undefined,
  ): string | undefined {
    if (!value) return undefined;
    return timezone ? `${value} (${timezone})` : value;
  }

  private localTimezone(): string | undefined {
    return Intl.DateTimeFormat().resolvedOptions().timeZone;
  }

  private sourceFlags(row: DrawResultOpsResponse): string[] {
    if (row.source !== 'EXTERNAL') return [];

    return uniqueStrings([
      ...this.sourceResultFlags(row.sourceResult, 'pick3', 'P3'),
      ...this.sourceResultFlags(row.sourceResult, 'pick4', 'P4'),
    ]);
  }

  private sourceResultFlags(value: unknown, pickKey: 'pick3' | 'pick4', label: string): string[] {
    const root = asRecord(value);
    const pick = asRecord(root?.[pickKey]);
    const flags = asRecord(pick?.['source_flags']);
    if (!flags) return [];

    const result: string[] = [];
    const origin = stringValue(flags['origin']);
    if (origin) result.push(`${label} origin: ${origin}`);

    const sourceHash = stringValue(flags['sourceHash']);
    if (sourceHash) result.push(`${label} hash: ${sourceHash.slice(0, 10)}`);

    const url = stringValue(flags['url']);
    if (url) result.push(`${label} url: ${compactUrl(url)}`);

    const metadata = asRecord(flags['metadata']);
    if (metadata) {
      for (const [key, value] of Object.entries(metadata)) {
        const text = stringValue(value);
        if (text) result.push(`${label} ${key}: ${truncate(text, 48)}`);
      }
    }

    return result;
  }

  private hasSourceDetails(value: unknown): boolean {
    const root = asRecord(value);
    return Boolean(asRecord(root?.['pick3']) || asRecord(root?.['pick4']));
  }

  private resultSlot(slotKey: string): CatalogResultSlotView | undefined {
    return this.resultSlotsByKey()[normalizeSlotKey(slotKey)];
  }

  private dateKey(value: string | null | undefined, timeZone?: string): string | null {
    const date = this.toDate(value);
    if (!date) return null;
    return new Intl.DateTimeFormat('en-CA', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      timeZone,
    }).format(date);
  }

  private toDate(value: string | null | undefined): Date | null {
    if (!value) return null;
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? null : date;
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

function normalizeSlotKey(slotKey: string): string {
  return slotKey.trim().toUpperCase();
}

function asRecord(value: unknown): Record<string, unknown> | null {
  return value && typeof value === 'object' && !Array.isArray(value)
    ? (value as Record<string, unknown>)
    : null;
}

function stringValue(value: unknown): string {
  return typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean'
    ? String(value).trim()
    : '';
}

function compactUrl(value: string): string {
  try {
    const url = new URL(value);
    return truncate(`${url.hostname}${url.pathname}`, 48);
  } catch {
    return truncate(value, 48);
  }
}

function truncate(value: string, maxLength: number): string {
  return value.length > maxLength ? `${value.slice(0, maxLength - 1)}…` : value;
}

function uniqueStrings(values: readonly string[]): string[] {
  return [...new Set(values.filter(Boolean))];
}
