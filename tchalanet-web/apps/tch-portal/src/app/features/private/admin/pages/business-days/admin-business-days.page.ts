import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { TranslateService } from '@ngx-translate/core';

import { webAppErrorFromProblemDetail } from '@tch/api';
import type { ProblemDetail } from '@tch/api';
import { TchLoading, TchErrorPanel, TchSectionError } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { AdminPageShellComponent } from '../../../shared/admin-ui/admin-page-shell.component';
import { AdminEmptyStateComponent } from '../../../shared/admin-ui/admin-empty-state.component';
import {
  BusinessDaysApiService,
  BusinessDayView,
  UpsertBusinessDayRequest,
} from '../../business-days-api.service';
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

@Component({
  selector: 'tch-admin-business-days-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminPageShellComponent,
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

    return Array.from({ length: days }, (_, i) => {
      const dayNum = i + 1;
      const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(dayNum).padStart(2, '0')}`;
      const override = overrideMap.get(dateStr);
      return {
        date: dateStr,
        dayNum,
        status: override?.status ?? null,
        override: !!override,
      };
    });
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
      next: v => { this.overrides.set(v); this.loading.set(false); },
      error: (err: unknown) => {
        this.error.set(this.errorViewModel(err, 'admin.businessDays.list', 'page'));
        this.loading.set(false);
      },
    });
  }

  addOverride(): void {
    const ref = this.dialog.open(AddBusinessDayDialog, { width: '420px' });
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
