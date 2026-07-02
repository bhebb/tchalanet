import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { TranslateService } from '@ngx-translate/core';
import { Observable } from 'rxjs';
import {
  DrawResultDrawerComponent,
  DrawResultDrawerState,
} from '../../components/draw-result-drawer/draw-result-drawer.component';

import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { AuthSessionService } from '@tch/core/auth';
import { TchLoading, TchErrorPanel, TchSectionError } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { AdminPageShellComponent } from '@tch/ui/console';
import { AdminCrudShellComponent } from '@tch/ui/console';
import { AdminDataToolbarComponent } from '@tch/ui/console';
import { AdminEmptyStateComponent } from '@tch/ui/console';

import { AdminGeneratedDrawsApiService } from '../../data-access/admin-generated-draws-api.service';
import {
  DrawLifecycleAction,
  GeneratedDrawView,
  GeneratedDrawGroup,
  DatePreset,
  DrawStatusFilter,
  SaveDrawResultRequest,
} from '../../data-access/admin-generated-draws.models';
import { GeneratedDrawsSummaryComponent } from '../../components/generated-draws-summary/generated-draws-summary.component';
import { GeneratedDrawsTableComponent } from '../../components/generated-draws-table/generated-draws-table.component';
import { AdminDrawLifecycleDialog } from './dialogs/admin-draw-lifecycle.dialog';

type PageState = 'loading' | 'ready' | 'error';

const TODAY = new Date().toISOString().slice(0, 10);

@Component({
  selector: 'tch-admin-generated-draws-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatButtonModule,
    MatDialogModule,
    MatIconModule,
    MatMenuModule,
    AdminPageShellComponent,
    AdminCrudShellComponent,
    AdminDataToolbarComponent,
    AdminEmptyStateComponent,
    TchLoading,
    TchErrorPanel,
    TchSectionError,
    GeneratedDrawsSummaryComponent,
    GeneratedDrawsTableComponent,
    DrawResultDrawerComponent,
  ],
  templateUrl: './admin-generated-draws.page.html',
  styleUrls: ['./admin-generated-draws.page.scss'],
})
export class AdminGeneratedDrawsPage implements OnInit {
  private readonly api      = inject(AdminGeneratedDrawsApiService);
  private readonly router   = inject(Router);
  private readonly route    = inject(ActivatedRoute);
  private readonly dialog   = inject(MatDialog);
  private readonly translate = inject(TranslateService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly auth = inject(AuthSessionService);

  readonly today = TODAY;

  readonly pageState     = signal<PageState>('loading');
  readonly pageError     = signal<ErrorViewModel | null>(null);
  readonly actionError   = signal<ErrorViewModel | null>(null);
  readonly actionNotice  = signal<string | null>(null);
  readonly allDraws      = signal<GeneratedDrawView[]>([]);
  readonly totalElements = signal<number>(0);
  readonly page          = signal<number>(0);
  readonly busy          = signal<boolean>(false);
  readonly selectedDraw  = signal<GeneratedDrawView | null>(null);
  readonly resultSaveState = signal<DrawResultDrawerState>('ready');
  readonly resultSaveMessage = signal<string | null>(null);

  readonly datePreset    = signal<DatePreset>('TODAY');
  readonly statusFilter  = signal<DrawStatusFilter>('all');
  readonly searchQuery   = signal<string>('');

  readonly statusFilters: { key: DrawStatusFilter; label: string }[] = [
    { key: 'all',          label: 'Tous les statuts' },
    { key: 'OPEN',         label: 'Ouverts à la vente' },
    { key: 'PAST',         label: 'Terminés / à traiter' },
    { key: 'EXPECTED',     label: 'Résultats attendus' },
    { key: 'MISSING',      label: 'Résultats manquants' },
    { key: 'CONFIRMED',    label: 'Confirmés' },
    { key: 'SOURCE_ERROR', label: 'Erreur source' },
  ];

  readonly datePresets: { key: DatePreset; label: string }[] = [
    { key: 'TODAY',     label: "Aujourd'hui" },
    { key: 'TOMORROW',  label: 'Demain' },
    { key: 'THIS_WEEK', label: 'Cette semaine' },
  ];

  readonly groupedDraws = computed<GeneratedDrawGroup[]>(() => {
    const map = new Map<string, GeneratedDrawView[]>();
    for (const draw of this.allDraws()) {
      const drawsForDate = map.get(draw.businessDate) ?? [];
      drawsForDate.push(draw);
      map.set(draw.businessDate, drawsForDate);
    }
    return Array.from(map.entries())
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([date, draws]) => ({ date, draws }));
  });

