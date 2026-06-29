import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { TranslateService } from '@ngx-translate/core';
import { Observable } from 'rxjs';
import { DrawResultDrawerComponent } from '../../components/draw-result-drawer/draw-result-drawer.component';

import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
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
  private readonly dialog   = inject(MatDialog);
  private readonly translate = inject(TranslateService);

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

  readonly datePreset    = signal<DatePreset>('TODAY');
  readonly statusFilter  = signal<DrawStatusFilter>('all');
  readonly searchQuery   = signal<string>('');

  readonly statusFilters: { key: DrawStatusFilter; label: string }[] = [
    { key: 'all',          label: 'Tous les statuts' },
    { key: 'OPEN',         label: 'Ouverts à la vente' },
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

  ngOnInit(): void { this.load(); }

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

  onEnterResult(draw: GeneratedDrawView): void  { this.selectedDraw.set(draw); }
  onViewResult(draw: GeneratedDrawView): void   { this.selectedDraw.set(draw); }
  onVerifySource(draw: GeneratedDrawView): void  { this.selectedDraw.set(draw); }

  onViewDetails(draw: GeneratedDrawView): void {
    this.router.navigate(['/app/admin/draws', draw.drawId]);
  }

  onDrawerClosed(): void { this.selectedDraw.set(null); }

  onResultSaved(updated: GeneratedDrawView): void {
    this.allDraws.update(draws =>
      draws.map(d => d.drawId === updated.drawId ? updated : d),
    );
  }

  onConfigureDrawChannels(): void {
    this.router.navigate(['/app/admin/draw-channels']);
  }

  onNextPage(): void { this.page.update(p => p + 1); this.load(); }
  onPrevPage(): void { this.page.update(p => Math.max(0, p - 1)); this.load(); }

  // ── Lifecycle actions ──────────────────────────────────────────────────────

  onLockDraw(draw: GeneratedDrawView): void   { this.openLifecycleDialog(draw, 'lock'); }
  onUnlockDraw(draw: GeneratedDrawView): void { this.openLifecycleDialog(draw, 'unlock'); }
  onCancelDraw(draw: GeneratedDrawView): void { this.openLifecycleDialog(draw, 'cancel'); }
  onArchiveDraw(draw: GeneratedDrawView): void { this.openLifecycleDialog(draw, 'archive'); }

  private openLifecycleDialog(draw: GeneratedDrawView, action: DrawLifecycleAction): void {
    const ref = this.dialog.open(AdminDrawLifecycleDialog, {
      data: { draw, action },
      width: '420px',
    });

    ref.afterClosed().subscribe((result: { reason?: string } | null) => {
      if (result == null) return;
      this.executeLifecycleAction(draw, action, result.reason);
    });
  }

  private executeLifecycleAction(
    draw: GeneratedDrawView,
    action: DrawLifecycleAction,
    reason?: string,
  ): void {
    this.busy.set(true);
    this.actionError.set(null);
    this.actionNotice.set(null);
    let call$: Observable<GeneratedDrawView>;

    switch (action) {
      case 'cancel':
        call$ = this.api.cancelDraw(draw.drawId, reason ?? 'ADMIN_REQUEST', { suppressShellFeedback: true });
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

    call$.subscribe({
      next: updated => {
        this.busy.set(false);
        this.allDraws.update(draws =>
          draws.map(d => d.drawId === updated.drawId ? updated : d),
        );
        const labels: Record<DrawLifecycleAction, string> = {
          cancel: 'Annulé', lock: 'Verrouillé', unlock: 'Déverrouillé', archive: 'Archivé',
        };
        this.actionNotice.set(`Tirage ${labels[action]} avec succès.`);
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
}
