import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, input, output, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {
  ConsoleDrawActionEvent,
  ConsoleDrawRow,
  ConsoleDrawSelectionEvent,
  ConsoleDrawsTableComponent,
  ConsoleRowAction,
} from '@tch/web/console';
import {
  DrawLifecycleAction,
  GeneratedDrawGroup,
  GeneratedDrawView,
  generatedDrawCountdownMs,
  generatedDrawDateTimeToEpochMs,
  generatedDrawLocalDateLabel,
  generatedDrawProviderAndTenantTimeLabel,
  generatedDrawTenantDateTimeLabel,
} from '../../data-access/admin-generated-draws.models';
import { lotteryLogoForSlot } from '../../../../../shared/lottery/lottery-assets';
import { RuntimeSettingsStore } from '@tch/shared-config';

@Component({
  selector: 'tch-generated-draws-table',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ConsoleDrawsTableComponent, MatButtonModule, MatIconModule],
  templateUrl: './generated-draws-table.component.html',
  styleUrls: ['./generated-draws-table.component.scss'],
})
export class GeneratedDrawsTableComponent {
  private readonly destroyRef = inject(DestroyRef);
  private readonly runtimeSettings = inject(RuntimeSettingsStore);

  readonly groups        = input.required<GeneratedDrawGroup[]>();
  readonly todayDate     = input<string>('');
  readonly totalElements = input<number>(0);
  readonly page          = input<number>(0);
  readonly pageSize      = input<number>(20);
  /** Draws avec une action de cycle de vie en cours (spinner + actions désactivées sur la ligne). */
  readonly pendingIds    = input<ReadonlySet<string>>(new Set());

  readonly enterResult   = output<GeneratedDrawView>();
  readonly viewResult    = output<GeneratedDrawView>();
  readonly viewDetails   = output<GeneratedDrawView>();
  readonly verifySource  = output<GeneratedDrawView>();
  readonly openDraw      = output<GeneratedDrawView>();
  readonly closeDraw     = output<GeneratedDrawView>();
  readonly lockDraw      = output<GeneratedDrawView>();
  readonly unlockDraw    = output<GeneratedDrawView>();
  readonly cancelDraw    = output<GeneratedDrawView>();
  readonly archiveDraw   = output<GeneratedDrawView>();
  readonly bulkLifecycle = output<{ action: DrawLifecycleAction; draws: readonly GeneratedDrawView[] }>();
  readonly nextPage      = output<void>();
  readonly prevPage      = output<void>();

  readonly selectedIds = signal<ReadonlySet<string>>(new Set());
  readonly nowMs = signal(Date.now());
  readonly tenantTimezone = computed(() => {
    const value = this.runtimeSettings.settings().values['app.timezone'];
    return typeof value === 'string' && value.trim() ? value : 'America/Port-au-Prince';
  });

  constructor() {
    const timer = globalThis.setInterval(() => this.nowMs.set(Date.now()), 1000);
    this.destroyRef.onDestroy(() => globalThis.clearInterval(timer));
  }
  readonly selectedDraws = computed(() => {
    const ids = this.selectedIds();
    return this.groups()
      .flatMap(group => group.draws)
      .filter(draw => ids.has(draw.drawId));
  });
  readonly selectedCount = computed(() => this.selectedIds().size);
  readonly hasSelection = computed(() => this.selectedCount() > 0);
  readonly commonLifecycleActions = computed(() => {
    const selected = this.selectedDraws();
    if (selected.length === 0) return [];
    const [first, ...rest] = selected;
    return this.lifecycleActions(first).filter(action =>
      rest.every(draw => this.lifecycleActions(draw).includes(action)),
    );
  });
  readonly drawRows = computed<readonly ConsoleDrawRow[]>(() =>
    this.groups().flatMap(group =>
      group.draws.map(draw => this.toConsoleDrawRow(draw, this.groupDateLabel(group.date))),
    ),
  );

  get hasPrev(): boolean { return this.page() > 0; }
  get hasNext(): boolean { return (this.page() + 1) * this.pageSize() < this.totalElements(); }
  get rangeStart(): number { return this.page() * this.pageSize() + 1; }
  get rangeEnd(): number {
    return Math.min((this.page() + 1) * this.pageSize(), this.totalElements());
  }

  groupDateLabel(date: string): string {
    const d = new Date(date + 'T00:00:00');
    const today = this.todayDate();
    const tomorrow = new Date(today + 'T00:00:00');
    tomorrow.setDate(tomorrow.getDate() + 1);
    const tomorrowStr = tomorrow.toISOString().slice(0, 10);

    const label = d.toLocaleDateString('fr-FR', {
      weekday: 'long', day: 'numeric', month: 'long', year: 'numeric',
    }).toUpperCase();

    if (date === today)       return `${label} (AUJOURD'HUI)`;
    if (date === tomorrowStr) return `${label} (DEMAIN)`;
    return label;
  }