  ngOnInit(): void {
    this.route.queryParamMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(params => {
        const nextStatus = this.statusFilterFromQuery(params.get('status'));
        if (nextStatus !== this.statusFilter()) {
          this.statusFilter.set(nextStatus);
          this.page.set(0);
        }
        this.load();
      });
  }

  load(): void {
    this.pageState.set('loading');
    this.pageError.set(null);
    this.actionError.set(null);
    this.actionNotice.set(null);
    this.api.getGeneratedDraws({
      datePreset: this.datePreset(),
      status: this.statusFilter() === 'all' ? null : this.statusFilter(),
      q: this.searchQuery() || null,
      page: this.page(),
    }, { suppressShellFeedback: true }).subscribe({
      next: result => {
        this.allDraws.set(result.content);
        this.totalElements.set(result.totalElements);
        this.pageState.set('ready');
      },
      error: (err: unknown) => {
        this.pageError.set(this.errorViewModel(err, 'admin.generatedDraws.list', 'page'));
        this.pageState.set('error');
      },
    });
  }

  onDatePreset(preset: DatePreset): void {
    this.datePreset.set(preset);
    this.page.set(0);
    this.load();
  }

  onStatusFilter(status: DrawStatusFilter): void {
    this.statusFilter.set(status);
    this.page.set(0);
    this.load();
  }

  onSearch(query: string): void {
    this.searchQuery.set(query);
    this.page.set(0);
    this.load();
  }

  onEnterResult(draw: GeneratedDrawView): void  { this.openResultDrawer(draw); }
  onViewResult(draw: GeneratedDrawView): void   { this.openResultDrawer(draw); }
  onVerifySource(draw: GeneratedDrawView): void  { this.openResultDrawer(draw); }

  onViewDetails(draw: GeneratedDrawView): void {
    this.router.navigate(['/app/admin/draws', draw.drawId]);
  }

  onDrawerClosed(): void {
    this.selectedDraw.set(null);
    this.resetResultSaveState();
  }

  onResultSaved(updated: GeneratedDrawView): void {
    this.allDraws.update(draws =>
      draws.map(d => d.drawId === updated.drawId ? updated : d),
    );
    this.selectedDraw.set(updated);
  }

  onResultSaveRequested(request: SaveDrawResultRequest): void {
    this.resultSaveState.set('saving');
    this.resultSaveMessage.set(null);
    this.api.saveDrawResult(request, { suppressShellFeedback: true }).subscribe({
      next: updated => {
        this.onResultSaved(updated);
        this.resultSaveState.set('success');
        this.resultSaveMessage.set(
          request.mode === 'confirmed'
            ? 'Résultat confirmé et publié.'
            : 'Résultat enregistré en provisoire.',
        );
      },
      error: () => {
        this.resultSaveState.set('error');
        this.resultSaveMessage.set('Impossible d\'enregistrer le résultat.');
      },
    });
  }

  canSaveProvisionalResult(draw: GeneratedDrawView): boolean {
    return this.hasNoResult(draw) && this.isManualResultDue(draw);
  }

  canSaveConfirmedResult(draw: GeneratedDrawView): boolean {
    return this.auth.hasRole('SUPER_ADMIN') && this.hasNoResult(draw) && this.isManualResultDue(draw);
  }

  canOverrideResult(draw: GeneratedDrawView): boolean {
    return this.auth.hasRole('SUPER_ADMIN') && this.hasResult(draw);
  }

  resultUnavailableReason(draw: GeneratedDrawView): string | null {
    if (this.hasResult(draw)) {
      return this.auth.hasRole('SUPER_ADMIN')
        ? null
        : 'Le résultat existe déjà. Seul un super admin peut le corriger.';
    }

    if (!this.isManualResultDue(draw)) {
      return 'La saisie manuelle sera disponible 30 minutes après l’heure prévue du tirage.';
    }

    return null;
  }

  onConfigureDrawChannels(): void {
    this.router.navigate(['/app/admin/draws/channels']);
  }

  onNextPage(): void { this.page.update(p => p + 1); this.load(); }
  onPrevPage(): void { this.page.update(p => Math.max(0, p - 1)); this.load(); }

  // ── Lifecycle actions ──────────────────────────────────────────────────────

