import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

import { webAppErrorFromProblemDetail } from '@tch/api';
import type { ProblemDetail } from '@tch/api';
import { TchLoading, TchErrorPanel, TchSectionError } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { AdminPageShellComponent } from '@tch/ui/console';
import { AdminEmptyStateComponent } from '@tch/ui/console';
import {
  BusinessDaysApiService,
  BusinessDayView,
  ProviderBusinessDayClosure,
  UpsertBusinessDayRequest,
} from './data-access/business-days-api.service';
import { AddBusinessDayDialog } from './dialogs/add-business-day.dialog';

function startOfMonth(year: number, month: number): string {
  return `${year}-${String(month + 1).padStart(2, '0')}-01`;
}

function endOfMonth(year: number, month: number): string {
  const last = new Date(year, month + 1, 0).getDate();
  return `${year}-${String(month + 1).padStart(2, '0')}-${String(last).padStart(2, '0')}`;
}

function daysInMonth(year: number, month: number): number {
  return new Date(year, month + 1, 0).getDate();
}

function monthStartOffset(year: number, month: number): number {
  return (new Date(year, month, 1).getDay() + 6) % 7;
}

function todayIso(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

function providerClosureSummary(closures: readonly ProviderBusinessDayClosure[]): string | null {
  if (!closures.length) return null;
  return closures
    .map(closure => {
      const source = `${closure.provider} · ${closure.slotKey}`;
      return closure.reason ? `${source} — ${closure.reason}` : source;
    })
    .join('\n');
}

@Component({
  selector: 'tch-admin-business-days-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminPageShellComponent,
    RouterLink,
    AdminEmptyStateComponent,
    TchLoading,
    TchErrorPanel,
    TchSectionError,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
  ],
  templateUrl: './admin-business-days.page.html',
  styleUrls: ['./admin-business-days.page.scss'],
})
export class AdminBusinessDaysPage implements OnInit {
  private readonly api = inject(BusinessDaysApiService);
  private readonly dialog = inject(MatDialog);
  private readonly translate = inject(TranslateService);

  readonly tableColumns = ['date', 'status', 'reason', 'actions'];
  readonly loading = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly actionError = signal<ErrorViewModel | null>(null);
  readonly actionNotice = signal<string | null>(null);
  readonly overrides = signal<BusinessDayView[]>([]);
  readonly providerClosures = signal<ProviderBusinessDayClosure[]>([]);
  readonly weekdays = ['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim'];

  private readonly now = new Date();
  readonly currentYear = signal(this.now.getFullYear());
  readonly currentMonth = signal(this.now.getMonth());

  readonly monthLabel = computed(() => {
    const d = new Date(this.currentYear(), this.currentMonth(), 1);
    return d.toLocaleDateString('fr-FR', { month: 'long', year: 'numeric' });
  });

  readonly calendarDays = computed(() => {
    const year = this.currentYear();
    const month = this.currentMonth();
    const days = daysInMonth(year, month);
    const overrideMap = new Map(this.overrides().map(o => [o.date, o]));
    const providerMap = new Map<string, ProviderBusinessDayClosure[]>();
    for (const closure of this.providerClosures()) {
      providerMap.set(closure.date, [...(providerMap.get(closure.date) ?? []), closure]);
    }
    const blanks = monthStartOffset(year, month);

    const blankDays = Array.from({ length: blanks }, (_, i) => ({
      key: `blank-${year}-${month}-${i}`,
      date: null,
      dayNum: null,
      status: null,
      override: false,
      reason: null,
      providerClosures: [] as ProviderBusinessDayClosure[],
      providerSummary: null,
    }));

    const monthDays = Array.from({ length: days }, (_, i) => {
      const dayNum = i + 1;
      const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(dayNum).padStart(2, '0')}`;
      const override = overrideMap.get(dateStr);
      const providerClosures = providerMap.get(dateStr) ?? [];
      const providerSummary = providerClosureSummary(providerClosures);
      return {
        key: dateStr,
        date: dateStr,
        dayNum,
        status: override?.status ?? null,
        override: !!override,
        reason: override?.reason ?? null,
        providerClosures,
        providerSummary,
      };
    });
    return [...blankDays, ...monthDays];
  });

  ngOnInit(): void {
    this.loadMonth();
  }

  prevMonth(): void {
    if (this.currentMonth() === 0) {
      this.currentYear.update(y => y - 1);
      this.currentMonth.set(11);
    } else {
      this.currentMonth.update(m => m - 1);
    }
    this.loadMonth();
  }

  nextMonth(): void {
    if (this.currentMonth() === 11) {
      this.currentYear.update(y => y + 1);
      this.currentMonth.set(0);
    } else {
      this.currentMonth.update(m => m + 1);
    }
    this.loadMonth();
  }

  loadMonth(preserveActionFeedback = false): void {
    const from = startOfMonth(this.currentYear(), this.currentMonth());
    const to = endOfMonth(this.currentYear(), this.currentMonth());
    this.loading.set(true);
    this.error.set(null);
    if (!preserveActionFeedback) {
      this.actionError.set(null);
      this.actionNotice.set(null);
    }
    this.api.listBusinessDays({ from, to }, { suppressShellFeedback: true }).subscribe({
      next: v => {
        this.overrides.set(v);
        this.loadProviderClosures(from, to);
      },
      error: (err: unknown) => {
        this.error.set(this.errorViewModel(err, 'admin.businessDays.list', 'page'));
        this.loading.set(false);
      },
    });
  }

  private loadProviderClosures(from: string, to: string): void {
    this.api.listProviderClosures({ from, to }, { suppressShellFeedback: true }).subscribe({
      next: v => {
        this.providerClosures.set(v);
        this.loading.set(false);
      },
      error: () => {
        this.providerClosures.set([]);
        this.loading.set(false);
      },
    });
  }

  addOverride(date = todayIso()): void {
    const existing = this.overrides().find(row => row.date === date);
    const ref = this.dialog.open(AddBusinessDayDialog, {
      width: '420px',
      data: {
        date,
        status: existing?.status ?? 'CLOSED',
        reason: existing?.reason ?? '',
      },
    });
    ref.afterClosed().subscribe((req: UpsertBusinessDayRequest | undefined) => {
      if (!req) return;
      this.actionError.set(null);
      this.actionNotice.set(null);
      this.api.upsertBusinessDay(req, { suppressShellFeedback: true }).subscribe({
        next: () => {
          this.actionNotice.set('Exception ajoutée.');
          this.loadMonth(true);
        },
        error: (err: unknown) => this.actionError.set(this.errorViewModel(err, 'admin.businessDays.add', 'section')),
      });
    });
  }

  deleteOverride(row: BusinessDayView): void {
    this.actionError.set(null);
    this.actionNotice.set(null);
    this.api.deleteBusinessDay(row.id, { suppressShellFeedback: true }).subscribe({
      next: () => {
        this.actionNotice.set('Exception supprimée.');
        this.loadMonth(true);
      },
      error: (err: unknown) => this.actionError.set(
        this.errorViewModel(err, `admin.businessDays.delete.${row.id}`, 'section'),
      ),
    });
  }

  private errorViewModel(
    err: unknown,
    source: string,
    surface: 'page' | 'section',
  ): ErrorViewModel {
    const problem = (err as { error?: ProblemDetail })?.error;
    if (!problem) {
      return {
        severity: 'error',
        title: this.translate.instant('common.errors.fallback.title'),
        message: this.translate.instant('common.errors.fallback.message'),
      };
    }

    const normalized = webAppErrorFromProblemDetail(problem, source, surface);
    const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
    return toErrorViewModel(normalized, copy);
  }
}
