import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { TCH_DEFAULT_PAGE_SIZE, TchPage } from '@tch/api';
import {
  AdminListStatusOption,
  AdminListSurface,
  TchDrawLabel,
  TchNotice,
} from '@tch/ui/components';
import { consoleTicketDrawIdentity } from '@tch/web/console';

import { AdminCrudShellComponent } from '@tch/ui/console';
import { AdminEmptyStateComponent } from '@tch/ui/console';
import { AdminPageShellComponent } from '@tch/ui/console';
import { AdminRefreshButtonComponent } from '@tch/ui/console';
import { TchPaginationComponent } from '@tch/ui/console';
import { AdminStatusPillComponent, AdminStatusTone } from '@tch/ui/console';
import { TchAsyncReadyDirective, TchAsyncViewComponent, resourceErrorVm } from '@tch/web/async';
import {
  AdminTicketsApi,
  TicketResultStatus,
  TicketRowView,
  TicketStatus,
} from '../../data-access/admin-tickets-api.service';
import {
  TICKET_STATUS_VALUES,
  isTicketResultStatus,
  isTicketStatus,
  ticketResultStatusLabelKey,
  ticketResultStatusTone,
  ticketStatusLabelKey,
  ticketStatusTone,
} from '../../../../shared/ticket/admin-ticket-status.util';
import { PosSaleSuccessDialogComponent } from '../../../pos/sale/components/pos-sale-success-dialog/pos-sale-success-dialog.component';
import { AdminDrawSalesMatrixApi } from '../../../draw-sales-matrix/data-access/admin-draw-sales-matrix-api.service';
import {
  DrawChannelFilterSelectComponent,
  DrawChannelFilterValue,
} from '../../../draw-sales-matrix/components/draw-channel-filter-select/draw-channel-filter-select.component';

const SORT_VALUES = [
  'createdAt,DESC',
  'createdAt,ASC',
  'totalAmount,DESC',
  'totalAmount,ASC',
  'ticketCode,ASC',
  'ticketCode,DESC',
] as const;
type TicketSort = (typeof SORT_VALUES)[number];

@Component({
  selector: 'tch-admin-tickets-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    RouterLink,
    AdminPageShellComponent,
    AdminRefreshButtonComponent,
    AdminCrudShellComponent,
    AdminListSurface,
    AdminEmptyStateComponent,
    AdminStatusPillComponent,
    TchPaginationComponent,
    TchAsyncReadyDirective,
    TchAsyncViewComponent,
    TchDrawLabel,
    TchNotice,
    TranslatePipe,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatTableModule,
    DrawChannelFilterSelectComponent,
  ],
  templateUrl: './admin-tickets.page.html',
  styleUrls: ['./admin-tickets.page.scss'],
})
export class AdminTicketsPage {
  private readonly api = inject(AdminTicketsApi);
  private readonly drawSalesMatrix = inject(AdminDrawSalesMatrixApi);
  private readonly dialog = inject(MatDialog);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly translate = inject(TranslateService);

  readonly columns = [
    'ticketCode',
    'status',
    'resultStatus',
    'drawChannelName',
    'drawScheduledAt',
    'totalAmountCents',
    'winningAmountCents',
    'placedAt',
    'actions',
  ];
  readonly reprintingTicketId = signal<string | null>(null);
  readonly reprintError = signal<string | null>(null);
  readonly statusOptions: readonly AdminListStatusOption[] = TICKET_STATUS_VALUES.map(status => ({
    value: status,
    label: this.translate.instant(ticketStatusLabelKey(status)),
  }));
  readonly sortOptions: readonly { value: TicketSort; labelKey: string }[] = [
    { value: 'createdAt,DESC', labelKey: 'admin.tickets.list.sort.createdDesc' },
    { value: 'createdAt,ASC', labelKey: 'admin.tickets.list.sort.createdAsc' },
    { value: 'totalAmount,DESC', labelKey: 'admin.tickets.list.sort.amountDesc' },
    { value: 'totalAmount,ASC', labelKey: 'admin.tickets.list.sort.amountAsc' },
    { value: 'ticketCode,ASC', labelKey: 'admin.tickets.list.sort.codeAsc' },
    { value: 'ticketCode,DESC', labelKey: 'admin.tickets.list.sort.codeDesc' },
  ];
  readonly resultStatusOptions: readonly {
    value: TicketResultStatus | 'WINNING';
    labelKey: string;
  }[] = [
    { value: 'WINNING', labelKey: 'admin.tickets.list.filters.winningOnly' },
    { value: 'WON', labelKey: 'admin.tickets.resultStatus.won' },
    { value: 'LOST', labelKey: 'admin.tickets.resultStatus.lost' },
    { value: 'NOT_RESULTED', labelKey: 'admin.tickets.resultStatus.notResulted' },
    { value: 'PENDING', labelKey: 'admin.tickets.resultStatus.pending' },
    { value: 'OVERRIDDEN', labelKey: 'admin.tickets.resultStatus.overridden' },
  ];
  private readonly queryParamMap = toSignal(this.route.queryParamMap, {
    initialValue: this.route.snapshot.queryParamMap,
  });