  onOpenDraw(draw: GeneratedDrawView): void   { this.openLifecycleDialog(draw, 'open'); }
  onCloseDraw(draw: GeneratedDrawView): void  { this.openLifecycleDialog(draw, 'close'); }
  onLockDraw(draw: GeneratedDrawView): void   { this.openLifecycleDialog(draw, 'lock'); }
  onUnlockDraw(draw: GeneratedDrawView): void { this.openLifecycleDialog(draw, 'unlock'); }
  onCancelDraw(draw: GeneratedDrawView): void { this.openLifecycleDialog(draw, 'cancel'); }
  onArchiveDraw(draw: GeneratedDrawView): void { this.openLifecycleDialog(draw, 'archive'); }
  onBulkLifecycle(event: { action: DrawLifecycleAction; draws: readonly GeneratedDrawView[] }): void {
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
      this.executeLifecycleAction(drawOrDraws, action, result.reason);
    });
  }

  private executeLifecycleAction(
    drawOrDraws: GeneratedDrawView | readonly GeneratedDrawView[],
    action: DrawLifecycleAction,
    reason?: string,
  ): void {
    this.busy.set(true);
    this.actionError.set(null);
    this.actionNotice.set(null);
    const draws = Array.isArray(drawOrDraws) ? drawOrDraws : [drawOrDraws];
    let call$: Observable<GeneratedDrawView | GeneratedDrawView[]>;

    if (draws.length > 1) {
      call$ = this.api.lifecycleDraws(
        action,
        draws.map(draw => draw.drawId),
        reason,
        { suppressShellFeedback: true },
      );
    } else {
      const [draw] = draws;
      switch (action) {
        case 'open':
          call$ = this.api.openDraw(draw.drawId, reason, { suppressShellFeedback: true });
          break;
        case 'close':
          call$ = this.api.closeDraw(draw.drawId, reason, { suppressShellFeedback: true });
          break;
        case 'cancel':
          call$ = this.api.cancelDraw(draw.drawId, reason, { suppressShellFeedback: true });
          break;
        case 'lock':
          call$ = this.api.lockDraw(draw.drawId, reason, { suppressShellFeedback: true });
          break;
        case 'unlock':
          call$ = this.api.unlockDraw(draw.drawId, reason, { suppressShellFeedback: true });
          break;
        case 'archive':
          call$ = this.api.archiveDraw(draw.drawId, reason, { suppressShellFeedback: true });
          break;
      }
    }

    call$!.subscribe({
      next: updatedValue => {
        this.busy.set(false);
        const updatedDraws = Array.isArray(updatedValue) ? updatedValue : [updatedValue];
        this.allDraws.update(current =>
          current.map(draw => updatedDraws.find(updated => updated.drawId === draw.drawId) ?? draw),
        );
        const labels: Record<DrawLifecycleAction, string> = {
          open: 'Ouvert',
          close: 'Fermé',
          cancel: 'Annulé',
          lock: 'Verrouillé',
          unlock: 'Déverrouillé',
          archive: 'Archivé',
        };
        this.actionNotice.set(
          updatedDraws.length > 1
            ? `${updatedDraws.length} tirages mis à jour avec succès.`
            : `Tirage ${labels[action]} avec succès.`,
        );
      },
      error: (err: unknown) => {
        this.busy.set(false);
        this.actionError.set(this.errorViewModel(err, `admin.generatedDraws.lifecycle.${action}`, 'section'));
      },
    });
  }

  private errorViewModel(
    err: unknown,
    source: string,
    surface: 'page' | 'section',
  ): ErrorViewModel {
    const problem = (err as { error?: ProblemDetail })?.error;
    if (problem) {
      const normalized = webAppErrorFromProblemDetail(problem, source, surface);
      const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
      return toErrorViewModel(normalized, copy);
    }

    return {
      title: this.translate.instant('common.errors.fallback.title'),
      message: this.translate.instant('common.errors.fallback.message'),
      severity: 'error',
    };
  }

  private statusFilterFromQuery(value: string | null): DrawStatusFilter {
    switch (value?.trim().toUpperCase()) {
      case 'OPEN': return 'OPEN';
      case 'PAST': return 'PAST';
      case 'EXPECTED': return 'EXPECTED';
      case 'MISSING': return 'MISSING';
      case 'CONFIRMED': return 'CONFIRMED';
      case 'SOURCE_ERROR': return 'SOURCE_ERROR';
      default: return 'all';
    }
  }

  private hasResult(draw: GeneratedDrawView): boolean {
    return draw.resultStatus === 'PROVISIONAL'
      || draw.resultStatus === 'CONFIRMED'
      || (draw.numbers?.length ?? 0) > 0;
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
    this.resetResultSaveState();
    this.selectedDraw.set(draw);
  }

  private resetResultSaveState(): void {
    this.resultSaveState.set('ready');
    this.resultSaveMessage.set(null);
  }
}
