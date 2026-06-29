import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { TranslateService } from '@ngx-translate/core';

import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { TchLoading, TchErrorPanel, TchSectionError } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '../../../../../core/api/error-feedback-copy';
import { ErrorViewModel, toErrorViewModel } from '../../../../../core/api/local-error-routing';
import { AdminPageShellComponent } from '../../../shared/admin-ui/admin-page-shell.component';
import { AdminEmptyStateComponent } from '../../../shared/admin-ui/admin-empty-state.component';
import { AdminCrudShellComponent } from '../../../shared/admin-ui/admin-crud-shell.component';
import { AdminDataToolbarComponent } from '../../../shared/admin-ui/admin-data-toolbar.component';
import { AdminStatusPillComponent } from '../../../shared/admin-ui/admin-status-pill.component';
import {
  PlatformOpsApi,
  DrawResultOpsResponse,
  HaitiLots,
  OpsDrawResultQuality,
} from '../../platform-ops-api.service';
import { FetchResultsDialog } from './dialogs/fetch-results.dialog';
import { ManualResultDialog } from './dialogs/manual-result.dialog';
import { OverrideResultDialog } from './dialogs/override-result.dialog';
import { lotteryAssetForSlot } from '../../../../../shared/lottery/lottery-assets';
import { HaitiLotsDisplayComponent } from '../../../../../shared/results/haiti-lots-display.component';

const RESULT_STATUS_OPTIONS = [
  { value: '', label: 'Tous les statuts' },
  { value: 'PROVISIONAL', label: 'Provisoire' },
  { value: 'CONFIRMED', label: 'Confirmé' },
  { value: 'OVERRIDDEN', label: 'Override' },
  { value: 'ERROR', label: 'Erreur' },
];

const RESULT_QUALITY_OPTIONS: { value: OpsDrawResultQuality | ''; label: string }[] = [
  { value: '', label: 'Toutes qualités' },
  { value: 'COMPLETE', label: 'Complète' },
  { value: 'SUSPECT', label: 'Suspecte' },
  { value: 'INVALID', label: 'Invalide' },
];

@Component({
  selector: 'tch-platform-ops-draw-results-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    AdminPageShellComponent,
    AdminEmptyStateComponent,
    AdminCrudShellComponent,
    AdminDataToolbarComponent,
    AdminStatusPillComponent,
    TchLoading,
    TchErrorPanel,
    TchSectionError,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatSelectModule,
    MatTableModule,
    HaitiLotsDisplayComponent,
  ],
  templateUrl: './platform-ops-draw-results.page.html',
  styleUrls: ['./platform-ops-draw-results.page.scss'],
})
export class PlatformOpsDrawResultsPage implements OnInit {
  private readonly api = inject(PlatformOpsApi);
  private readonly dialog = inject(MatDialog);
  private readonly translate = inject(TranslateService);

  readonly displayedColumns = ['lottery', 'slotKey', 'occurredAt', 'haiti', 'status', 'source', 'quality', 'fetchedAt', 'actions'];
  readonly loading = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly actionFeedback = signal<ErrorViewModel | null>(null);
  readonly page = signal<{ items: DrawResultOpsResponse[]; totalElements: number; totalPages: number; number: number; size: number } | null>(null);
  readonly pageIndex = signal(0);
  readonly pageSize = signal(20);
  readonly actionLoading = signal(false);
  readonly slotKeyFilter = signal('');
  readonly statusFilter = signal('');
  readonly qualityFilter = signal<OpsDrawResultQuality | ''>('');
  readonly fromFilter = signal('');
  readonly toFilter = signal('');
  readonly statusOptions = RESULT_STATUS_OPTIONS;
  readonly qualityOptions = RESULT_QUALITY_OPTIONS;

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.listDrawResults({
      slotKey: this.slotKeyFilter() || undefined,
      status: this.statusFilter() || undefined,
      quality: this.qualityFilter() || undefined,
      from: this.fromFilter() || undefined,
      to: this.toFilter() || undefined,
      page: this.pageIndex(),
      size: this.pageSize(),
      sort: 'occurredAt,DESC',
    }, { suppressShellFeedback: true }).subscribe({
      next: v => { this.page.set(v); this.loading.set(false); },
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

  onSlotSearch(v: string): void { this.slotKeyFilter.set(v.trim()); this.pageIndex.set(0); this.load(); }
  onStatusChange(v: string): void { this.statusFilter.set(v); this.pageIndex.set(0); this.load(); }
  onQualityChange(v: OpsDrawResultQuality | ''): void { this.qualityFilter.set(v); this.pageIndex.set(0); this.load(); }
  onFromChange(v: string): void { this.fromFilter.set(v); this.pageIndex.set(0); this.load(); }
  onToChange(v: string): void { this.toFilter.set(v); this.pageIndex.set(0); this.load(); }

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

  statusTone(status: string): 'success' | 'warning' | 'danger' | 'neutral' | 'info' {
    const map: Record<string, 'success' | 'warning' | 'danger' | 'neutral' | 'info'> = {
      CONFIRMED: 'success',
      PROVISIONAL: 'warning',
      OVERRIDDEN: 'info',
      MANUAL: 'info',
      REJECTED: 'danger',
    };
    return map[status] ?? 'neutral';
  }

  qualityTone(quality: string): 'success' | 'warning' | 'danger' | 'neutral' {
    const map: Record<string, 'success' | 'warning' | 'danger' | 'neutral'> = {
      COMPLETE: 'success',
      SUSPECT: 'warning',
      INVALID: 'danger',
    };
    return map[quality] ?? 'neutral';
  }

  lotteryAsset(slotKey: string): string | null {
    return lotteryAssetForSlot(slotKey);
  }

  haitiLots(row: DrawResultOpsResponse): HaitiLots {
    const value = row.haitiResult;
    if (!value || typeof value !== 'object') return {};
    return value as HaitiLots;
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
