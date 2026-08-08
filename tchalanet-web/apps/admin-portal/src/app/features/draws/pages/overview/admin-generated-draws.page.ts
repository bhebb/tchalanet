import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatNativeDateModule } from '@angular/material/core';

import { TCH_DEFAULT_PAGE_SIZE } from '@tch/api';
import { AccessService } from '@tch/core/auth';
import { AdminListStatusOption, AdminListSurface, TchSectionError } from '@tch/ui/components';
import {
  TchAsyncReadyDirective,
  TchAsyncViewComponent,
  resourceErrorVm,
  tchMutation,
} from '@tch/web/async';
import { AdminPageShellComponent } from '@tch/ui/console';
import { AdminRefreshButtonComponent } from '@tch/ui/console';
import { AdminEmptyStateComponent } from '@tch/ui/console';

import {
  DrawResultDrawerComponent,
  DrawResultDrawerState,
} from '../../components/draw-result-drawer/draw-result-drawer.component';
import { AdminGeneratedDrawsApiService } from '../../data-access/admin-generated-draws-api.service';
import {
  DrawLifecycleAction,
  GeneratedDrawView,
  GeneratedDrawGroup,
  DatePreset,
  DrawStatusFilter,
  SaveDrawResultRequest,
  shiftIsoDate,
  tenantTodayIsoDate,
} from '../../data-access/admin-generated-draws.models';
import {
  GeneratedDrawsSummaryComponent,
  GeneratedDrawsSummaryKpi,
} from '../../components/generated-draws-summary/generated-draws-summary.component';
import { GeneratedDrawsTableComponent } from '../../components/generated-draws-table/generated-draws-table.component';
import { AdminDrawLifecycleDialog } from './dialogs/admin-draw-lifecycle.dialog';
import {
  CONSOLE_DRAW_RESULT_ACCESS,
  consoleDrawResultStatusLabel,
  consoleDrawStatusLabel,
} from '@tch/web/console';
import { AdminDrawSalesMatrixApi } from '../../../draw-sales-matrix/data-access/admin-draw-sales-matrix-api.service';
import {
  DrawChannelFilterSelectComponent,
  DrawChannelFilterValue,
} from '../../../draw-sales-matrix/components/draw-channel-filter-select/draw-channel-filter-select.component';

const LIFECYCLE_LABELS: Record<DrawLifecycleAction, string> = {
  open: 'Ouvert',
  close: 'Fermé',
  cancel: 'Annulé',
  lock: 'Verrouillé',
  unlock: 'Déverrouillé',
  settle: 'Réglé',
  archive: 'Archivé',
};

interface LifecycleInput {
  readonly action: DrawLifecycleAction;
  readonly drawIds: readonly string[];
  readonly reason?: string;
}

