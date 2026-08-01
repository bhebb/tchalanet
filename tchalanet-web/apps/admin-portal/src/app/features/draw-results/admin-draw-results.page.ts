import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatNativeDateModule } from '@angular/material/core';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { TranslateService } from '@ngx-translate/core';
import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';

import { TchLoading, TchErrorPanel } from '@tch/ui/components';
import { RuntimeSettingsStore } from '@tch/shared-config';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { AdminPageShellComponent } from '@tch/ui/console';
import { AdminRefreshButtonComponent } from '@tch/ui/console';
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
  AdminDrawResultsApi,
  DrawResultView,
  DrawResultStatus,
  DrawResultQuality,
} from './data-access/admin-draw-results-api.service';
import {
  generatedDrawLocalDateLabel,
  generatedDrawLocalTimeLabel,
  generatedDrawTenantDateTimeLabel,
  generatedDrawTimezoneShortLabel,
  generatedDrawTimeWithZoneLabel,
} from '../draws/data-access/admin-generated-draws.models';

const RESULT_STATUS_OPTIONS: Array<{ value: DrawResultStatus | ''; label: string }> = [
  { value: '', label: 'Tous les statuts' },
  { value: 'PROVISIONAL', label: consoleDrawResultStatusLabel('PROVISIONAL') },
  { value: 'CONFIRMED', label: consoleDrawResultStatusLabel('CONFIRMED') },
  { value: 'OVERRIDDEN', label: consoleDrawResultStatusLabel('OVERRIDDEN') },
  { value: 'ERROR', label: consoleDrawResultStatusLabel('ERROR') },
];

const RESULT_QUALITY_OPTIONS: Array<{ value: DrawResultQuality | ''; label: string }> = [
  { value: '', label: 'Toutes qualités' },
  { value: 'COMPLETE', label: consoleDrawResultQualityLabel('COMPLETE') },
  { value: 'SUSPECT', label: consoleDrawResultQualityLabel('SUSPECT') },
  { value: 'INVALID', label: consoleDrawResultQualityLabel('INVALID') },
];

@Component({
  selector: 'tch-admin-draw-results-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminPageShellComponent,
    AdminRefreshButtonComponent,
    AdminEmptyStateComponent,
    AdminCrudShellComponent,
    AdminDataToolbarComponent,
    ConsoleDrawResultsTableComponent,
    FormsModule,
    TchLoading,
    TchErrorPanel,
    MatButtonModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatNativeDateModule,
    MatPaginatorModule,
    MatSelectModule,
  ],
  templateUrl: './admin-draw-results.page.html',
  styleUrls: ['./admin-draw-results.page.scss'],
})
export class AdminDrawResultsPage implements OnInit {
  private readonly api = inject(AdminDrawResultsApi);
  private readonly translate = inject(TranslateService);
  private readonly runtimeSettings = inject(RuntimeSettingsStore);
  private readonly router = inject(Router);