  readonly statusFilter = computed<TicketStatus | ''>(() => {
    const status = this.queryParamMap().get('status');
    return isTicketStatus(status) ? status : '';
  });
  readonly drawIdFilter = computed(() => uuidParam(this.queryParamMap().get('drawId')));
  readonly providerFilter = computed(() => providerParam(this.queryParamMap().get('provider')));
  readonly slotKeyFilter = computed(() => slotKeyParam(this.queryParamMap().get('slotKey')));
  readonly providerMatrix = this.drawSalesMatrix.getMatrixResource({ suppressShellFeedback: true });
  readonly resultFilter = computed<TicketResultStatus | 'WINNING' | ''>(() => {
    if (this.queryParamMap().get('winningOnly') === 'true') return 'WINNING';
    const status = this.queryParamMap().get('resultStatus');
    return isTicketResultStatus(status) ? status : '';
  });

  readonly page = computed(() => numberParam(this.queryParamMap().get('page'), 0));
  readonly size = computed(() =>
    numberParam(this.queryParamMap().get('size'), TCH_DEFAULT_PAGE_SIZE),
  );
  readonly codeFilter = computed(() => this.queryParamMap().get('q')?.trim() ?? '');
  readonly fromFilter = computed(() => dateParam(this.queryParamMap().get('from')));
  readonly toFilter = computed(() => dateParam(this.queryParamMap().get('to')));
  readonly sortFilter = computed<TicketSort>(() => {
    const sort = this.queryParamMap().get('sort');
    return isTicketSort(sort) ? sort : 'createdAt,DESC';
  });
  /** Filter panel starts open only when a non-default filter is already active from the URL. */
  readonly hasActiveFilters = computed(
    () =>
      !!this.drawIdFilter() ||
      !!this.providerFilter() ||
      !!this.slotKeyFilter() ||
      !!this.statusFilter() ||
      !!this.resultFilter() ||
      !!this.fromFilter() ||
      !!this.toFilter() ||
      this.sortFilter() !== 'createdAt,DESC',
  );

