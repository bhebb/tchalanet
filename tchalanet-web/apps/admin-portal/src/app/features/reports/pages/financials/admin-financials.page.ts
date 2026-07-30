import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatNativeDateModule } from '@angular/material/core';
import { MatTabsModule } from '@angular/material/tabs';
import { TranslateService } from '@ngx-translate/core';
import { catchError, map, of, startWith, switchMap } from 'rxjs';
import { mapHttpErrorToProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { AdminEmptyState, AdminPageHeader, TchErrorPanel, TchLoading } from '@tch/ui/components';
import { AdminMetricCardComponent, AdminMetricGridComponent } from '@tch/ui/console';
import { consoleGameName } from '@tch/web/console';
import { ErrorViewModel, resolveErrorFeedbackCopy, toErrorViewModel } from '@tch/web/errors';

import {
  AdminFinancialsApi,
  type DrawFinancialRow,
  type SellerTerminalDrawFinancialRow,
  type TenantFinancialBreakdownView,
} from '../../data-access/admin-financials-api.service';
import { AdminReportsApi } from '../../data-access/admin-reports-api.service';

type PageState =
  | { readonly status: 'loading' }
  | { readonly status: 'error'; readonly error: ErrorViewModel }
  | { readonly status: 'ready'; readonly data: TenantFinancialBreakdownView };

function today(): Date {
  return new Date();
}

function toIsoDate(date: Date): string {
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function addDays(date: Date, days: number): Date {
  const next = new Date(date);
  next.setDate(next.getDate() + days);
  return next;
}

@Component({
  selector: 'tch-admin-financials-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    DecimalPipe,
    FormsModule,
    MatButtonModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatNativeDateModule,
    MatTabsModule,
    AdminEmptyState,
    AdminPageHeader,
    AdminMetricCardComponent,
    AdminMetricGridComponent,
    TchErrorPanel,
    TchLoading,
  ],
  templateUrl: './admin-financials.page.html',
  styleUrl: './admin-financials.page.scss',
})
export class AdminFinancialsPage {
  private readonly api = inject(AdminFinancialsApi);
  private readonly reportsApi = inject(AdminReportsApi);
  private readonly translate = inject(TranslateService, { optional: true });

  readonly maxDate = today();
  readonly filtersExpanded = signal(false);
  readonly fromDate = signal<Date>(addDays(today(), -6));
  readonly toDate = signal<Date>(today());

  private readonly sellerTerminalLabels = toSignal(
    this.reportsApi.searchSellerTerminals('', { size: 200, suppressShellFeedback: true }).pipe(
      map(options => new Map(options.map(option => [option.id, option.subtitle ?? option.title]))),
      catchError(() => of(new Map<string, string | undefined>())),
    ),
    { initialValue: new Map<string, string | undefined>() },
  );

  private readonly params = computed(() => ({
    from: toIsoDate(this.fromDate()),
    to: toIsoDate(this.toDate()),
    drawLimit: 100,
    sellerTerminalLimit: 100,
  }));

  readonly state = toSignal(
    toObservable(this.params).pipe(
      switchMap(params =>
        this.api.getBreakdown(params, { suppressShellFeedback: true }).pipe(
          switchMap(data => of({ status: 'ready', data } as PageState)),
          catchError(err =>
            of({
              status: 'error',
              error: this.errorViewModel(err),
            } as PageState),
          ),
          startWith({ status: 'loading' } as PageState),
        ),
      ),
    ),
    { initialValue: { status: 'loading' } as PageState },
  );

  onFromDateChange(date: Date | null): void {
    if (!date) return;
    this.fromDate.set(date);
    if (date > this.toDate()) {
      this.toDate.set(date);
    }
  }

  onToDateChange(date: Date | null): void {
    if (!date) return;
    this.toDate.set(date);
    if (date < this.fromDate()) {
      this.fromDate.set(date);
    }
  }

  setToday(): void {
    const date = today();
    this.fromDate.set(date);
    this.toDate.set(date);
  }

  setSevenDays(): void {
    const date = today();
    this.fromDate.set(addDays(date, -6));
    this.toDate.set(date);
  }

  hasNoData(data: TenantFinancialBreakdownView): boolean {
    return (
      data.summary.ticketsSold === 0 &&
      data.drawRows.length === 0 &&
      data.sellerTerminalDrawRows.length === 0 &&
      data.sellerTerminalDailyRows.length === 0
    );
  }

  shortId(value: string | null | undefined): string {
    if (!value) return '—';
    return value.length > 8 ? value.slice(0, 8) : value;
  }

  terminalLabel(sellerTerminalId: string): string {
    return this.sellerTerminalLabels().get(sellerTerminalId) ?? this.shortId(sellerTerminalId);
  }

  drawLabel(row: DrawFinancialRow | SellerTerminalDrawFinancialRow): string {
    const channel = row.drawChannelCode ? `${row.drawChannelCode} · ` : '';
    return `${channel}${consoleGameName(row.gameCode)}`;
  }

  private errorViewModel(err: unknown): ErrorViewModel {
    const normalized = webAppErrorFromProblemDetail(
      mapHttpErrorToProblemDetail(err),
      'admin.reports.financials',
      'page',
    );
    const copy = resolveErrorFeedbackCopy(normalized, key => this.translate?.instant(key) ?? key);
    return toErrorViewModel(normalized, copy);
  }
}
