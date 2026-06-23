import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DrawResultDrawerComponent } from '../../components/draw-result-drawer/draw-result-drawer.component';

import { TchLoading, TchErrorPanel } from '@tch/ui/components';
import { AdminPageShellComponent } from '../../../../shared/admin-ui/admin-page-shell.component';
import { AdminCrudShellComponent } from '../../../../shared/admin-ui/admin-crud-shell.component';
import { AdminDataToolbarComponent } from '../../../../shared/admin-ui/admin-data-toolbar.component';
import { AdminEmptyStateComponent } from '../../../../shared/admin-ui/admin-empty-state.component';

import { AdminGeneratedDrawsApiService } from '../../data-access/admin-generated-draws-api.service';
import {
  GeneratedDrawView,
  GeneratedDrawGroup,
  DatePreset,
  DrawStatusFilter,
} from '../../data-access/admin-generated-draws.models';
import { GeneratedDrawsSummaryComponent } from '../../components/generated-draws-summary/generated-draws-summary.component';
import { GeneratedDrawsTableComponent } from '../../components/generated-draws-table/generated-draws-table.component';

type PageState = 'loading' | 'ready' | 'error';

const TODAY = new Date().toISOString().slice(0, 10);

@Component({
  selector: 'tch-admin-generated-draws-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    MatButtonModule,
    MatMenuModule,
    AdminPageShellComponent,
    AdminCrudShellComponent,
    AdminDataToolbarComponent,
    AdminEmptyStateComponent,
    TchLoading,
    TchErrorPanel,
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
  private readonly snackBar = inject(MatSnackBar);

  readonly today = TODAY;

  readonly pageState     = signal<PageState>('loading');
  readonly pageError     = signal<string | null>(null);
  readonly allDraws      = signal<GeneratedDrawView[]>([]);
  readonly totalElements = signal<number>(0);
  readonly page          = signal<number>(0);
  readonly planning      = signal<boolean>(false);
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
      if (!map.has(draw.businessDate)) map.set(draw.businessDate, []);
      map.get(draw.businessDate)!.push(draw);
    }
    return Array.from(map.entries())
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([date, draws]) => ({ date, draws }));
  });

  ngOnInit(): void { this.load(); }

  load(): void {
    this.pageState.set('loading');
    this.pageError.set(null);
    this.api.getGeneratedDraws({
      datePreset: this.datePreset(),
      status: this.statusFilter() === 'all' ? null : this.statusFilter(),
      q: this.searchQuery() || null,
      page: this.page(),
    }).subscribe({
      next: result => {
        this.allDraws.set(result.content);
        this.totalElements.set(result.totalElements);
        this.pageState.set('ready');
      },
      error: (err: unknown) => {
        this.pageError.set((err as { error?: { title?: string } })?.error?.title ?? 'Erreur de chargement.');
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

  onPlanNextDraws(): void {
    this.planning.set(true);
    this.api.planNextDraws({ daysAhead: 7 }).subscribe({
      next: result => {
        this.planning.set(false);
        this.snackBar.open(
          `${result.createdCount} tirage(s) planifié(s) du ${result.rangeStart} au ${result.rangeEnd}.`,
          'OK', { duration: 5000 },
        );
        this.load();
      },
      error: () => {
        this.planning.set(false);
        this.snackBar.open('Erreur lors de la planification.', 'OK', { duration: 4000 });
      },
    });
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
}
