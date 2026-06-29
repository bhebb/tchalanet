import {
  ChangeDetectionStrategy,
  Component,
  computed,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateService } from '@ngx-translate/core';

import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { TchErrorPanel, TchLoading, TchSearchOption, TchSearchSelect, TchSectionError } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { AdminCrudShellComponent } from '../../../../shared/admin-ui/admin-crud-shell.component';
import { AdminDataToolbarComponent } from '../../../../shared/admin-ui/admin-data-toolbar.component';
import { AdminEmptyStateComponent } from '../../../../shared/admin-ui/admin-empty-state.component';
import { AdminPageShellComponent } from '../../../../shared/admin-ui/admin-page-shell.component';
import { AdminStatusPillComponent, AdminStatusTone } from '../../../../shared/admin-ui/admin-status-pill.component';
import {
  PlatformOpsApi,
  DrawView,
  OpenTodayDrawsRequest,
  CloseDueDrawsRequest,
} from '../../data-access/platform-ops-api.service';
import { GenerateDrawsDialog } from '../../components/dialogs/generate-draws.dialog';
import { BatchOpDialog, AnyBatchResult } from '../../components/dialogs/batch-op.dialog';
import { ApplyResultsDialog } from '../../components/dialogs/apply-results.dialog';
import {
  BulkCancelDrawDialog,
  BulkDrawActionType,
  BulkSimpleDrawActionDialog,
  CancelDrawDialog,
  CorrectDrawResultDialog,
  RescheduleDrawDialog,
  SimpleDrawActionDialog,
} from '../../components/dialogs/draw-action-dialogs';
import { lotteryAssetForSlot } from '../../../../../../shared/lottery/lottery-assets';
import { PlatformTenantsApi, TenantSummaryView } from '../../../tenants/data-access/platform-tenants-api.service';
import { Observable, map } from 'rxjs';

// ── Helpers ───────────────────────────────────────────────────────────────────

function toneForStatus(status: string): AdminStatusTone {
  switch (status) {
    case 'OPEN': return 'success';
    case 'RESULTED': case 'SETTLED': return 'info';
    case 'LOCKED': return 'warning';
    case 'CANCELLED': return 'danger';
    case 'ARCHIVED': return 'neutral';
    default: return 'neutral';
  }
}

type DrawActionItem =
  | { kind: 'cancel' }
  | { kind: 'lock' }
  | { kind: 'unlock' }
  | { kind: 'reschedule' }
  | { kind: 'settle' }
  | { kind: 'archive' }
  | { kind: 'correct' };

function actionsForDraw(draw: DrawView): DrawActionItem[] {
  switch (draw.status) {
    case 'SCHEDULED': return [{ kind: 'lock' }, { kind: 'reschedule' }, { kind: 'cancel' }];
    case 'OPEN':      return [{ kind: 'lock' }, { kind: 'cancel' }];
    case 'LOCKED':    return [{ kind: 'unlock' }, { kind: 'cancel' }];
    case 'CLOSED':    return [{ kind: 'settle' }, { kind: 'cancel' }];
    case 'RESULTED':  return [{ kind: 'correct' }, { kind: 'settle' }, { kind: 'archive' }];
    case 'SETTLED':   return [{ kind: 'archive' }];
    default: return [];
  }
}

function todayIsoDate(): string {
  return relativeIsoDate(0);
}

function relativeIsoDate(offsetDays: number): string {
  const now = new Date();
  now.setDate(now.getDate() + offsetDays);
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

const MAX_BULK_DRAW_ACTIONS = 50;

const ACTION_ICON: Record<string, string> = {
  cancel: 'cancel',
  lock: 'lock',
  unlock: 'lock_open',
  reschedule: 'schedule',
  settle: 'paid',
  archive: 'inventory_2',
  correct: 'edit',
};

const STATUS_OPTIONS = [
  { value: '', label: 'Tous les statuts' },
  { value: 'SCHEDULED', label: 'Planifié' },
  { value: 'OPEN', label: 'Ouvert' },
  { value: 'LOCKED', label: 'Verrouillé' },
  { value: 'CLOSED', label: 'Fermé' },
  { value: 'RESULTED', label: 'Résultat appliqué' },
  { value: 'SETTLED', label: 'Réglé' },
  { value: 'CANCELLED', label: 'Annulé' },
  { value: 'ARCHIVED', label: 'Archivé' },
];

const DEFAULT_DRAW_TENANT_CODE = 'tchalanet';

const DRAW_ACTION_LABELS: Record<string, string> = {
  cancel: 'Annuler',
  lock: 'Verrouiller',
  unlock: 'Déverrouiller',
  reschedule: 'Reprogrammer',
  settle: 'Régler',
  archive: 'Archiver',
  correct: 'Corriger résultat',
};

// ── Main Page ─────────────────────────────────────────────────────────────────

@Component({
  selector: 'tch-platform-ops-draws-page',
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
    TchSearchSelect,
    TchSectionError,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatMenuModule,
    MatSelectModule,
    MatTableModule,
    MatTooltipModule,
  ],
  templateUrl: './platform-ops-draws.page.html',
  styleUrls: ['./platform-ops-draws.page.scss'],
})
export class PlatformOpsDrawsPage implements OnInit {
  private readonly api = inject(PlatformOpsApi);
  private readonly tenantsApi = inject(PlatformTenantsApi);
  private readonly dialog = inject(MatDialog);
  private readonly translate = inject(TranslateService);

