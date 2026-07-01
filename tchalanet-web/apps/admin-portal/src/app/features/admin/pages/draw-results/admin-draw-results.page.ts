import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { TranslateService } from '@ngx-translate/core';
import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';

import { TchLoading, TchErrorPanel } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { AdminPageShellComponent } from '@tch/ui/console';
import { AdminEmptyStateComponent } from '@tch/ui/console';
import {
  AdminStatusPillComponent,
  AdminStatusTone,
} from '@tch/ui/console';
import {
  AdminDrawResultsApi,
  DrawResultView,
  DrawResultStatus,
  DrawResultQuality,
} from '../../admin-draw-results-api.service';
import { lotteryLogoForSlot, lotteryProviderCodeFromSlot } from '../../../../shared/lottery/lottery-assets';

@Component({
  selector: 'tch-admin-draw-results-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    AdminPageShellComponent,
    AdminEmptyStateComponent,
    AdminStatusPillComponent,
    TchLoading,
    TchErrorPanel,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatTableModule,
  ],
  templateUrl: './admin-draw-results.page.html',
  styleUrls: ['./admin-draw-results.page.scss'],
})
export class AdminDrawResultsPage implements OnInit {
  private readonly api = inject(AdminDrawResultsApi);
  private readonly translate = inject(TranslateService);

  readonly columns = ['source', 'draw', 'numbers', 'status', 'quality', 'fetchedAt', 'appliedAt'];

  readonly loading = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly results = signal<DrawResultView[]>([]);
  readonly statusFilter = signal<DrawResultStatus | ''>('');
  readonly qualityFilter = signal<DrawResultQuality | ''>('');

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.listLastDays(30, {
      status: this.statusFilter() || undefined,
      quality: this.qualityFilter() || undefined,
      size: 100,
    }, { suppressShellFeedback: true }).subscribe({
      next: p => { this.results.set(p.items); this.loading.set(false); },
      error: (err: unknown) => {
        this.error.set(this.errorViewModel(err));
        this.loading.set(false);
      },
    });
  }

  onStatusFilter(status: DrawResultStatus | ''): void {
    this.statusFilter.set(status);
    this.load();
  }

  onQualityFilter(quality: DrawResultQuality | ''): void {
    this.qualityFilter.set(quality);
    this.load();
  }

  statusTone(status: DrawResultStatus): AdminStatusTone {
    switch (status) {
      case 'APPLIED': return 'success';
      case 'CORRECTED': return 'warning';
      case 'VOIDED': return 'danger';
      default: return 'neutral';
    }
  }

  qualityTone(quality: DrawResultQuality): AdminStatusTone {
    switch (quality) {
      case 'OFFICIAL': return 'success';
      case 'MANUAL': return 'warning';
      case 'ESTIMATED': return 'warning';
      default: return 'neutral';
    }
  }

  providerLogo(row: DrawResultView): string | null {
    return lotteryLogoForSlot(row.slotKey);
  }

  providerCode(row: DrawResultView): string {
    return lotteryProviderCodeFromSlot(row.slotKey)?.toUpperCase() ?? row.channelCode;
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
}