  accentColor(draw: GeneratedDrawView): string {
    if (draw.resultStatus === 'CONFIRMED')    return 'ready';
    if (draw.resultStatus === 'SOURCE_ERROR') return 'error';
    if (draw.resultStatus === 'MISSING')      return 'error';
    if (draw.salesStatus === 'OPEN')          return 'open';
    return 'neutral';
  }

  scheduledDate(draw: GeneratedDrawView): string {
    return generatedDrawLocalDateLabel(draw.scheduledAt, draw.timezone) ?? '—';
  }

  scheduledTime(draw: GeneratedDrawView): string {
    return generatedDrawProviderAndTenantTimeLabel(draw.scheduledAt, draw.timezone, this.tenantTimezone()) ?? '—';
  }

  fetchedAtLabel(draw: GeneratedDrawView): string {
    return generatedDrawTenantDateTimeLabel(draw.fetchedAt, this.tenantTimezone());
  }

  sourceErrorAtLabel(draw: GeneratedDrawView): string {
    return generatedDrawTenantDateTimeLabel(draw.sourceError?.occurredAt, this.tenantTimezone());
  }

  salesCountdown(draw: GeneratedDrawView): string | null {
    const remainingMs = generatedDrawCountdownMs(draw, this.nowMs());
    if (remainingMs == null) return null;
    if (remainingMs <= 0) return 'Vente expirée';
    const totalSeconds = Math.floor(remainingMs / 1000);
    const hours = Math.floor(totalSeconds / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    const seconds = totalSeconds % 60;
    const time = hours > 0
      ? `${hours}h ${String(minutes).padStart(2, '0')}m ${String(seconds).padStart(2, '0')}s`
      : `${minutes}m ${String(seconds).padStart(2, '0')}s`;
    return `Fin ventes dans ${time}`;
  }

  isClosingSoon(draw: GeneratedDrawView): boolean {
    const remainingMs = generatedDrawCountdownMs(draw, this.nowMs());
    return remainingMs != null && remainingMs > 0 && remainingMs <= 5 * 60 * 1000;
  }

  providerLogo(draw: GeneratedDrawView): string | null {
    return lotteryLogoForSlot(draw.slotKey);
  }

  canEnterManualResult(draw: GeneratedDrawView): boolean {
    return this.hasNoResult(draw) && this.isManualResultDue(draw);
  }

  lifecycleActions(draw: GeneratedDrawView): DrawLifecycleAction[] {
    switch (draw.lifecycleStatus) {
      case 'SCHEDULED': return ['open', 'lock', 'cancel'];
      case 'OPEN':      return ['close', 'lock', 'cancel'];
      case 'LOCKED':    return ['unlock', 'cancel'];
      case 'CLOSED':    return ['cancel'];
      case 'RESULTED':
      case 'SETTLED':   return ['archive'];
      default:          return [];
    }
  }

  lifecycleLabel(action: DrawLifecycleAction): string {
    switch (action) {
      case 'open':    return 'Ouvrir la vente';
      case 'close':   return 'Fermer la vente';
      case 'lock':    return 'Verrouiller';
      case 'unlock':  return 'Déverrouiller';
      case 'cancel':  return 'Annuler';
      case 'archive': return 'Archiver';
    }
  }

  lifecycleIcon(action: DrawLifecycleAction): string {
    switch (action) {
      case 'open':    return 'play_arrow';
      case 'close':   return 'stop';
      case 'lock':    return 'lock';
      case 'unlock':  return 'lock_open';
      case 'cancel':  return 'cancel';
      case 'archive': return 'inventory_2';
    }
  }

  onLifecycleAction(draw: GeneratedDrawView, action: DrawLifecycleAction): void {
    switch (action) {
      case 'open':    this.openDraw.emit(draw);    break;
      case 'close':   this.closeDraw.emit(draw);   break;
      case 'lock':    this.lockDraw.emit(draw);    break;
      case 'unlock':  this.unlockDraw.emit(draw);  break;
      case 'cancel':  this.cancelDraw.emit(draw);  break;
      case 'archive': this.archiveDraw.emit(draw); break;
    }
  }

  onRowAction(event: ConsoleDrawActionEvent): void {
    const draw = this.findDraw(event.row.id);
    if (!draw) return;

    if (event.action.id.startsWith('lifecycle:')) {
      this.onLifecycleAction(draw, event.action.id.slice('lifecycle:'.length) as DrawLifecycleAction);
      return;
    }

    switch (event.action.id) {
      case 'enterResult':
        this.enterResult.emit(draw);
        break;
      case 'viewResult':
        this.viewResult.emit(draw);
        break;
      case 'verifySource':
        this.verifySource.emit(draw);
        break;
      case 'viewDetails':
        this.viewDetails.emit(draw);
        break;
    }
  }

  onRowSelection(event: ConsoleDrawSelectionEvent): void {
    const draw = this.findDraw(event.row.id);
    if (draw) this.toggleSelection(draw, event.selected);
  }

  isPending(draw: GeneratedDrawView): boolean {
    return this.pendingIds().has(draw.drawId);
  }

  isSelected(draw: GeneratedDrawView): boolean {
    return this.selectedIds().has(draw.drawId);
  }

  toggleSelection(draw: GeneratedDrawView, checked: boolean): void {
    this.selectedIds.update(current => {
      const next = new Set(current);
      if (checked) {
        next.add(draw.drawId);
      } else {
        next.delete(draw.drawId);
      }
      return next;
    });
  }

  clearSelection(): void {
    this.selectedIds.set(new Set());
  }

  onBulkLifecycleAction(action: DrawLifecycleAction): void {
    const draws = this.selectedDraws();
    if (draws.length === 0) return;
    this.bulkLifecycle.emit({ action, draws });
    this.clearSelection();
  }

  private hasNoResult(draw: GeneratedDrawView): boolean {
    return draw.resultStatus !== 'PROVISIONAL'
      && draw.resultStatus !== 'CONFIRMED'
      && (draw.numbers?.length ?? 0) === 0;
  }

  private isManualResultDue(draw: GeneratedDrawView): boolean {
    const scheduledAt = generatedDrawDateTimeToEpochMs(draw.scheduledAt, draw.timezone);
    if (scheduledAt == null) return false;
    return Date.now() >= scheduledAt + 30 * 60 * 1000;
  }

  private findDraw(drawId: string): GeneratedDrawView | null {
    return this.groups()
      .flatMap(group => group.draws)
      .find(draw => draw.drawId === drawId) ?? null;
  }

  private toConsoleDrawRow(draw: GeneratedDrawView, groupLabel: string): ConsoleDrawRow {
    return {
      id: draw.drawId,
      groupLabel,
      title: draw.label,
      subtitle: `${draw.slotLabel} · ${draw.slotKey}`,
      meta: draw.providerLabel,
      logoUrl: this.providerLogo(draw),
      logoAlt: draw.providerLabel,
      scheduledDateLabel: this.scheduledDate(draw),
      scheduledTimeLabel: this.scheduledTime(draw),
      countdownLabel: this.salesCountdown(draw),
      countdownTone: this.isClosingSoon(draw) ? 'warning' : 'default',
      statusLabel: draw.salesStatus,
      statusTone: this.salesTone(draw.salesStatus),
      resultLabel: draw.resultStatus,
      resultTone: this.resultTone(draw.resultStatus),
      resultNumbers: draw.numbers?.map(n => String(n)) ?? [],
      resultHint: this.resultHint(draw),
      modeLabel: draw.resultMode,
      publicationLabel: draw.publicationStatus && draw.publicationStatus !== 'NOT_PUBLISHED'
        ? draw.publicationStatus
        : undefined,
      publicationTone: draw.publicationStatus ? this.publicationTone(draw.publicationStatus) : 'neutral',
      pending: this.isPending(draw),
      actions: this.consoleActions(draw),
    };
  }

  private consoleActions(draw: GeneratedDrawView): readonly ConsoleRowAction[] {
    const actions: ConsoleRowAction[] = [this.primaryAction(draw)];
    for (const action of this.lifecycleActions(draw)) {
      actions.push({
        id: `lifecycle:${action}`,
        label: this.lifecycleLabel(action),
        icon: this.lifecycleIcon(action),
        tone: action === 'cancel' ? 'danger' : 'default',
        variant: 'icon',
      });
    }
    return actions;
  }

  private primaryAction(draw: GeneratedDrawView): ConsoleRowAction {
    switch (draw.resultStatus) {
      case 'MISSING':
        return this.canEnterManualResult(draw)
          ? { id: 'enterResult', label: 'Saisir résultat', icon: 'edit_note', tone: 'primary' }
          : { id: 'viewDetails', label: 'Voir détails', icon: 'visibility' };
      case 'CONFIRMED':
        return { id: 'viewResult', label: 'Voir résultat', icon: 'fact_check' };
      case 'SOURCE_ERROR':
        return this.canEnterManualResult(draw)
          ? { id: 'verifySource', label: 'Vérifier', icon: 'warning', tone: 'danger' }
          : { id: 'viewDetails', label: 'Voir détails', icon: 'visibility' };
      default:
        return { id: 'viewDetails', label: 'Voir détails', icon: 'visibility' };
    }
  }

  private resultHint(draw: GeneratedDrawView): string | undefined {
    if (draw.sourceError) return this.sourceErrorAtLabel(draw);
    if (draw.fetchedAt) return `Récupéré ${this.fetchedAtLabel(draw)}`;
    return undefined;
  }

  private salesTone(status: string) {
    switch (status) {
      case 'OPEN': return 'success';
      case 'LOCKED': return 'warning';
      case 'CANCELLED': return 'danger';
      default: return 'neutral';
    }
  }

  private resultTone(status: string) {
    switch (status) {
      case 'CONFIRMED': return 'success';
      case 'PROVISIONAL': return 'warning';
      case 'MISSING':
      case 'SOURCE_ERROR': return 'danger';
      default: return 'neutral';
    }
  }

  private publicationTone(status: string) {
    switch (status) {
      case 'PUBLISHED': return 'success';
      case 'PROVISIONAL': return 'warning';
      default: return 'neutral';
    }
  }
}