  readonly displayedColumns = ['select', 'drawDate', 'channel', 'slot', 'status', 'scheduledAt', 'lastResult', 'actions'];
  readonly statusOptions = STATUS_OPTIONS;

  readonly loading = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly actionError = signal<ErrorViewModel | null>(null);
  readonly actionNotice = signal<{ title: string; message: string } | null>(null);
  readonly draws = signal<DrawView[]>([]);
  readonly selectedTenant = signal<TenantSummaryView | null>(null);
  readonly statusFilter = signal('');
  readonly slotKeyFilter = signal('');
  readonly fromFilter = signal(relativeIsoDate(-1));
  readonly toFilter = signal(todayIsoDate());
  readonly deletedVisibility = signal<'active' | 'deleted' | 'all'>('active');
  readonly page = signal(0);
  readonly totalElements = signal(0);
  readonly totalPages = signal(1);
  readonly selectedIds = signal<ReadonlySet<string>>(new Set<string>());
  readonly selectedDraws = computed(() => this.draws().filter(draw => this.selectedIds().has(draw.id)));
  readonly commonBulkActions = computed(() => this.resolveCommonBulkActions(this.selectedDraws()));
  readonly bulkSelectionMessage = computed(() => this.resolveBulkSelectionMessage(this.selectedDraws()));
  readonly maxBulkDrawActions = MAX_BULK_DRAW_ACTIONS;

  toneForStatus = toneForStatus;
  actionsForDraw = actionsForDraw;
  actionIcon = (kind: string) => ACTION_ICON[kind] ?? 'settings';
  actionLabel = (kind: string) => DRAW_ACTION_LABELS[kind] ?? kind;

  private currentBatchDialog: BatchOpDialog | null = null;

  readonly searchTenants = (query: string): Observable<readonly TchSearchOption<TenantSummaryView>[]> =>
    this.tenantsApi.listTenants({ q: query, page: 0, size: 12, status: 'ACTIVE' }).pipe(
      map(page => page.items.map(tenant => this.toTenantOption(tenant))),
    );

  ngOnInit(): void {
    this.loadDefaultTenant();
  }