  readonly loading = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly filtersExpanded = signal(false);
  readonly page = signal<{
    items: DrawResultView[];
    totalElements: number;
    page: number;
    size: number;
  } | null>(null);
  readonly slotKeyFilter = signal('');
  readonly statusFilter = signal<DrawResultStatus | ''>('');
  readonly qualityFilter = signal<DrawResultQuality | ''>('');
  readonly fromFilter = signal('');
  readonly toFilter = signal('');
  readonly pageIndex = signal(0);
  readonly pageSize = signal(20);
  readonly statusOptions = RESULT_STATUS_OPTIONS;
  readonly qualityOptions = RESULT_QUALITY_OPTIONS;
  readonly fromDateValue = computed(() => this.fromFilter() ? isoDateToLocalDate(this.fromFilter()) : null);
  readonly toDateValue = computed(() => this.toFilter() ? isoDateToLocalDate(this.toFilter()) : null);
  readonly rows = computed<readonly ConsoleDrawResultRow[]>(() =>
    (this.page()?.items ?? []).map(row => this.toConsoleRow(row)),
  );

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.list({
      slotKey: this.slotKeyFilter() || undefined,
      status: this.statusFilter() || undefined,
      quality: this.qualityFilter() || undefined,
      from: this.fromFilter() || undefined,
      to: this.toFilter() || undefined,
      page: this.pageIndex(),
      size: this.pageSize(),
      sort: 'occurredAt,DESC',
    }, { suppressShellFeedback: true }).subscribe({
      next: p => { this.page.set(p); this.loading.set(false); },
      error: (err: unknown) => {
        this.error.set(this.errorViewModel(err));
        this.loading.set(false);
      },
    });
  }

  onPage(e: PageEvent): void {
    this.pageIndex.set(e.pageIndex);
    this.pageSize.set(e.pageSize);
    this.load();
  }

  onSlotSearch(value: string): void {
    this.slotKeyFilter.set(value.trim());
    this.pageIndex.set(0);
    this.load();
  }

  onStatusFilter(status: DrawResultStatus | ''): void {
    this.statusFilter.set(status);
    this.pageIndex.set(0);
    this.load();
  }

  onQualityFilter(quality: DrawResultQuality | ''): void {
    this.qualityFilter.set(quality);
    this.pageIndex.set(0);
    this.load();
  }

  onFromFilter(value: string): void {
    this.fromFilter.set(value);
    this.pageIndex.set(0);
    this.load();
  }

  onToFilter(value: string): void {
    this.toFilter.set(value);
    this.pageIndex.set(0);
    this.load();
  }

  onFromDatePicker(value: Date | null): void {
    this.onFromFilter(value ? toIsoDate(value) : '');
  }

  onToDatePicker(value: Date | null): void {
    this.onToFilter(value ? toIsoDate(value) : '');
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

  onResultAction(event: ConsoleDrawResultActionEvent): void {
    if (event.action.id === 'detail') {
      void this.router.navigate(['/app/admin/draws/results', event.row.id]);
    }
  }

  statusTone(status: DrawResultStatus): AdminStatusTone {
    return consoleDrawResultStatusTone(status);
  }

  qualityTone(quality: DrawResultQuality): AdminStatusTone {
    return consoleDrawResultQualityTone(quality);
  }

  statusLabel(status: DrawResultStatus): string {
    return consoleDrawResultStatusLabel(status);
  }

  qualityLabel(quality: DrawResultQuality): string {
    return consoleDrawResultQualityLabel(quality);
  }

  sourceLabel(row: DrawResultView): string {
    const source = row.source || this.payloadText(row, 'source') || this.payloadText(row, 'mode');
    switch (source) {
      case 'MANUAL': return 'Manuel';
      case 'ADMIN_OVERRIDE':
      case 'OVERRIDE': return 'Correction admin';
      case 'EXTERNAL':
      case 'US_LOTTERY':
      case 'NY_OPEN_DATA':
      case 'FL_APIM': return 'Provider';
      case 'AUTO':
      case 'SYSTEM': return 'Système';
      default: return source ? this.titleCase(source) : '—';
    }
  }

  providerCode(row: DrawResultView): string {
    return row.provider?.trim() || this.payloadText(row, 'provider') || '—';
  }

  slotCode(row: DrawResultView): string {
    return row.slotKey?.trim() ||
      this.payloadText(row, 'slot_key') ||
      this.payloadText(row, 'slotKey') ||
      this.payloadText(row, 'result_slot_key') ||
      this.payloadText(row, 'resultSlotKey') ||
      '—';
  }

  resultNumbers(row: DrawResultView): Array<string | null> {
    for (const payload of [row.haitiResult, row.sourceResult, row.rawPayload]) {
      const lots = this.haitiLots(payload);
      if (lots) return lots;
    }

    return row.numbers?.map(n => String(n).trim()).filter(Boolean) ?? [];
  }

  private haitiLots(payload: Record<string, unknown> | null | undefined): Array<string | null> | null {
    if (!payload) return null;

    const values = ['lot1', 'lot2', 'lot3', 'lot4'].map(key => {
      const value = payload[key];
      return typeof value === 'string' || typeof value === 'number' ? String(value).trim() || null : null;
    });

    return values.some(Boolean) ? values : null;
  }

  private payloadText(row: DrawResultView, key: string): string | null {
    const payloads = [row.haitiResult, row.sourceResult, row.rawPayload];
    for (const payload of payloads) {
      const value = payload?.[key];
      if (typeof value === 'string' && value.trim()) return value.trim();
      if (typeof value === 'number') return String(value);
    }
    return null;
  }

  providerDateLabel(row: DrawResultView): string {
    if (!row.occurredAt) return row.drawDate ?? row.resultDate ?? '—';
    return generatedDrawLocalDateLabel(row.occurredAt, this.providerTimezone(row)) ??
      row.drawDate ??
      row.resultDate ??
      '—';
  }

  providerTimeLabel(row: DrawResultView): string | undefined {
    if (!row.occurredAt) return undefined;
    return generatedDrawTimeWithZoneLabel(row.occurredAt, this.providerTimezone(row)) ?? '—';
  }

  providerTimeValue(row: DrawResultView): string | undefined {
    if (!row.occurredAt) return undefined;
    return generatedDrawLocalTimeLabel(row.occurredAt, this.providerTimezone(row)) ?? undefined;
  }

  localDateLabel(row: DrawResultView, providerDate: string): string | undefined {
    if (!row.occurredAt) return undefined;
    const localDate = generatedDrawLocalDateLabel(row.occurredAt, this.tenantTimezone());
    return localDate && localDate !== providerDate ? localDate : undefined;
  }

  localTimeLabel(row: DrawResultView): string | undefined {
    if (!row.occurredAt) return undefined;
    return generatedDrawTimeWithZoneLabel(row.occurredAt, this.tenantTimezone()) ?? undefined;
  }

  localTimeValue(row: DrawResultView): string | undefined {
    if (!row.occurredAt) return undefined;
    return generatedDrawLocalTimeLabel(row.occurredAt, this.tenantTimezone()) ?? undefined;
  }

  tenantTimestamp(value: string | null | undefined): string {
    return generatedDrawTenantDateTimeLabel(value, this.tenantTimezone());
  }

  appliedLabel(row: DrawResultView): string {
    return row.appliedAt ? this.tenantTimestamp(row.appliedAt) : 'Non appliqué';
  }

  tenantTimezoneLabel(): string {
    const timezone = this.tenantTimezone();
    return `${timezone} (${generatedDrawTimezoneShortLabel(timezone)})`;
  }

  private tenantTimezone(): string {
    const value = this.runtimeSettings.settings().values['app.timezone'];
    return typeof value === 'string' && value.trim() ? value : 'America/Port-au-Prince';
  }

  private providerTimezone(row: DrawResultView): string {
    if (row.timezone?.trim()) return row.timezone;
    const providerCode = this.providerCode(row);
    switch (providerCode) {
      case 'CA': return 'America/Los_Angeles';
      case 'IL':
      case 'MO':
      case 'TN':
      case 'TX':
        return 'America/Chicago';
      default:
        return 'America/New_York';
    }
  }

  private titleCase(value: string): string {
    return value
      .toLowerCase()
      .split(/[\s_-]+/)
      .filter(Boolean)
      .map(part => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ');
  }

  private errorViewModel(err: unknown): ErrorViewModel {
    const problem = (err as { error?: ProblemDetail })?.error;
    if (problem) {
      const normalized = webAppErrorFromProblemDetail(problem, 'admin.drawResults.list', 'page');
      const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
      return toErrorViewModel(normalized, copy);
    }

    return {
      title: this.translate.instant('common.errors.fallback.title'),
      message: this.translate.instant('common.errors.fallback.message'),
      severity: 'error',
    };
  }

  private toConsoleRow(row: DrawResultView): ConsoleDrawResultRow {
    const providerDateLabel = this.providerDateLabel(row);
    const providerTimeLabel = this.providerTimeLabel(row);
    const localDateLabel = this.localDateLabel(row, providerDateLabel);
    const localTimeLabel = this.localTimeLabel(row);
    return consoleDrawResultRowViewModel({
      id: row.id,
      identityInput: {
        providerCode: row.provider ?? this.payloadText(row, 'provider'),
        channelCode: row.channelCode ?? this.payloadText(row, 'channelCode') ?? this.payloadText(row, 'channel_code'),
        channelName: row.channelName,
        slotKey: this.slotCode(row),
        slotLabel: row.slotLabel,
        officialDateLabel: providerDateLabel,
        officialTimeLabel: this.providerTimeValue(row),
        officialTimezoneLabel: generatedDrawTimezoneShortLabel(this.providerTimezone(row)),
        localDateLabel,
        localTimeLabel: this.localTimeValue(row),
        localTimezoneLabel: generatedDrawTimezoneShortLabel(this.tenantTimezone()),
      },
      subtitle: this.slotCode(row),
      meta: providerTimeLabel ? `${providerDateLabel} · ${providerTimeLabel}` : providerDateLabel,
      slotKey: this.slotCode(row),
      numbers: this.resultNumbers(row),
      statusLabel: this.statusLabel(row.status),
      statusTone: this.statusTone(row.status),
      qualityLabel: this.qualityLabel(row.quality),
      qualityTone: this.qualityTone(row.quality),
      sourceLabel: this.sourceLabel(row),
      occurredDateLabel: providerDateLabel,
      providerDateLabel,
      providerTimeLabel,
      localDateLabel,
      localTimeLabel,
      fetchedAtLabel: this.tenantTimestamp(row.fetchedAt),
      appliedAtLabel: row.appliedAt ? this.tenantTimestamp(row.appliedAt) : undefined,
      actions: [
        {
          id: 'detail',
          label: 'Voir détail',
          icon: 'open_in_new',
          variant: 'button',
        },
      ],
    });
  }
}

function isoDateToLocalDate(value: string): Date {
  const [year, month, day] = value.split('-').map(Number);
  return new Date(year, month - 1, day);
}

function toIsoDate(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}
