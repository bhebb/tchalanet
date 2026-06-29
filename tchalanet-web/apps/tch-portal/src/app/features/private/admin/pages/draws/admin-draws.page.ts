import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';
import { TranslateService } from '@ngx-translate/core';

import { webAppErrorFromProblemDetail } from '@tch/api';
import type { ProblemDetail } from '@tch/api';
import { TchLoading, TchErrorPanel } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { AdminPageShellComponent } from '../../../shared/admin-ui/admin-page-shell.component';
import { AdminEmptyStateComponent } from '../../../shared/admin-ui/admin-empty-state.component';
import {
  AdminStatusPillComponent,
  AdminStatusTone,
} from '../../../shared/admin-ui/admin-status-pill.component';
import { AdminDrawsApi, DrawSummaryView, DrawStatus } from '../../admin-draws-api.service';

@Component({
  selector: 'tch-admin-draws-page',
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
    MatTabsModule,
  ],
  templateUrl: './admin-draws.page.html',
  styleUrls: ['./admin-draws.page.scss'],
})
export class AdminDrawsPage implements OnInit {
  private readonly api = inject(AdminDrawsApi);
  private readonly translate = inject(TranslateService);

  readonly columns = ['channel', 'slot', 'drawDate', 'scheduledAt', 'cutoffAt', 'status'];

  readonly loadingToday = signal(false);
  readonly errorToday = signal<string | null>(null);
  readonly today = signal<DrawSummaryView[]>([]);

  readonly loadingUpcoming = signal(false);
  readonly errorUpcoming = signal<string | null>(null);
  readonly upcoming = signal<DrawSummaryView[]>([]);

  readonly loadingAll = signal(false);
  readonly errorAll = signal<string | null>(null);
  readonly all = signal<DrawSummaryView[]>([]);

  ngOnInit(): void {
    this.loadToday();
    this.loadUpcoming();
    this.loadAll();
  }

  loadToday(): void {
    this.loadingToday.set(true);
    this.errorToday.set(null);
    this.api.listToday({ size: 50 }, { suppressShellFeedback: true }).subscribe({
      next: p => { this.today.set(p.content); this.loadingToday.set(false); },
      error: (err: unknown) => {
        this.errorToday.set(this.errorTitle((err as { error?: ProblemDetail })?.error, 'admin.draws.today'));
        this.loadingToday.set(false);
      },
    });
  }

  loadUpcoming(): void {
    this.loadingUpcoming.set(true);
    this.errorUpcoming.set(null);
    this.api.listUpcoming({ days: 7, size: 50 }, { suppressShellFeedback: true }).subscribe({
      next: p => { this.upcoming.set(p.content); this.loadingUpcoming.set(false); },
      error: (err: unknown) => {
        this.errorUpcoming.set(this.errorTitle((err as { error?: ProblemDetail })?.error, 'admin.draws.upcoming'));
        this.loadingUpcoming.set(false);
      },
    });
  }

  loadAll(): void {
    this.loadingAll.set(true);
    this.errorAll.set(null);
    this.api.list({ size: 50 }, { suppressShellFeedback: true }).subscribe({
      next: p => { this.all.set(p.content); this.loadingAll.set(false); },
      error: (err: unknown) => {
        this.errorAll.set(this.errorTitle((err as { error?: ProblemDetail })?.error, 'admin.draws.all'));
        this.loadingAll.set(false);
      },
    });
  }

  statusTone(status: DrawStatus): AdminStatusTone {
    switch (status) {
      case 'OPEN': return 'success';
      case 'LOCKED': return 'warning';
      case 'PENDING_RESULTS': return 'warning';
      case 'RESULTS_APPLIED': return 'success';
      case 'SETTLED': return 'success';
      case 'CANCELLED': return 'danger';
      case 'ARCHIVED': return 'neutral';
      default: return 'neutral';
    }
  }

  private errorTitle(problem: ProblemDetail | undefined, source: string): string {
    if (!problem) return this.translate.instant('common.errors.categories.unexpected.title');

    const normalized = webAppErrorFromProblemDetail(problem, source, 'section');
    return resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key)).title;
  }
}
