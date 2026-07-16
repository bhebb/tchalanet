import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  signal,
} from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { FormsModule } from '@angular/forms';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { catchError, of, startWith, switchMap } from 'rxjs';
import {
  TchLoading,
  TchErrorPanel,
  AdminPageHeader,
  AdminEmptyState,
} from '@tch/ui/components';
import { consoleGameName } from '@tch/web/console';

import { AdminFinancialsApi, type DrawFinancialRow } from './data-access/admin-financials-api.service';

function toIso(date: Date): string {
  return date.toISOString().slice(0, 10);
}

function today(): Date {
  return new Date();
}

interface ReportTotals {
  ticketsSold: number;
  totalSales: number;
  totalPayout: number;
  netRevenue: number;
}

interface SalesReportLine {
  readonly gameCode: string;
  readonly ticketsSold: number;
  readonly totalSales: number;
  readonly totalPayout: number;
  readonly netRevenue: number;
}

type PageState =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | {
      readonly status: 'ready';
      readonly lines: readonly SalesReportLine[];
      readonly totals: ReportTotals;
      readonly from: string;
      readonly to: string;
    };

@Component({
  selector: 'tch-admin-today-report-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DecimalPipe,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatDatepickerModule,
    MatNativeDateModule,
    TchLoading,
    TchErrorPanel,
    AdminPageHeader,
    AdminEmptyState,
  ],
  templateUrl: './admin-today-report.page.html',
  styleUrl: './admin-today-report.page.scss',
})
export class AdminTodayReportPage {
  private readonly api = inject(AdminFinancialsApi);

  readonly selectedDate = signal<Date>(today());
  readonly maxDate = today();

  private readonly dateParams = computed(() => {
    const iso = toIso(this.selectedDate());
    return { from: iso, to: iso };
  });

  readonly state = toSignal(
    toObservable(this.dateParams).pipe(
      switchMap(params =>
        this.api.getBreakdown(params).pipe(
          switchMap(resp => {
            const lines = this.linesByGame(resp.drawRows);
            const totals: ReportTotals = lines.reduce(
              (acc, l) => ({
                ticketsSold: acc.ticketsSold + l.ticketsSold,
                totalSales: acc.totalSales + l.totalSales,
                totalPayout: acc.totalPayout + l.totalPayout,
                netRevenue: acc.netRevenue + l.netRevenue,
              }),
              { ticketsSold: 0, totalSales: 0, totalPayout: 0, netRevenue: 0 },
            );
            return of({
              status: 'ready',
              lines,
              totals,
              from: resp.from,
              to: resp.to,
            } as PageState);
          }),
          catchError(() => of({ status: 'error' } as PageState)),
          startWith({ status: 'loading' } as PageState),
        ),
      ),
    ),
    { initialValue: { status: 'loading' } as PageState },
  );

  onDateChange(date: Date | null): void {
    if (date) this.selectedDate.set(date);
  }

  gameLabel(code: string): string {
    return consoleGameName(code);
  }

  private linesByGame(rows: readonly DrawFinancialRow[]): readonly SalesReportLine[] {
    const byGame = new Map<string, SalesReportLine>();
    for (const row of rows) {
      const current = byGame.get(row.gameCode) ?? {
        gameCode: row.gameCode,
        ticketsSold: 0,
        totalSales: 0,
        totalPayout: 0,
        netRevenue: 0,
      };
      byGame.set(row.gameCode, {
        gameCode: row.gameCode,
        ticketsSold: current.ticketsSold + row.ticketsSold,
        totalSales: current.totalSales + row.grossSales,
        totalPayout: current.totalPayout + row.payoutsPaid,
        netRevenue: current.netRevenue + row.netRevenuePaidBasis,
      });
    }
    return [...byGame.values()].sort((a, b) => a.gameCode.localeCompare(b.gameCode));
  }
}
