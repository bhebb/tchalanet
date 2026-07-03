import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { TranslatePipe } from '@ngx-translate/core';
import { TchPage } from '@tch/api';

import { AdminCrudShellComponent } from '@tch/ui/console';
import { AdminDataToolbarComponent } from '@tch/ui/console';
import { AdminEmptyStateComponent } from '@tch/ui/console';
import { AdminPageShellComponent } from '@tch/ui/console';
import { TchPaginationComponent } from '@tch/ui/console';
import {
  AdminStatusPillComponent,
  AdminStatusTone,
} from '@tch/ui/console';
import {
  TchAsyncReadyDirective,
  TchAsyncViewComponent,
  resourceErrorVm,
} from '@tch/web/async';
import { AdminTicketsApi, TicketRowView, TicketStatus } from '../../admin-tickets-api.service';
import {
  TICKET_STATUS_VALUES,
  isTicketStatus,
  ticketStatusLabelKey,
  ticketStatusTone,
} from '../../admin-ticket-status.util';

const DEFAULT_PAGE_SIZE = 20;
const SORT_VALUES = [
  'createdAt,DESC',
  'createdAt,ASC',
  'totalAmount,DESC',
  'totalAmount,ASC',
  'ticketCode,ASC',
  'ticketCode,DESC',
] as const;
type TicketSort = typeof SORT_VALUES[number];

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
    TchPaginationComponent,
    TchAsyncReadyDirective,
    TchAsyncViewComponent,
    TranslatePipe,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatTableModule,
  ],
  templateUrl: './admin-tickets.page.html',
  styleUrls: ['./admin-tickets.page.scss'],
})
export class AdminTicketsPage {
  private readonly api = inject(AdminTicketsApi);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly columns = ['ticketCode', 'status', 'drawChannelName', 'drawScheduledAt', 'totalAmountCents', 'placedAt'];
  readonly statusOptions = TICKET_STATUS_VALUES;
  readonly sortOptions: readonly { value: TicketSort; labelKey: string }[] = [
    { value: 'createdAt,DESC', labelKey: 'admin.tickets.list.sort.createdDesc' },
    { value: 'createdAt,ASC', labelKey: 'admin.tickets.list.sort.createdAsc' },
    { value: 'totalAmount,DESC', labelKey: 'admin.tickets.list.sort.amountDesc' },
    { value: 'totalAmount,ASC', labelKey: 'admin.tickets.list.sort.amountAsc' },
    { value: 'ticketCode,ASC', labelKey: 'admin.tickets.list.sort.codeAsc' },
    { value: 'ticketCode,DESC', labelKey: 'admin.tickets.list.sort.codeDesc' },
  ];

  private readonly queryParamMap = toSignal(this.route.queryParamMap, {
    initialValue: this.route.snapshot.queryParamMap,
  });

  readonly statusFilter = computed<TicketStatus | ''>(() => {
    const status = this.queryParamMap().get('status');
    return isTicketStatus(status) ? status : '';
  });

  readonly page = computed(() => numberParam(this.queryParamMap().get('page'), 0));
  readonly size = computed(() => numberParam(this.queryParamMap().get('size'), DEFAULT_PAGE_SIZE));
  readonly codeFilter = computed(() => this.queryParamMap().get('q')?.trim() ?? '');
  readonly fromFilter = computed(() => dateParam(this.queryParamMap().get('from')));
  readonly toFilter = computed(() => dateParam(this.queryParamMap().get('to')));
  readonly sortFilter = computed<TicketSort>(() => {
    const sort = this.queryParamMap().get('sort');
    return isTicketSort(sort) ? sort : 'createdAt,DESC';
  });

  readonly tickets = this.api.listResource(
    () => ({
      status: this.statusFilter() || undefined,
      q: this.codeFilter() || undefined,
      fromDate: this.fromFilter() || undefined,
      toDate: this.toFilter() || undefined,
      sort: this.sortFilter(),
      page: this.page(),
      size: this.size(),
    }),
    { suppressShellFeedback: true },
  );
  readonly ticketsError = resourceErrorVm(this.tickets, 'admin.tickets.list');
  readonly ticketPage = computed<TchPage<TicketRowView> | null>(() => {
    const status = this.tickets.status();
    if (status !== 'resolved' && status !== 'local' && status !== 'reloading') return null;
    return this.tickets.value() ?? null;
  });
  readonly ticketRows = computed(() => this.ticketPage()?.items ?? []);
  readonly hasTickets = computed(() => this.ticketRows().length > 0);
  readonly ticketPageIndex = computed(() => this.ticketPage()?.page ?? this.page());
  readonly ticketPageSize = computed(() => this.ticketPage()?.size ?? this.size());
  readonly ticketTotalElements = computed(() => this.ticketPage()?.totalElements ?? 0);

  onStatusFilter(status: TicketStatus | ''): void {
    this.navigateList({ status: status || null, page: null });
  }

  onCodeFilter(q: string): void {
    const value = q.trim();
    this.navigateList({ q: value || null, page: null });
  }

  onFromFilter(from: string): void {
    this.navigateList({ from: from || null, page: null });
  }

  onToFilter(to: string): void {
    this.navigateList({ to: to || null, page: null });
  }

  onSortFilter(sort: TicketSort): void {
    this.navigateList({ sort: sort === 'createdAt,DESC' ? null : sort, page: null });
  }

  onPageChange(page: number): void {
    this.navigateList({ page: page > 0 ? page : null });
  }

  onSizeChange(size: number): void {
    this.navigateList({ size: size !== DEFAULT_PAGE_SIZE ? size : null, page: null });
  }

  statusTone(status: TicketStatus): AdminStatusTone {
    return ticketStatusTone(status);
  }

  statusLabelKey(status: TicketStatus): string {
    return ticketStatusLabelKey(status);
  }

  amountDisplay(cents: number): string {
    return (cents / 100).toFixed(2);
  }

  private navigateList(params: {
    readonly status?: TicketStatus | null;
    readonly q?: string | null;
    readonly from?: string | null;
    readonly to?: string | null;
    readonly sort?: TicketSort | null;
    readonly page?: number | null;
    readonly size?: number | null;
  }): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: params,
      queryParamsHandling: 'merge',
    });
  }
}

function isTicketSort(value: string | null): value is TicketSort {
  return SORT_VALUES.includes(value as TicketSort);
}

function numberParam(value: string | null, fallback: number): number {
  if (value === null || value.trim() === '') return fallback;
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed >= 0 ? parsed : fallback;
}

function dateParam(value: string | null): string {
  if (value === null) return '';
  return /^\d{4}-\d{2}-\d{2}$/.test(value) ? value : '';
}