@Component({
  selector: 'tch-admin-generated-draws-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatButtonModule,
    FormsModule,
    MatDatepickerModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatMenuModule,
    MatNativeDateModule,
    AdminPageShellComponent,
    AdminRefreshButtonComponent,
    AdminEmptyStateComponent,
    AdminListSurface,
    TchAsyncViewComponent,
    TchAsyncReadyDirective,
    TchSectionError,
    GeneratedDrawsSummaryComponent,
    GeneratedDrawsTableComponent,
    DrawResultDrawerComponent,
    DrawChannelFilterSelectComponent,
  ],
  templateUrl: './admin-generated-draws.page.html',
  styleUrls: ['./admin-generated-draws.page.scss'],
})
export class AdminGeneratedDrawsPage {
  private readonly api = inject(AdminGeneratedDrawsApiService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly dialog = inject(MatDialog);
  private readonly access = inject(AccessService);
  private readonly drawSalesMatrix = inject(AdminDrawSalesMatrixApi);

  /**
   * Today on the tenant's calendar — `businessDate` and the from/to filters are channel-local,
   * so the browser's own date would be a day ahead every evening in Haiti.
   */
  readonly today = computed(() => tenantTodayIsoDate(this.api.tenantTimezone()));
  private readonly yesterday = computed(() => shiftIsoDate(this.today(), -1));

  readonly statusFilters: { key: DrawStatusFilter; label: string }[] = [
    { key: 'all', label: 'Tous les statuts' },
    { key: 'SCHEDULED', label: consoleDrawStatusLabel('SCHEDULED') },
    { key: 'OPEN', label: consoleDrawStatusLabel('OPEN') },
    { key: 'LOCKED', label: consoleDrawStatusLabel('LOCKED') },
    { key: 'CLOSED', label: consoleDrawStatusLabel('CLOSED') },
    { key: 'RESULTED', label: consoleDrawStatusLabel('RESULTED') },
    { key: 'SETTLED', label: consoleDrawStatusLabel('SETTLED') },
    { key: 'CANCELLED', label: consoleDrawStatusLabel('CANCELLED') },
    { key: 'ARCHIVED', label: consoleDrawStatusLabel('ARCHIVED') },
    { key: 'PAST', label: 'Terminés / à traiter' },
    { key: 'NOT_DUE', label: consoleDrawResultStatusLabel('NOT_DUE') },
    { key: 'EXPECTED', label: consoleDrawResultStatusLabel('EXPECTED') },
    { key: 'MISSING', label: consoleDrawResultStatusLabel('MISSING') },
    { key: 'PROVISIONAL', label: consoleDrawResultStatusLabel('PROVISIONAL') },
    { key: 'CONFIRMED', label: consoleDrawResultStatusLabel('CONFIRMED') },
    { key: 'SOURCE_ERROR', label: consoleDrawResultStatusLabel('SOURCE_ERROR') },
  ];

  readonly datePresets: { key: DatePreset; label: string }[] = [
    { key: 'LAST_48H', label: '48 dernières heures' },
    { key: 'TODAY', label: "Aujourd'hui" },
    { key: 'TOMORROW', label: 'Demain' },
    { key: 'THIS_WEEK', label: 'Cette semaine' },
  ];

  // ── URL = source de vérité pour les filtres/pagination ──────────────────────
  private readonly qp = toSignal(this.route.queryParamMap, {
    initialValue: this.route.snapshot.queryParamMap,
  });
  readonly datePreset = computed<DatePreset>(() => datePresetFromQuery(this.qp().get('date')));
  readonly fromDate = computed(() => dateParam(this.qp().get('from'), this.yesterday()));
  readonly toDate = computed(() => dateParam(this.qp().get('to'), this.today()));
  readonly fromDateValue = computed(() => isoDateToLocalDate(this.fromDate()));
  readonly toDateValue = computed(() => isoDateToLocalDate(this.toDate()));
  readonly hasCustomDateRange = computed(() => this.qp().has('from') || this.qp().has('to'));
  readonly statusFilter = computed<DrawStatusFilter>(() =>
    statusFilterFromQuery(this.qp().get('status')),
  );
  readonly providerFilter = computed(() => providerParam(this.qp().get('provider')));
  readonly slotKeyFilter = computed(() => slotKeyParam(this.qp().get('slotKey')));
  readonly providerMatrix = this.drawSalesMatrix.getMatrixResource({ suppressShellFeedback: true });
  readonly searchQuery = computed(() => this.qp().get('q')?.trim() ?? '');
  readonly hasActiveFilters = computed(
    () =>
      this.hasCustomDateRange() ||
      this.datePreset() !== 'LAST_48H' ||
      this.statusFilter() !== 'all' ||
      !!this.providerFilter() ||
      !!this.slotKeyFilter() ||
      !!this.searchQuery(),
  );
  readonly statusFilterOptions = computed<readonly AdminListStatusOption[]>(() =>
    this.statusFilters
      .filter(filter => filter.key !== 'all')
      .map(filter => ({ value: filter.key, label: filter.label })),
  );
  readonly page = computed(() => numberParam(this.qp().get('page'), 0));

  // ── Lecture (resource créée par le client, statut filtré côté client) ───────
  // `q` is intentionally NOT part of this query: the backend has no text-search field for
  // /admin/draws (see AdminGeneratedDrawsApiService.filterDrawsByQuery), so including it here
  // would only trigger a wasted refetch on every keystroke for a param the server ignores.
  readonly draws = this.api.generatedDrawsResource(() => ({
    datePreset: this.datePreset(),
    from: this.hasCustomDateRange() ? this.fromDate() : null,
    to: this.hasCustomDateRange() ? this.toDate() : null,
    status: this.statusFilter(),
    page: this.page(),
  }));
  // KPI summary resource: exact period totals from the backend, never status-filtered.
  private readonly drawsKpi = this.api.generatedDrawsSummaryResource(() => ({
    datePreset: this.datePreset(),
    from: this.hasCustomDateRange() ? this.fromDate() : null,
    to: this.hasCustomDateRange() ? this.toDate() : null,
    today: this.today(),
  }));
  readonly kpiSummary = computed(() => this.drawsKpi.value());
  readonly drawsError = resourceErrorVm(this.draws, 'admin.generatedDraws.list');
  readonly allDraws = computed(() => {
    const statusFiltered = this.api.filterDrawsByStatus(
      this.draws.value()?.items ?? [],
      this.statusFilter(),
    );
    const channelFiltered = filterDrawsByChannel(
      statusFiltered,
      this.providerFilter(),
      this.slotKeyFilter(),
    );
    return this.api.filterDrawsByQuery(channelFiltered, this.searchQuery());
  });
  readonly totalElements = computed(() => this.draws.value()?.totalElements ?? 0);
  /**
   * The table's `hasNext`/`hasPrev` math needs the real page size the backend used, not a
   * guessed default — fall back to the shared default only before a page has loaded.
   */
  readonly pageSize = computed(() => this.draws.value()?.size ?? TCH_DEFAULT_PAGE_SIZE);
  readonly isEmpty = (): boolean => (this.draws.value()?.items?.length ?? 0) === 0;
  readonly pageTitle = computed(() => {
    switch (this.statusFilter()) {
      case 'OPEN': return 'Tirages ouverts';
      case 'PAST': return 'Tirages passés';
      case 'CLOSED': return 'Tirages fermés';
      case 'CONFIRMED': return 'Tirages confirmés';
      case 'EXPECTED_OR_MISSING': return 'Résultats attendus';
      default: return 'Tirages générés';
    }
  });
  readonly canEnterManualResults = computed(() =>
    this.access.can(CONSOLE_DRAW_RESULT_ACCESS.manual),
  );
  readonly canConfirmResults = computed(() => this.access.can(CONSOLE_DRAW_RESULT_ACCESS.confirm));
  readonly canOverrideResults = computed(() =>
    this.access.can(CONSOLE_DRAW_RESULT_ACCESS.override),
  );
  readonly canManageDrawLifecycle = computed(() =>
    this.access.can([{ role: 'SUPER_ADMIN' }, { permission: 'draw.lifecycle.manage' }]),
  );

  readonly groupedDraws = computed<GeneratedDrawGroup[]>(() => {
    const map = new Map<string, GeneratedDrawView[]>();
    for (const draw of this.allDraws()) {
      const drawsForDate = map.get(draw.businessDate) ?? [];
      drawsForDate.push(draw);
      map.set(draw.businessDate, drawsForDate);
    }
    // Most recent date first — an admin checking on draws cares about today before yesterday.
    return Array.from(map.entries())
      .sort(([a], [b]) => b.localeCompare(a))
      .map(([date, draws]) => ({ date, draws }));
  });

  // ── Drawer de résultat (UI locale) ──────────────────────────────────────────
  readonly selectedDraw = signal<GeneratedDrawView | null>(null);
  private readonly resultMessage = signal<string | null>(null);

  readonly saveResult = tchMutation<SaveDrawResultRequest, GeneratedDrawView>({
    run: req => this.api.saveDrawResult(req, { suppressShellFeedback: true }),
    source: 'admin.generatedDraws.result',
    onSuccess: (updated, input) => {
      this.selectedDraw.set(updated);
      this.resultMessage.set(
        input.mode === 'confirmed'
          ? 'Résultat confirmé et publié.'
          : 'Résultat enregistré en provisoire.',
      );
      this.reload();
    },
    onError: () => {
      // Keep the normalized mutation feedback; the drawer owns its presentation.
      this.resultMessage.set(null);
    },
  });

  readonly resultSaveState = computed<DrawResultDrawerState>(() => {
    if (this.saveResult.pending()) return 'saving';
    const fb = this.saveResult.feedback();
    return fb?.kind === 'success' ? 'success' : fb?.kind === 'error' ? 'error' : 'ready';
  });
  readonly resultSaveMessage = computed(
    () => this.resultMessage() ?? this.saveResult.feedback()?.vm?.message ?? null,
  );

  // ── Actions de cycle de vie (mutation + pending par ligne) ──────────────────
  readonly pendingDrawIds = signal<ReadonlySet<string>>(new Set());
  readonly lifecycleNotice = signal<string | null>(null);

  readonly lifecycle = tchMutation<LifecycleInput, GeneratedDrawView[]>({
    run: input =>
      this.api.lifecycleDraws(input.action, input.drawIds, input.reason, {
        suppressShellFeedback: true,
      }),
    source: 'admin.generatedDraws.lifecycle',
    onSuccess: (_result, input) => {
      this.pendingDrawIds.set(new Set());
      this.lifecycleNotice.set(
        input.drawIds.length > 1
          ? `${input.drawIds.length} tirages mis à jour avec succès.`
          : `Tirage ${LIFECYCLE_LABELS[input.action]} avec succès.`,
      );
      this.reload();
    },
    onError: () => {
      this.pendingDrawIds.set(new Set());
    },
  });

  // ── Filtres → URL ───────────────────────────────────────────────────────────
  onDatePreset(preset: DatePreset): void {
    this.navigate({
      date: preset === 'LAST_48H' ? null : preset,
      from: null,
      to: null,
      page: null,
    });
  }

  reload(): void {
    this.draws.reload();
    this.drawsKpi.reload();
  }

  onFromDate(value: string): void {
    this.navigate({ from: dateParam(value, this.yesterday()), date: null, page: null });
  }

  onToDate(value: string): void {
    this.navigate({ to: dateParam(value, this.today()), date: null, page: null });
  }

  onFromDatePicker(value: Date | null): void {
    if (value) this.onFromDate(toIsoDate(value));
  }

  onToDatePicker(value: Date | null): void {
    if (value) this.onToDate(toIsoDate(value));
  }

  onStatusFilter(status: DrawStatusFilter): void {
    this.navigate({ status: status === 'all' ? null : status, page: null });
  }

  onStatusFilterValue(status: string): void {
    this.onStatusFilter(statusFilterFromQuery(status));
  }

  onDrawChannelFilter(selection: DrawChannelFilterValue): void {
    this.navigate({
      provider: selection.provider || null,
      slotKey: selection.slotKey || null,
      page: null,
    });
  }

  // ── Cartes KPI → filtres ─────────────────────────────────────────────────────
  onKpiToday(): void {
    this.onDatePreset('TODAY');
  }

  onKpiSalesOpen(): void {
    this.onStatusFilter('OPEN');
  }

  onKpiExpected(): void {
    this.onStatusFilter('EXPECTED_OR_MISSING');
  }

  onKpiConfirmed(): void {
    this.onStatusFilter('CONFIRMED');
  }

  onKpiSelected(kpi: GeneratedDrawsSummaryKpi): void {
    switch (kpi) {
      case 'today':
        this.onKpiToday();
        break;
      case 'salesOpen':
        this.onKpiSalesOpen();
        break;
      case 'expected':
        this.onKpiExpected();
        break;
      case 'confirmed':
        this.onKpiConfirmed();
        break;
    }
  }

  onSearch(query: string): void {
    this.navigate({ q: query || null, page: null });
  }

  resetFilters(): void {
    this.navigate({
      date: null,
      from: null,
      to: null,
      status: null,
      provider: null,
      slotKey: null,
      q: null,
      page: null,
    });
  }

  onNextPage(): void {
    this.navigate({ page: this.page() + 1 });
  }

  onPrevPage(): void {
    const prev = Math.max(0, this.page() - 1);
    this.navigate({ page: prev > 0 ? prev : null });
  }

  // ── Drawer ──────────────────────────────────────────────────────────────────
  onEnterResult(draw: GeneratedDrawView): void {
    this.openResultDrawer(draw);
  }
  onViewResult(draw: GeneratedDrawView): void {
    this.openResultDrawer(draw);
  }
  onVerifySource(draw: GeneratedDrawView): void {
    this.openResultDrawer(draw);
  }

  onViewDetails(draw: GeneratedDrawView): void {
    this.router.navigate(['/app/admin/draws', draw.drawId]);
  }

  onDrawerClosed(): void {
    this.selectedDraw.set(null);
    this.resultMessage.set(null);
    this.saveResult.clearFeedback();
  }

  onResultSaveRequested(request: SaveDrawResultRequest): void {
    this.resultMessage.set(null);
    this.saveResult.execute(request);
  }

  canSaveProvisionalResult(draw: GeneratedDrawView): boolean {
    return this.canEnterManualResults() && this.hasNoResult(draw) && this.isManualResultDue(draw);
  }

  canSaveConfirmedResult(draw: GeneratedDrawView): boolean {
    return this.canConfirmResults() && this.hasNoResult(draw) && this.isManualResultDue(draw);
  }

  canOverrideResult(draw: GeneratedDrawView): boolean {
    return this.canOverrideResults() && this.hasResult(draw);
  }

  resultUnavailableReason(draw: GeneratedDrawView): string | null {
    if (this.hasResult(draw)) {
      return this.canOverrideResults()
        ? null
        : 'Le résultat existe déjà. Seul un super admin peut le corriger.';
    }
    if (!this.canEnterManualResults()) {
      return 'Vous n’avez pas l’autorisation de saisir un résultat manuel.';
    }
    if (!this.isManualResultDue(draw)) {
      return 'La saisie manuelle sera disponible 30 minutes après l’heure prévue du tirage.';
    }
    return null;
  }

  onConfigureDrawChannels(): void {
    this.router.navigate(['/app/admin/draws/channels']);
  }

  // ── Actions de cycle de vie ─────────────────────────────────────────────────
  onOpenDraw(draw: GeneratedDrawView): void {
    this.openLifecycleDialog(draw, 'open');
  }
  onCloseDraw(draw: GeneratedDrawView): void {
    this.openLifecycleDialog(draw, 'close');
  }
  onLockDraw(draw: GeneratedDrawView): void {
    this.openLifecycleDialog(draw, 'lock');
  }
  onUnlockDraw(draw: GeneratedDrawView): void {
    this.openLifecycleDialog(draw, 'unlock');
  }
  onCancelDraw(draw: GeneratedDrawView): void {
    this.openLifecycleDialog(draw, 'cancel');
  }
  onSettleDraw(draw: GeneratedDrawView): void {
    this.openLifecycleDialog(draw, 'settle');
  }
  onArchiveDraw(draw: GeneratedDrawView): void {
    this.openLifecycleDialog(draw, 'archive');
  }
  onBulkLifecycle(event: {
    action: DrawLifecycleAction;
    draws: readonly GeneratedDrawView[];
  }): void {
    this.openLifecycleDialog(event.draws, event.action);
  }

  private openLifecycleDialog(
    drawOrDraws: GeneratedDrawView | readonly GeneratedDrawView[],
    action: DrawLifecycleAction,
  ): void {
    const ref = this.dialog.open(AdminDrawLifecycleDialog, {
      data: {
        draw: Array.isArray(drawOrDraws) ? drawOrDraws[0] : drawOrDraws,
        draws: Array.isArray(drawOrDraws) ? drawOrDraws : undefined,
        action,
      },
      width: '420px',
    });

    ref.afterClosed().subscribe((result: { reason?: string } | null) => {
      if (result == null) return;
      const draws = Array.isArray(drawOrDraws) ? drawOrDraws : [drawOrDraws];
      const drawIds = draws.map(draw => draw.drawId);
      this.lifecycleNotice.set(null);
      this.pendingDrawIds.set(new Set(drawIds));
      this.lifecycle.execute({ action, drawIds, reason: result.reason });
    });
  }

  private navigate(params: Record<string, string | number | null>): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: params,
      queryParamsHandling: 'merge',
    });
  }

  private hasResult(draw: GeneratedDrawView): boolean {
    return (
      draw.resultStatus === 'PROVISIONAL' ||
      draw.resultStatus === 'CONFIRMED' ||
      (draw.numbers?.length ?? 0) > 0
    );
  }

  private hasNoResult(draw: GeneratedDrawView): boolean {
    return !this.hasResult(draw);
  }

  private isManualResultDue(draw: GeneratedDrawView): boolean {
    const scheduledAt = Date.parse(draw.scheduledAt);
    if (Number.isNaN(scheduledAt)) return false;
    return Date.now() >= scheduledAt + 30 * 60 * 1000;
  }

  private openResultDrawer(draw: GeneratedDrawView): void {
    this.resultMessage.set(null);
    this.saveResult.clearFeedback();
    this.selectedDraw.set(draw);
  }
}

