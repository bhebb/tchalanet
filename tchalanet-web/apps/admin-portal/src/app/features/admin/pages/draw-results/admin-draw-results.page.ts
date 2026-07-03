import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { TranslateService } from '@ngx-translate/core';
import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';

import { TchLoading, TchErrorPanel } from '@tch/ui/components';
import { RuntimeSettingsStore } from '@tch/shared-config';
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
} from '@tch/web/console';
import {
  AdminDrawResultsApi,
  DrawResultView,
  DrawResultStatus,
  DrawResultQuality,
} from '../../data-access/admin-draw-results-api.service';
import { lotteryLogoForSlot, lotteryProviderCodeFromSlot } from '../../../../shared/lottery/lottery-assets';
import {
  generatedDrawProviderAndTenantTimeLabel,
  generatedDrawTenantDateTimeLabel,
  generatedDrawTimezoneShortLabel,
} from '../../draws/data-access/admin-generated-draws.models';

@Component({
  selector: 'tch-admin-draw-results-page',
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
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
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
    switch (status) {
      case 'CONFIRMED':
      case 'APPLIED': return 'success';
      case 'PROVISIONAL':
      case 'CORRECTED': return 'warning';
      case 'ERROR':
      case 'VOIDED': return 'danger';
      default: return 'neutral';
    }
  }

  qualityTone(quality: DrawResultQuality): AdminStatusTone {
    switch (quality) {
      case 'COMPLETE':
      case 'OFFICIAL': return 'success';
      case 'SUSPECT':
      case 'MANUAL': return 'warning';
      case 'ESTIMATED': return 'warning';
      case 'INVALID': return 'danger';
      default: return 'neutral';
    }
  }

  statusLabel(status: DrawResultStatus): string {
    switch (status) {
      case 'CONFIRMED': return 'Confirmé';
      case 'PROVISIONAL': return 'Provisoire';
      case 'OVERRIDDEN': return 'Remplacé';
      case 'ERROR': return 'Erreur';
      case 'APPLIED': return 'Appliqué';
      case 'CORRECTED': return 'Corrigé';
      case 'VOIDED': return 'Annulé';
      case 'PENDING': return 'En attente';
    }
  }

  qualityLabel(quality: DrawResultQuality): string {
    switch (quality) {
      case 'COMPLETE': return 'Complet';
      case 'SUSPECT': return 'À vérifier';
      case 'INVALID': return 'Invalide';
      case 'OFFICIAL': return 'Officiel';
      case 'MANUAL': return 'Manuel';
      case 'ESTIMATED': return 'Estimé';
      case 'UNKNOWN': return 'Inconnu';
    }
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

  providerLogo(row: DrawResultView): string | null {
    const slotCode = this.slotCode(row);
    return lotteryLogoForSlot(slotCode === '—' ? row.provider : slotCode);
  }

  providerCode(row: DrawResultView): string {
    const slotCode = this.slotCode(row);
    return (
      lotteryProviderCodeFromSlot(slotCode === '—' ? null : slotCode)?.toUpperCase() ??
      row.provider?.toUpperCase() ??
      this.payloadText(row, 'provider')?.toUpperCase() ??
      row.channelCode?.toUpperCase() ??
      '—'
    );
  }

  providerLabel(row: DrawResultView): string {
    const providerCode = this.providerCode(row);
    return row.channelName?.trim() || PROVIDER_LABELS[providerCode] || providerCode;
  }

  drawTitle(row: DrawResultView): string {
    const explicit = row.slotLabel?.trim();
    if (explicit && explicit !== row.slotKey) return explicit;

    const provider = this.providerLabel(row);
    const slot = this.slotName(row);
    if (provider === '—') return slot || 'Tirage';
    return slot ? `${provider} · ${slot}` : provider;
  }

  slotCode(row: DrawResultView): string {
    return row.slotKey?.trim() ||
      this.payloadText(row, 'slot_key') ||
      this.payloadText(row, 'slotKey') ||
      this.payloadText(row, 'result_slot_key') ||
      this.payloadText(row, 'resultSlotKey') ||
      '—';
  }

  slotName(row: DrawResultView): string {
    const slotKey = this.slotCode(row);
    if (!slotKey || slotKey === '—') return '';
    const parts = slotKey.split(/[_-]/).filter(Boolean);
    const rawSlot = parts[parts.length - 1];
    if (!rawSlot) return '';
    return SLOT_LABELS[rawSlot.toUpperCase()] || this.titleCase(rawSlot);
  }

  resultNumbers(row: DrawResultView): string[] {
    const fromResponse = row.numbers?.map(n => String(n).trim()).filter(Boolean) ?? [];
    if (fromResponse.length > 0) return fromResponse;

    const payload = row.haitiResult ?? row.sourceResult ?? row.rawPayload;
    if (!payload) return [];
    return ['lot1', 'lot2', 'lot3', 'lot4', 'pick3', 'pick4']
      .map(key => payload[key])
      .map(value => typeof value === 'string' || typeof value === 'number' ? String(value).trim() : '')
      .filter(Boolean);
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

  resultDrawTime(row: DrawResultView): string {
    if (!row.occurredAt) return '—';
    return generatedDrawProviderAndTenantTimeLabel(
      row.occurredAt,
      this.providerTimezone(row),
      this.tenantTimezone(),
    ) ?? '—';
  }

  resultDrawDate(row: DrawResultView): string {
    return row.drawDate ?? row.resultDate ?? '—';
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
    const drawDate = this.resultDrawDate(row);
    const occurredTime = row.occurredAt ? this.resultDrawTime(row) : null;
    return {
      id: row.id,
      title: this.drawTitle(row),
      subtitle: this.slotCode(row),
      meta: occurredTime ? `${drawDate} · ${occurredTime}` : drawDate,
      logoUrl: this.providerLogo(row),
      logoAlt: this.providerLabel(row),
      slotKey: this.slotCode(row),
      numbers: this.resultNumbers(row),
      statusLabel: this.statusLabel(row.status),
      statusTone: this.statusTone(row.status),
      qualityLabel: this.qualityLabel(row.quality),
      qualityTone: this.qualityTone(row.quality),
      sourceLabel: this.sourceLabel(row),
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
    };
  }
}

const PROVIDER_LABELS: Record<string, string> = {
  CA: 'California',
  FL: 'Florida',
  GA: 'Georgia',
  IL: 'Illinois',
  MI: 'Michigan',
  MO: 'Missouri',
  NJ: 'New Jersey',
  NY: 'New York',
  OH: 'Ohio',
  PA: 'Pennsylvania',
  TN: 'Tennessee',
  TX: 'Texas',
};

const SLOT_LABELS: Record<string, string> = {
  DAY: 'Day',
  EVE: 'Evening',
  EVENING: 'Evening',
  LATE: 'Late',
  MID: 'Midday',
  MIDDAY: 'Midday',
  MORNING: 'Morning',
};
