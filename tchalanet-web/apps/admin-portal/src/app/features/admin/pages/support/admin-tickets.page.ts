import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { TranslateService } from '@ngx-translate/core';
import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';

import { TchLoading, TchErrorPanel } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { AdminCrudShellComponent } from '@tch/ui/console';
import { AdminDataToolbarComponent } from '@tch/ui/console';
import { AdminEmptyStateComponent } from '@tch/ui/console';
import { AdminPageShellComponent } from '@tch/ui/console';
import {
  AdminStatusPillComponent,
  AdminStatusTone,
} from '@tch/ui/console';
import { AdminTicketsApi, TicketRowView, TicketStatus } from '../../admin-tickets-api.service';

@Component({
  selector: 'tch-admin-tickets-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    RouterLink,
    AdminPageShellComponent,
    AdminCrudShellComponent,
    AdminDataToolbarComponent,
    AdminEmptyStateComponent,
    AdminStatusPillComponent,
    TchLoading,
    TchErrorPanel,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatTableModule,
  ],
  templateUrl: './admin-tickets.page.html',
  styleUrls: ['./admin-tickets.page.scss'],
})
export class AdminTicketsPage implements OnInit {
  private readonly api = inject(AdminTicketsApi);
  private readonly translate = inject(TranslateService);

  readonly columns = ['ticketCode', 'status', 'drawChannelName', 'drawScheduledAt', 'totalAmountCents', 'placedAt'];

  readonly loading = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly items = signal<TicketRowView[]>([]);
  readonly total = signal(0);
  readonly page = signal(0);
  readonly statusFilter = signal<TicketStatus | ''>('');

  readonly totalPages = computed(() => Math.max(1, Math.ceil(this.total() / 20)));

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.list({
      status: this.statusFilter() || undefined,
      page: this.page(),
      size: 20,
    }, { suppressShellFeedback: true }).subscribe({
      next: p => {
        this.items.set(p.items);
        this.total.set(p.totalElements);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.error.set(this.errorViewModel(err, 'admin.tickets.list'));
        this.loading.set(false);
      },
    });
  }

  onStatusFilter(status: TicketStatus | ''): void {
    this.statusFilter.set(status);
    this.page.set(0);
    this.load();
  }

  prevPage(): void {
    if (this.page() > 0) { this.page.update(p => p - 1); this.load(); }
  }

  nextPage(): void {
    if (this.page() + 1 < this.totalPages()) { this.page.update(p => p + 1); this.load(); }
  }

  statusTone(status: TicketStatus): AdminStatusTone {
    switch (status) {
      case 'PLACED': return 'neutral';
      case 'PAID': return 'success';
      case 'CANCELLED': return 'danger';
      case 'EXPIRED': return 'warning';
      default: return 'neutral';
    }
  }

  amountDisplay(cents: number): string {
    return (cents / 100).toFixed(2);
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
