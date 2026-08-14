import {
  ChangeDetectionStrategy,
  Component,
  OnDestroy,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { mapHttpErrorToProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { AccessService } from '@tch/core/auth';
import {
  CONSOLE_DRAW_RESULT_ACCESS,
  ConsoleDrawDetailComponent,
  ConsoleDrawDetailView,
  ConsoleEntityDetailActionEvent,
  consoleDrawDetailViewModel,
  consoleDrawPublicationStatusLabel,
  consoleDrawResultStatusLabel,
  consoleDrawSalesStatusLabel,
} from '@tch/web/console';
import { AdminSectionCardComponent } from '@tch/ui/console';
import { ErrorViewModel, resolveErrorFeedbackCopy, toErrorViewModel } from '@tch/web/errors';

import {
  GeneratedDrawResultStatus,
  GeneratedDrawSalesStatus,
  GeneratedDrawView,
  SaveDrawResultRequest,
} from '../../data-access/admin-generated-draws.models';
import {
  AdminDrawOverviewEffectiveLimit,
  AdminDrawOverviewExposureAlert,
  AdminGeneratedDrawsApiService,
} from '../../data-access/admin-generated-draws-api.service';
import {
  DrawResultDrawerComponent,
  DrawResultDrawerState,
} from '../../components/draw-result-drawer/draw-result-drawer.component';
import {
  AdminFinancialsApi,
  DrawFinancialRow,
  SellerTerminalDrawFinancialRow,
} from '../../../reports/data-access/admin-financials-api.service';
import {
  DrawActivityReport,
  DrawExposureAlert,
  DrawDetailActivityView,
  DrawDetailActivityComponent,
  DrawTopSelectionItem,
} from './components/draw-detail-activity/draw-detail-activity.component';
import { BlockNumberQuickDialogComponent } from '../../../limits/components/block-number-quick-dialog/block-number-quick-dialog.component';
import { AdminLimitsSectionComponent } from '../../../limits/components/limits-section/admin-limits-section.component';

// BlockNumberQuickDialogComponent is opened programmatically via MatDialog — not declared in the template.

type PageState = 'loading' | 'ready' | 'error';
type DrawActivityState = 'idle' | 'loading' | 'ready' | 'error';
type DrawTopSelectionsState = 'idle' | 'loading' | 'ready' | 'error';

@Component({
  selector: 'tch-admin-draw-detail-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    ConsoleDrawDetailComponent,
    AdminSectionCardComponent,
    AdminLimitsSectionComponent,
    DrawResultDrawerComponent,
    DrawDetailActivityComponent,
  ],
  templateUrl: './admin-draw-detail.page.html',
  styleUrls: ['./admin-draw-detail.page.scss'],
})
export class AdminDrawDetailPage implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly api = inject(AdminGeneratedDrawsApiService);
  private readonly financials = inject(AdminFinancialsApi);
  private readonly access = inject(AccessService);
  private readonly translate = inject(TranslateService);
  private readonly dialog = inject(MatDialog);
  private timerId: ReturnType<typeof setInterval> | null = null;

  readonly pageState = signal<PageState>('loading');
  readonly draw = signal<GeneratedDrawView | null>(null);
  readonly nowMs = signal(Date.now());
  readonly activityState = signal<DrawActivityState>('idle');
  readonly activityReport = signal<DrawActivityReport | null>(null);
  readonly topSelectionsState = signal<DrawTopSelectionsState>('idle');
  readonly topSelections = signal<readonly DrawTopSelectionItem[]>([]);
  readonly exposureAlerts = signal<readonly DrawExposureAlert[]>([]);
  readonly overviewEffectiveLimits = signal<readonly AdminDrawOverviewEffectiveLimit[]>([]);
  readonly errorTitle = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);
  readonly activityError = signal<ErrorViewModel | null>(null);
  readonly topSelectionsError = signal<ErrorViewModel | null>(null);
  readonly selectedDraw = signal<GeneratedDrawView | null>(null);
  readonly resultSaveState = signal<DrawResultDrawerState>('ready');
  readonly resultSaveMessage = signal<string | null>(null);

  readonly canEnterManualResults = computed(() =>
    this.access.can(CONSOLE_DRAW_RESULT_ACCESS.manual),
  );
  readonly canOverrideResults = computed(() =>
    this.access.can(CONSOLE_DRAW_RESULT_ACCESS.override),
  );
  readonly detailMeta = computed(() => {
    const draw = this.draw();
    if (!draw) return [];
    const values = [draw.slotKey, draw.timezone];
    if (draw.lifecycleStatus) values.push(draw.lifecycleStatus);
    return values;
  });
  readonly detailError = computed(() =>
    this.errorTitle()
      ? { title: this.errorTitle() ?? 'Problème détecté', message: this.errorMessage() ?? '' }
      : null,
  );
  readonly detailActions = computed(() => {
    const d = this.draw();
    const actions: { id: string; label: string; icon: string }[] = [
      { id: 'back', label: 'Retour', icon: 'arrow_back' },
    ];
    if (d?.salesStatus === 'OPEN') {
      actions.push({ id: 'block-number', label: 'Bloke nimero', icon: 'block' });
    }
    return actions;
  });
  readonly drawDetailView = computed<ConsoleDrawDetailView | null>(() => {
    const draw = this.draw();
    if (!draw) return null;
    return consoleDrawDetailViewModel({
      identityInput: {
        providerCode: draw.providerCode,
        providerName: draw.providerLabel,
        channelCode: draw.drawChannelCode,
        channelName: draw.label,
        slotKey: draw.slotKey,
        slotLabel: draw.slotLabel,
        officialDateLabel: draw.businessDate,
        officialTimeLabel: this.scheduledTime(draw),
        officialTimezoneLabel: draw.timezone,
        fallbackTitle: draw.label,
      },
      title: this.drawDisplayName(draw),
      description: this.scheduledLocalSummary(draw),
      meta: this.detailMeta(),
      statusLabel: this.salesStatusLabel(draw.salesStatus),
      statusTone: draw.salesStatus === 'OPEN' ? 'success' : 'neutral',
      overviewFacts: [
        { label: 'Code du slot', value: draw.slotKey, code: true },
        { label: 'Créneau de vente', value: this.slotDisplayLabel(draw) },
        { label: 'Date métier', value: draw.businessDate },
        { label: 'Heure du tirage', value: this.scheduledTime(draw) },
        { label: 'Tirage prévu', value: this.scheduledLocalDateTime(draw) },
        { label: 'Fin des ventes', value: this.cutoffLocalDateTime(draw) },
        { label: 'Fuseau horaire', value: draw.timezone },
        { label: 'État technique', value: draw.lifecycleStatus ?? 'Non disponible' },
      ],
      result: {
        statusLabel: this.resultStatusLabel(draw.resultStatus),
        publicationLabel: this.publicationStatusLabel(draw.publicationStatus),
        numbers: draw.numbers ?? [],
        modeLabel: draw.resultMode,
        fetchedAtTitle: this.resultTimestampTitle(draw),
        fetchedAtLabel: this.fetchedAtLabel(draw),
        emptyLabel: this.resultEmptyLabel(draw),
        sourceErrorLabel: draw.sourceError?.message ?? null,
      },
      aside: {
        title: this.drawDisplayName(draw),
        subtitle: this.asideDrawDateTime(draw),
        code: draw.slotKey,
        items: [
          {
            label: 'Fin des ventes',
            value: this.countdownLabel(draw),
            hint: this.translate.instant(this.salesStatusLabel(draw.salesStatus)),
          },
          {
            label: 'Ventes',
            value: this.asideTicketCountLabel(this.activityReport()),
            hint: this.asideSalesAmountLabel(this.activityReport()),
          },
          { label: 'Terrain', value: this.asideSellerCountLabel(this.activityReport()) },
          { label: 'Sélections chaudes', value: this.asideHotSelectionsLabel() },
          { label: 'Résultat', value: this.resultFollowupLabel(draw) },
        ],
        advice: this.operationalHint(draw),
      },
    });
  });
  readonly activityView = computed<DrawDetailActivityView | null>(() => {
    const draw = this.draw();
    if (!draw) return null;
    const report = this.activityReport();
    return {
      countdown: this.countdownLabel(draw),
      salesStatus: this.salesStatusLabel(draw.salesStatus),
      salesOpen: draw.salesStatus === 'OPEN',
      activityState: this.activityState(),
      activityReport: report,
      activityErrorMessage: this.activityError()?.message ?? null,
      noSalesHint: this.noSalesHint(report),
      sellersReportQueryParams: this.sellersReportQueryParams(draw),
      topSelectionsState: this.topSelectionsState(),
      topSelections: this.topSelections(),
      topSelectionsErrorMessage: this.topSelectionsError()?.message ?? null,
      exposureAlerts: this.exposureAlerts(),
      exposureLimitConfigured: this.exposureLimitConfigured(),
    };
  });

  ngOnInit(): void {
    this.load();
    this.timerId = setInterval(() => this.nowMs.set(Date.now()), 1000);
  }

  ngOnDestroy(): void {
    if (this.timerId) {
      clearInterval(this.timerId);
      this.timerId = null;
    }
  }

  onDetailAction(event: ConsoleEntityDetailActionEvent): void {
    if (event.action.id === 'back') {
      void this.router.navigate(['/app/admin/draws']);
    } else if (event.action.id === 'block-number') {
      const d = this.draw();
      if (d) this.openBlockNumber(d);
    }
  }

  load(): void {
    const drawId = this.route.snapshot.paramMap.get('id');
    if (!drawId) {
      this.pageState.set('error');
      this.errorTitle.set('Tirage introuvable');
      this.errorMessage.set('Aucun identifiant de tirage n’a été fourni.');
      return;
    }

    this.pageState.set('loading');
    this.errorTitle.set(null);
    this.errorMessage.set(null);
    this.activityError.set(null);
    this.topSelectionsError.set(null);

    this.api.getDrawById(drawId, { suppressShellFeedback: true }).subscribe({
      next: draw => {
        this.draw.set(draw);
        this.pageState.set('ready');
        this.loadActivity(draw);
        this.loadDrawOverview(draw);
      },
      error: err => {
        const error = this.errorViewModel(err, 'admin.generatedDraws.detail');
        this.pageState.set('error');
        this.errorTitle.set(error.title);
        this.errorMessage.set(error.message);
      },
    });
  }

  countdownLabel(draw: GeneratedDrawView): string {
    if (draw.salesStatus !== 'OPEN') return 'Vente fermée';
    const target = this.dateTimeToEpochMs(draw.cutoffAt ?? draw.scheduledAt, draw.timezone);
    if (target == null) return 'Fin des ventes non disponible';
    const diff = target - this.nowMs();
    if (diff <= 0) return 'Ventes fermées';

    const totalSeconds = Math.floor(diff / 1000);
    const hours = Math.floor(totalSeconds / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    const seconds = totalSeconds % 60;
    if (hours > 0) {
      return `${hours}h ${minutes.toString().padStart(2, '0')}m ${seconds.toString().padStart(2, '0')}s`;
    }
    return `${minutes}m ${seconds.toString().padStart(2, '0')}s`;
  }

  drawDisplayName(draw: GeneratedDrawView): string {
    const label = draw.label || draw.providerLabel;
    return label.replace(/^Ha[iï]ti\s*[·•-]\s*/i, '').trim();
  }

  providerSlotCode(draw: GeneratedDrawView): string {
    return draw.slotKey;
  }

  slotDisplayLabel(draw: GeneratedDrawView): string {
    const label = draw.slotLabel?.trim();
    if (!label) return draw.slotKey;
    if (label === label.toUpperCase()) {
      return label
        .toLowerCase()
        .split(/[\s_-]+/)
        .map(part => part.charAt(0).toUpperCase() + part.slice(1))
        .join(' ');
    }
    return label;
  }

  scheduledLocalSummary(draw: GeneratedDrawView): string {
    const value = this.formatLocalDateTime(draw.scheduledAt, draw.timezone);
    return value ? `Tirage prévu le ${value}` : 'Tirage prévu';
  }

  scheduledLocalDateTime(draw: GeneratedDrawView): string {
    return this.formatLocalDateTime(draw.scheduledAt, draw.timezone) ?? 'Non disponible';
  }

  cutoffLocalDateTime(draw: GeneratedDrawView): string {
    const cutoff = draw.cutoffAt ?? draw.scheduledAt;
    return this.formatLocalDateTime(cutoff, draw.timezone) ?? 'Non disponible';
  }

  asideDrawDateTime(draw: GeneratedDrawView): string {
    const date = this.formatIsoLikeLocalDateTime(draw.scheduledAt, draw.timezone);
    return date ? `${draw.providerLabel} · ${date}` : draw.providerLabel;
  }

  scheduledTime(draw: GeneratedDrawView): string {
    const parts = this.hasExplicitOffset(draw.scheduledAt)
      ? null
      : this.localDateTimeParts(draw.scheduledAt);
    if (parts) return `${parts.hour}:${parts.minute}`;
    const date = new Date(draw.scheduledAt);
    if (Number.isNaN(date.getTime())) return 'Non disponible';
    return this.formatTimeInZone(date, draw.timezone);
  }

  operationalHint(draw: GeneratedDrawView): string {
    if (draw.salesStatus === 'OPEN') {
      return 'Le tirage est ouvert. Surveillez les ventes et les sélections chaudes avant la fermeture.';
    }
    if (this.hasResult(draw)) {
      return 'Le résultat est disponible. Vérifiez la publication et le suivi des tickets gagnants.';
    }
    if (draw.salesStatus === 'CLOSED') {
      return 'Le tirage est fermé. Vous pouvez vérifier le résultat et lancer le suivi des tickets.';
    }
    if (draw.salesStatus === 'CANCELLED') {
      return 'Le tirage est annulé. Vérifiez les ventes et les opérations de suivi associées.';
    }
    return 'Le tirage est planifié. Les ventes seront visibles dès son ouverture.';
  }

  noSalesHint(report: DrawActivityReport | null): string | null {
    if (!report || report.ticketsSold > 0 || report.grossSales > 0) return null;
    const draw = this.draw();
    if (!draw) return 'Aucune vente enregistrée pour ce tirage.';
    if (draw.salesStatus === 'OPEN') {
      return 'Le tirage est ouvert, mais aucune vente n’a encore été enregistrée.';
    }
    return 'Aucune vente enregistrée pour ce tirage.';
  }

  sellersReportQueryParams(draw: GeneratedDrawView): Record<string, string> {
    return {
      drawId: draw.drawId,
      tab: 'sellers',
    };
  }

  salesStatusLabel(status: GeneratedDrawSalesStatus): string {
    return consoleDrawSalesStatusLabel(status);
  }

  resultStatusLabel(status: GeneratedDrawResultStatus): string {
    return consoleDrawResultStatusLabel(status);
  }

  publicationStatusLabel(status: GeneratedDrawView['publicationStatus']): string {
    return consoleDrawPublicationStatusLabel(status);
  }

  canEnterManualResult(draw: GeneratedDrawView): boolean {
    return (
      this.canEnterManualResults() &&
      this.isDrawPastSales(draw) &&
      this.hasNoResult(draw) &&
      this.isManualResultDue(draw)
    );
  }

  resultActionLabel(draw: GeneratedDrawView): string {
    if (this.hasResult(draw)) return 'Détail du résultat';
    if (this.canEnterManualResult(draw)) return 'Entrer le résultat';
    return '';
  }

  resultDetailQueryParams(draw: GeneratedDrawView): Record<string, string> {
    return {
      drawId: draw.drawId,
      slotKey: draw.slotKey,
      drawDate: draw.businessDate,
    };
  }

  ticketsQueryParams(draw: GeneratedDrawView): Record<string, string> {
    return {
      provider: draw.providerCode,
      slotKey: draw.slotKey,
      from: draw.businessDate,
      to: draw.businessDate,
    };
  }

  limitsSectionTitle(): string {
    return this.translate.instant('admin.generatedDraws.detail.limitsTitle');
  }

  tenantScopeLabel(): string {
    return this.translate.instant('admin.limits.section.scope.tenant');
  }

  resultUnavailableReason(draw: GeneratedDrawView): string | null {
    if (this.hasResult(draw)) return null;
    if (!this.canEnterManualResults()) {
      return 'Vous n’avez pas l’autorisation de saisir un résultat manuel.';
    }
    if (!this.isDrawPastSales(draw)) return null;
    if (!this.isManualResultDue(draw)) {
      return 'La saisie manuelle sera disponible 30 minutes après l’heure prévue du tirage.';
    }
    return null;
  }

  asideTicketCountLabel(report: DrawActivityReport | null): string {
    const count = report?.ticketsSold ?? 0;
    return `${count} ticket${count > 1 ? 's' : ''}`;
  }

  asideSalesAmountLabel(report: DrawActivityReport | null): string {
    return `${this.formatAmount(report?.grossSales ?? 0)} HTG`;
  }

  asideSellerCountLabel(report: DrawActivityReport | null): string {
    const count = report?.activeSellerCount ?? 0;
    return `${count} vendeur${count > 1 ? 's' : ''} actif${count > 1 ? 's' : ''}`;
  }

  asideHotSelectionsLabel(): string {
    const count = this.exposureAlerts().length || this.topSelections().length;
    if (count === 0) return 'Aucune';
    return `${count} à surveiller`;
  }

  exposureLimitConfigured(): boolean {
    return this.overviewEffectiveLimits().some(limit => limit.ruleKey.includes('EXPOSURE'));
  }

  resultFollowupLabel(draw: GeneratedDrawView): string {
    if (this.hasResult(draw)) return 'Disponible';
    if (draw.salesStatus === 'OPEN') return 'Attendu après fermeture';
    if (this.canEnterManualResult(draw)) return 'Saisie manuelle possible';
    return this.resultStatusLabel(draw.resultStatus);
  }

  fetchedAtLabel(draw: GeneratedDrawView): string {
    return draw.fetchedAt
      ? (this.formatLocalDateTime(draw.fetchedAt, draw.timezone) ?? 'Non disponible')
      : 'Non disponible';
  }

  resultTimestampTitle(draw: GeneratedDrawView): string {
    return draw.resultMode === 'MANUAL' ? 'Saisi le' : 'Récupéré le';
  }

  resultEmptyLabel(draw: GeneratedDrawView): string {
    if (draw.salesStatus === 'CLOSED' || draw.salesStatus === 'CANCELLED') {
      return (
        this.resultUnavailableReason(draw) ??
        'Aucun résultat n’est encore disponible pour ce tirage.'
      );
    }
    return 'Le tirage est encore ouvert. Le résultat pourra être saisi ou récupéré après la fermeture.';
  }

  openBlockNumber(draw: GeneratedDrawView): void {
    this.dialog.open(BlockNumberQuickDialogComponent, {
      width: '480px',
      maxWidth: '95vw',
      data: {
        channelId: draw.drawChannelId,
        channelLabel: draw.label || this.slotDisplayLabel(draw),
      },
    });
  }

  openResultPanel(draw: GeneratedDrawView): void {
    this.resultSaveState.set('ready');
    this.resultSaveMessage.set(null);
    this.selectedDraw.set(draw);
  }

  closeResultPanel(): void {
    this.selectedDraw.set(null);
    this.resultSaveState.set('ready');
    this.resultSaveMessage.set(null);
  }

  onResultSaveRequested(request: SaveDrawResultRequest): void {
    this.resultSaveState.set('saving');
    this.resultSaveMessage.set(null);
    this.api.saveDrawResult(request, { suppressShellFeedback: true }).subscribe({
      next: updated => {
        this.draw.set(updated);
        this.selectedDraw.set(updated);
        this.resultSaveState.set('success');
        this.resultSaveMessage.set(
          request.mode === 'confirmed'
            ? 'Résultat confirmé et publié.'
            : 'Résultat enregistré en provisoire.',
        );
      },
      error: err => {
        this.resultSaveState.set('error');
        this.resultSaveMessage.set(
          this.errorViewModel(err, 'admin.generatedDraws.detail.saveResult', 'form').message,
        );
      },
    });
  }

  private errorViewModel(
    err: unknown,
    source: string,
    surface: 'page' | 'section' | 'form' = 'page',
  ): ErrorViewModel {
    const normalized = webAppErrorFromProblemDetail(
      mapHttpErrorToProblemDetail(err),
      source,
      surface,
    );
    const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
    return toErrorViewModel(normalized, copy);
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

  private isDrawPastSales(draw: GeneratedDrawView): boolean {
    return draw.salesStatus === 'CLOSED' || draw.salesStatus === 'CANCELLED';
  }

  private isManualResultDue(draw: GeneratedDrawView): boolean {
    const scheduledAt = this.dateTimeToEpochMs(draw.scheduledAt, draw.timezone);
    if (scheduledAt == null) return false;
    return Date.now() >= scheduledAt + 30 * 60 * 1000;
  }

  private formatLocalDateTime(value: string, timezone: string): string | null {
    const parts = this.hasExplicitOffset(value) ? null : this.localDateTimeParts(value);
    if (parts) return `${parts.day}/${parts.month}/${parts.year} ${parts.hour}:${parts.minute}`;

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return null;
    return date.toLocaleString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      timeZone: timezone,
    });
  }

  private formatAmount(value: number): string {
    return new Intl.NumberFormat('fr-FR', {
      maximumFractionDigits: 2,
    }).format(value);
  }

  private formatIsoLikeLocalDateTime(value: string, timezone: string): string | null {
    const parts = this.hasExplicitOffset(value) ? null : this.localDateTimeParts(value);
    if (parts) return `${parts.year}-${parts.month}-${parts.day} ${parts.hour}:${parts.minute}`;

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return null;
    const dateParts = new Intl.DateTimeFormat('fr-CA', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      hourCycle: 'h23',
      timeZone: timezone,
    }).formatToParts(date);
    const pick = (type: string) => dateParts.find(part => part.type === type)?.value ?? '';
    return `${pick('year')}-${pick('month')}-${pick('day')} ${pick('hour')}:${pick('minute')}`;
  }

  private dateTimeToEpochMs(value: string, timezone: string): number | null {
    if (this.hasExplicitOffset(value)) {
      const epoch = Date.parse(value);
      return Number.isNaN(epoch) ? null : epoch;
    }

    const parts = this.localDateTimeParts(value);
    if (!parts) {
      const epoch = Date.parse(value);
      return Number.isNaN(epoch) ? null : epoch;
    }

    return this.zonedLocalTimeToEpochMs(
      Number(parts.year),
      Number(parts.month),
      Number(parts.day),
      Number(parts.hour),
      Number(parts.minute),
      Number(parts.second ?? '0'),
      timezone,
    );
  }

  private localDateTimeParts(value: string): {
    year: string;
    month: string;
    day: string;
    hour: string;
    minute: string;
    second?: string;
  } | null {
    const match = value.match(/^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})(?::(\d{2}))?/);
    if (!match) return null;
    return {
      year: match[1],
      month: match[2],
      day: match[3],
      hour: match[4],
      minute: match[5],
      second: match[6],
    };
  }

  private hasExplicitOffset(value: string): boolean {
    return /(?:Z|[+-]\d{2}:?\d{2})$/i.test(value);
  }

  private zonedLocalTimeToEpochMs(
    year: number,
    month: number,
    day: number,
    hour: number,
    minute: number,
    second: number,
    timezone: string,
  ): number {
    const utcGuess = Date.UTC(year, month - 1, day, hour, minute, second);
    const offset = this.timezoneOffsetMs(new Date(utcGuess), timezone);
    const first = utcGuess - offset;
    const correctedOffset = this.timezoneOffsetMs(new Date(first), timezone);
    return utcGuess - correctedOffset;
  }

  private timezoneOffsetMs(date: Date, timezone: string): number {
    const parts = new Intl.DateTimeFormat('en-US', {
      timeZone: timezone,
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hourCycle: 'h23',
    }).formatToParts(date);
    const pick = (type: string) => Number(parts.find(part => part.type === type)?.value ?? 0);
    const asUtc = Date.UTC(
      pick('year'),
      pick('month') - 1,
      pick('day'),
      pick('hour'),
      pick('minute'),
      pick('second'),
    );
    return asUtc - date.getTime();
  }

  private formatTimeInZone(date: Date, timezone: string): string {
    return new Intl.DateTimeFormat('fr-FR', {
      hour: '2-digit',
      minute: '2-digit',
      hourCycle: 'h23',
      timeZone: timezone,
    }).format(date);
  }

  private loadActivity(draw: GeneratedDrawView): void {
    this.activityState.set('loading');
    this.activityReport.set(null);
    this.financials
      .getBreakdown(
        {
          from: draw.businessDate,
          to: draw.businessDate,
          drawLimit: 250,
          sellerTerminalLimit: 500,
        },
        { suppressShellFeedback: true },
      )
      .subscribe({
        next: breakdown => {
          const drawRows = breakdown.drawRows.filter(row => row.drawId === draw.drawId);
          const sellerRows = breakdown.sellerTerminalDrawRows.filter(
            row => row.drawId === draw.drawId,
          );
          this.activityReport.set(this.aggregateActivity(drawRows, sellerRows));
          this.activityState.set('ready');
        },
        error: err => {
          this.activityError.set(
            this.errorViewModel(err, 'admin.generatedDraws.detail.activity', 'section'),
          );
          this.activityState.set('error');
        },
      });
  }

  private loadDrawOverview(draw: GeneratedDrawView): void {
    this.topSelectionsState.set('loading');
    this.topSelections.set([]);
    this.exposureAlerts.set([]);
    this.overviewEffectiveLimits.set([]);
    this.api
      .getDrawOverview(draw.drawId, { suppressShellFeedback: true })
      .subscribe({
        next: overview => {
          this.topSelections.set(overview.topSelections);
          this.exposureAlerts.set(this.mapExposureAlerts(overview.exposureAlerts));
          this.overviewEffectiveLimits.set(overview.effectiveLimits);
          this.topSelectionsState.set('ready');
        },
        error: err => {
          this.topSelectionsError.set(
            this.errorViewModel(err, 'admin.generatedDraws.detail.topSelections', 'section'),
          );
          this.topSelectionsState.set('error');
        },
      });
  }

  private mapExposureAlerts(
    alerts: readonly AdminDrawOverviewExposureAlert[],
  ): readonly DrawExposureAlert[] {
    return alerts.map(alert => ({
      selectionKey: alert.selectionKey,
      stakeTotal: Number(alert.stakeTotal ?? 0),
      maxStakeExposureLimit:
        alert.maxStakeExposureLimit === null ? null : Number(alert.maxStakeExposureLimit),
      stakeRatio: alert.stakeRatio === null ? null : Number(alert.stakeRatio),
    }));
  }

  private aggregateActivity(
    drawRows: readonly DrawFinancialRow[],
    sellerRows: readonly SellerTerminalDrawFinancialRow[],
  ): DrawActivityReport {
    const activeSellerIds = new Set(
      sellerRows
        .filter(row => row.ticketsSold > 0 || row.grossSales > 0)
        .map(row => row.sellerTerminalId),
    );
    return {
      ticketsSold: this.sum(drawRows, row => row.ticketsSold),
      grossSales: this.sum(drawRows, row => row.grossSales),
      sellerCommission: this.sum(drawRows, row => row.sellerCommission),
      activeSellerCount: activeSellerIds.size,
      netRevenueEstimated: this.sum(drawRows, row => row.netRevenueEstimated),
      promotionPayoutBase: this.sum(drawRows, row => row.promotionPayoutBase),
    };
  }

  private sum<T>(rows: readonly T[], selector: (row: T) => number): number {
    return rows.reduce((total, row) => total + Number(selector(row) ?? 0), 0);
  }
}