function datePresetFromQuery(value: string | null): DatePreset {
  switch (value?.trim().toUpperCase()) {
    case 'TODAY':
      return 'TODAY';
    case 'TOMORROW':
      return 'TOMORROW';
    case 'THIS_WEEK':
      return 'THIS_WEEK';
    default:
      return 'LAST_48H';
  }
}

function statusFilterFromQuery(value: string | null): DrawStatusFilter {
  switch (value?.trim().toUpperCase()) {
    case 'SCHEDULED':
      return 'SCHEDULED';
    case 'OPEN':
      return 'OPEN';
    case 'LOCKED':
      return 'LOCKED';
    case 'CLOSED':
      return 'CLOSED';
    case 'RESULTED':
      return 'RESULTED';
    case 'SETTLED':
      return 'SETTLED';
    case 'CANCELLED':
      return 'CANCELLED';
    case 'ARCHIVED':
      return 'ARCHIVED';
    case 'PAST':
      return 'PAST';
    case 'EXPECTED_OR_MISSING':
      return 'EXPECTED_OR_MISSING';
    case 'NOT_DUE':
      return 'NOT_DUE';
    case 'EXPECTED':
      return 'EXPECTED';
    case 'MISSING':
      return 'MISSING';
    case 'PROVISIONAL':
      return 'PROVISIONAL';
    case 'CONFIRMED':
      return 'CONFIRMED';
    case 'SOURCE_ERROR':
      return 'SOURCE_ERROR';
    default:
      return 'all';
  }
}

function dateParam(value: string | null, fallback: string): string {
  if (!value || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return fallback;
  return value;
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

function numberParam(value: string | null, fallback: number): number {
  if (value === null || value.trim() === '') return fallback;
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed >= 0 ? parsed : fallback;
}

function providerParam(value: string | null): string {
  if (value === null) return '';
  const provider = value.trim().toUpperCase();
  return /^[A-Z]{2,8}$/.test(provider) ? provider : '';
}

function slotKeyParam(value: string | null): string {
  if (value === null) return '';
  const slotKey = value.trim().toUpperCase();
  return /^[A-Z0-9_-]{2,64}$/.test(slotKey) ? slotKey : '';
}

function filterDrawsByChannel(
  draws: readonly GeneratedDrawView[],
  provider: string,
  slotKey: string,
): GeneratedDrawView[] {
  if (!provider && !slotKey) return [...draws];
  return draws.filter(draw => {
    const providerMatches = !provider || draw.providerCode.toUpperCase() === provider;
    const slotMatches = !slotKey || draw.slotKey.toUpperCase() === slotKey;
    return providerMatches && slotMatches;
  });
}