  readonly tickets = this.api.listResource(
    () => {
      const resultFilter = this.resultFilter();
      return {
        drawId: this.drawIdFilter() || undefined,
        provider: this.providerFilter() || undefined,
        slotKey: this.slotKeyFilter() || undefined,
        status: this.statusFilter() || undefined,
        resultStatus: resultFilter && resultFilter !== 'WINNING' ? resultFilter : undefined,
        winningOnly: resultFilter === 'WINNING',
        q: this.codeFilter() || undefined,
        fromDate: this.fromFilter() || undefined,
        toDate: this.toFilter() || undefined,
        sort: this.sortFilter(),
        page: this.page(),
        size: this.size(),
      };
    },
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

  ticketDrawIdentity(ticket: TicketRowView) {
    return consoleTicketDrawIdentity({
      channelCode: ticket.drawChannelCode,
      channelLabel: ticket.drawChannelName,
      resultSlotKey: ticket.resultSlotKey,
      scheduledAt: ticket.drawScheduledAt,
      fallbackLabel: ticket.drawChannelName,
    });
  }

  ticketDrawLabel(ticket: TicketRowView): string {
    return this.ticketDrawIdentity(ticket).receiptLabel;
  }

  onStatusFilter(status: TicketStatus | ''): void {
    this.navigateList({ status: status || null, page: null });
  }

  onDrawChannelFilter(selection: DrawChannelFilterValue): void {
    this.navigateList({
      provider: selection.provider || null,
      slotKey: selection.slotKey || null,
      page: null,
    });
  }

  onResultFilter(status: TicketResultStatus | 'WINNING' | ''): void {
    this.navigateList({
      resultStatus: status && status !== 'WINNING' ? status : null,
      winningOnly: status === 'WINNING' ? 'true' : null,
      page: null,
    });
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

  resetFilters(): void {
    this.navigateList({
      drawId: null,
      provider: null,
      slotKey: null,
      q: null,
      status: null,
      resultStatus: null,
      winningOnly: null,
      from: null,
      to: null,
      sort: null,
      page: null,
    });
  }

  onPageChange(page: number): void {
    this.navigateList({ page: page > 0 ? page : null });
  }

  onSizeChange(size: number): void {
    this.navigateList({ size: size !== TCH_DEFAULT_PAGE_SIZE ? size : null, page: null });
  }

  statusTone(status: TicketStatus): AdminStatusTone {
    return ticketStatusTone(status);
  }

  statusLabelKey(status: TicketStatus): string {
    return ticketStatusLabelKey(status);
  }

  resultStatusTone(status: TicketResultStatus | string | null | undefined): AdminStatusTone {
    return ticketResultStatusTone(status);
  }

  resultStatusLabelKey(status: TicketResultStatus | string | null | undefined): string {
    return ticketResultStatusLabelKey(status);
  }

  amountDisplay(cents: number): string {
    return (cents / 100).toFixed(2);
  }

  canReprint(ticket: TicketRowView): boolean {
    return !!this.sellerTerminalId(ticket) && this.reprintingTicketId() !== ticket.id;
  }

  reprint(ticket: TicketRowView, event: Event): void {
    event.stopPropagation();
    event.preventDefault();

    const sellerTerminalId = this.sellerTerminalId(ticket);
    if (!sellerTerminalId || this.reprintingTicketId()) return;

    this.reprintingTicketId.set(ticket.id);
    this.reprintError.set(null);

    this.dialog.open(PosSaleSuccessDialogComponent, {
      width: 'min(42rem, calc(100vw - 2rem))',
      data: {
        mode: 'ticket-actions',
        sellerTerminalId,
        totalAmount: ticket.totalAmountCents / 100,
        currency: ticket.currency,
        ticket: {
          ticketId: ticket.id,
          ticketCode: ticket.ticketCode,
          publicCode: ticket.publicCode,
          actionAvailability: {
            canSell: false,
            canPrint: true,
            canSendSms: false,
            canSendWhatsapp: false,
            canSendEmail: false,
            canCopy: true,
          },
        },
      },
    });
    this.reprintingTicketId.set(null);
  }

  private navigateList(params: {
    readonly drawId?: string | null;
    readonly provider?: string | null;
    readonly slotKey?: string | null;
    readonly status?: TicketStatus | null;
    readonly resultStatus?: TicketResultStatus | null;
    readonly winningOnly?: string | null;
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

  private sellerTerminalId(ticket: TicketRowView): string | null {
    const value = ticket.sellerTerminalId;
    if (typeof value === 'string') return value;
    return value?.value ?? null;
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

function uuidParam(value: string | null): string {
  if (value === null) return '';
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(value)
    ? value
    : '';
}

function providerParam(value: string | null): string {
  if (value === null) return '';
  const provider = value.trim().toUpperCase();
  return /^[A-Z]{2,8}$/.test(provider) ? provider : '';
}

function slotKeyParam(value: string | null): string {
  if (value === null) return '';
  const slotKey = value.trim().toUpperCase();
  return /^[A-Z0-9_-]{2,64}$/.test(slotKey) ? slotKey : '';
}