  load(): void {
    const tenantId = this.selectedTenantId();
    if (!tenantId) {
      this.error.set({
        title: 'Tenant requis',
        message: 'Sélectionnez un tenant pour afficher les tirages.',
        severity: 'warn',
      });
      this.loading.set(false);
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.actionError.set(null);
    this.api.listDraws({
      status: this.statusFilter() || undefined,
      resultSlotKey: this.slotKeyFilter() || undefined,
      from: this.fromFilter() || undefined,
      to: this.toFilter() || undefined,
      deletedVisibility: this.deletedVisibility(),
      page: this.page(),
      size: 25,
      suppressShellFeedback: true,
    }, tenantId).subscribe({
      next: p => {
        this.draws.set(p.items);
        this.selectedIds.set(new Set());
        this.totalElements.set(p.totalElements);
        this.totalPages.set(p.totalPages || 1);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.error.set(this.errorViewModel(err, 'platform.ops.draws.list'));
        this.loading.set(false);
      },
    });
  }

  onStatusChange(v: string): void { this.statusFilter.set(v); this.page.set(0); this.load(); }
  onSearch(v: string): void { this.slotKeyFilter.set(v); this.page.set(0); this.load(); }
  onFromChange(v: string): void { this.fromFilter.set(v); this.page.set(0); this.load(); }
  onToChange(v: string): void { this.toFilter.set(v); this.page.set(0); this.load(); }
  onDeletedVisibilityChange(v: 'active' | 'deleted' | 'all'): void { this.deletedVisibility.set(v); this.page.set(0); this.load(); }
  prevPage(): void { this.page.set(this.page() - 1); this.load(); }
  nextPage(): void { this.page.set(this.page() + 1); this.load(); }

  selectTenant(option: TchSearchOption | null): void {
    const tenant = option?.data as TenantSummaryView | undefined;
    if (!tenant) {
      this.loadDefaultTenant();
      return;
    }
    this.selectedTenant.set(tenant);
    this.page.set(0);
    this.load();
  }

  resetTenant(): void {
    this.loadDefaultTenant();
  }

  selectedTenantId(): string | null {
    const tenant = this.selectedTenant();
    return tenant?.id ?? tenant?.tenantId ?? null;
  }

  selectedTenantLabel(): string {
    const tenant = this.selectedTenant();
    return tenant ? `${tenant.name} (${tenant.code})` : 'Tenant non sélectionné';
  }

  isSelected(draw: DrawView): boolean {
    return this.selectedIds().has(draw.id);
  }

  toggleDraw(draw: DrawView, checked: boolean): void {
    const next = new Set(this.selectedIds());
    if (checked) next.add(draw.id);
    else next.delete(draw.id);
    this.selectedIds.set(next);
  }

  allPageSelected(): boolean {
    const rows = this.draws();
    return rows.length > 0 && rows.every(row => this.selectedIds().has(row.id));
  }

  somePageSelected(): boolean {
    const rows = this.draws();
    return rows.some(row => this.selectedIds().has(row.id)) && !this.allPageSelected();
  }

  togglePage(checked: boolean): void {
    const next = new Set(this.selectedIds());
    for (const draw of this.draws()) {
      if (checked) next.add(draw.id);
      else next.delete(draw.id);
    }
    this.selectedIds.set(next);
  }

  clearSelection(): void {
    this.selectedIds.set(new Set());
  }

  openBulkAction(action: BulkDrawActionType): void {
    const draws = this.selectedDraws();
    if (!draws.length) return;
    if (draws.length > MAX_BULK_DRAW_ACTIONS) {
      this.actionError.set({
        title: 'Sélection trop grande',
        message: `Maximum ${MAX_BULK_DRAW_ACTIONS} tirages par action. Réduisez la sélection.`,
        severity: 'warn',
      });
      return;
    }
    const tenantId = this.singleTenantSelectionId();
    if (!tenantId) {
      this.actionError.set({
        title: 'Sélection multi-tenant',
        message: 'Utilisez une action ops par journée/tenant.',
        severity: 'warn',
      });
      return;
    }
    this.actionError.set(null);
    this.actionNotice.set(null);
    if (action === 'cancel') {
      const ref = this.dialog.open(BulkCancelDrawDialog, {
        data: { draws, tenantId },
        width: '460px',
      });
      ref.afterClosed().subscribe((done: boolean | null) => {
        if (done) {
          this.actionNotice.set({
            title: 'Opération effectuée',
            message: `${draws.length} tirage(s) traité(s).`,
          });
          this.load();
        }
      });
      return;
    }
    const ref = this.dialog.open(BulkSimpleDrawActionDialog, {
      data: { draws, action, tenantId },
      width: '460px',
    });
    ref.afterClosed().subscribe((done: boolean | null) => {
      if (done) {
        this.actionNotice.set({
          title: 'Opération effectuée',
          message: `${draws.length} tirage(s) traité(s).`,
        });
        this.load();
      }
    });
  }

  private toTenantOption(tenant: TenantSummaryView): TchSearchOption<TenantSummaryView> {
    return {
      id: tenant.id ?? tenant.tenantId ?? tenant.code,
      title: tenant.name,
      subtitle: tenant.code,
      badge: tenant.status,
      icon: 'apartment',
      data: tenant,
    };
  }

  private loadDefaultTenant(): void {
    this.loading.set(true);
    this.error.set(null);
    this.tenantsApi.listTenants({ q: DEFAULT_DRAW_TENANT_CODE, page: 0, size: 10, status: 'ACTIVE' }).subscribe({
      next: page => {
        const tenants = page.items ?? [];
        const defaultTenant = tenants.find(tenant => tenant.code?.toLowerCase() === DEFAULT_DRAW_TENANT_CODE)
          ?? tenants[0]
          ?? null;
        if (!defaultTenant) {
          this.selectedTenant.set(null);
          this.draws.set([]);
          this.totalElements.set(0);
          this.totalPages.set(1);
          this.loading.set(false);
          this.error.set({
            title: 'Tenant introuvable',
            message: `Tenant par défaut ${DEFAULT_DRAW_TENANT_CODE} introuvable.`,
            severity: 'warn',
          });
          return;
        }
        this.selectedTenant.set(defaultTenant);
        this.page.set(0);
        this.load();
      },
      error: (err: unknown) => {
        this.loading.set(false);
        this.error.set(this.errorViewModel(err, 'platform.ops.draws.defaultTenant'));
      },
    });
  }

  private resolveCommonBulkActions(draws: DrawView[]): BulkDrawActionType[] {
    if (draws.length === 0 || draws.length > MAX_BULK_DRAW_ACTIONS || !this.singleTenantSelectionId()) return [];
    const allowed = draws.map(draw => actionsForDraw(draw)
      .map(action => action.kind)
      .filter((kind): kind is BulkDrawActionType => kind === 'cancel' || kind === 'lock' || kind === 'unlock' || kind === 'settle' || kind === 'archive'));
    return allowed.reduce<BulkDrawActionType[]>((common, current) => common.filter(kind => current.includes(kind)), allowed[0] ?? []);
  }

  private resolveBulkSelectionMessage(draws: DrawView[]): string {
    if (draws.length > MAX_BULK_DRAW_ACTIONS) {
      return `Maximum ${MAX_BULK_DRAW_ACTIONS} tirages par action. Réduisez la sélection.`;
    }
    if (draws.length > 0 && !this.singleTenantSelectionId()) {
      return 'Sélection multi-tenant: choisissez un tenant ou réduisez la sélection.';
    }
    return 'Aucune action commune pour ces statuts.';
  }

  private singleTenantSelectionId(): string | null {
    const tenantIds = new Set(this.selectedDraws().map(draw => draw.tenantId).filter(Boolean));
    return tenantIds.size === 1 ? [...tenantIds][0] : null;
  }

  // ── Bulk ops ──────────────────────────────────────────────────────────────

  openGenerate(): void {
    this.dialog.open(GenerateDrawsDialog, { width: '520px' });
  }

  openOpenToday(): void {
    this.openBatch('Ouvrir les tirages du jour', true, (tenantCodes, dryRun, limit) => {
      const req: OpenTodayDrawsRequest = { tenantCodes, limit, dryRun };
      this.api.openTodayDraws(req, { suppressShellFeedback: true }).subscribe({
        next: res => this.currentBatchDialog?.setResult(res as AnyBatchResult),
        error: (err: unknown) => this.currentBatchDialog?.setError(this.errorViewModel(err, 'platform.ops.draws.openToday')),
      });
    });
  }

  openCloseDue(): void {
    this.openBatch('Fermer les tirages échus', true, (tenantCodes, dryRun, limit) => {
      const req: CloseDueDrawsRequest = { tenantCodes, limit, dryRun };
      this.api.closeDueDraws(req, { suppressShellFeedback: true }).subscribe({
        next: res => this.currentBatchDialog?.setResult(res as AnyBatchResult),
        error: (err: unknown) => this.currentBatchDialog?.setError(this.errorViewModel(err, 'platform.ops.draws.closeDue')),
      });
    });
  }

  openApply(): void {
    this.dialog.open(ApplyResultsDialog, {
      data: { tenantCode: this.selectedTenant()?.code ?? null },
      width: '480px',
    });
  }

  private openBatch(
    title: string,
    hasLimit: boolean,
    execute: (tenantCodes: string[], dryRun: boolean, limit?: number) => void,
  ): void {
    const ref = this.dialog.open(BatchOpDialog, { data: { title, hasLimit, execute }, width: '500px' });
    this.currentBatchDialog = ref.componentInstance;
    ref.afterClosed().subscribe(() => { this.currentBatchDialog = null; });
  }

  // ── Row actions ──────────────────────────────────────────────────────────

  openRowAction(draw: DrawView, action: DrawActionItem): void {
    const tenantId = this.selectedTenantId() ?? draw.tenantId ?? null;
    switch (action.kind) {
      case 'cancel':
        this.openAndReload(CancelDrawDialog, { draw, tenantId }, '460px');
        break;
      case 'lock':
      case 'unlock':
      case 'settle':
      case 'archive':
        this.openAndReload(SimpleDrawActionDialog, { draw, action: action.kind, tenantId }, '440px');
        break;
      case 'reschedule':
        this.openAndReload(RescheduleDrawDialog, { draw, tenantId }, '480px');
        break;
      case 'correct':
        this.openAndReload(CorrectDrawResultDialog, { draw, tenantId }, '500px');
        break;
    }
  }

  private openAndReload(component: unknown, data: unknown, width: string): void {
    const ref = this.dialog.open(component as Parameters<MatDialog['open']>[0], { data, width });
    ref.afterClosed().subscribe((done: boolean | null) => {
      if (done) {
        this.actionNotice.set({
          title: 'Opération effectuée',
          message: 'La liste des tirages a été rafraîchie.',
        });
        this.load();
      }
    });
  }

  lotSummary(draw: DrawView): string {
    const r = draw.lastResult;
    if (!r) return '—';
    const parts = [r.lot1, r.lot2, r.lot3, r.lot4].filter(Boolean);
    return parts.length ? parts.join(' · ') : r.status;
  }

  lotteryAsset(slotKey: string): string | null {
    return lotteryAssetForSlot(slotKey);
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
