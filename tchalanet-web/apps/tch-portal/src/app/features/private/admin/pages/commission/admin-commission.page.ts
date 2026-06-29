import { LowerCasePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTableModule } from '@angular/material/table';
import { TranslateService } from '@ngx-translate/core';
import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';

import { TchLoading, TchErrorPanel } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { AdminPageShellComponent } from '../../../shared/admin-ui/admin-page-shell.component';
import { AdminSectionCardComponent } from '../../../shared/admin-ui/admin-section-card.component';
import { AdminEmptyStateComponent } from '../../../shared/admin-ui/admin-empty-state.component';
import {
  AdminSectionErrorTargetDirective,
  AdminSectionTargetError,
} from '../../../shared/admin-ui/admin-section-error-target.directive';
import {
  AdminCommissionApi,
  CommissionOverviewView,
  SellerTerminalCommissionRow,
} from '../../admin-commission-api.service';
import { SetDefaultRateDialog } from './dialogs/set-default-rate.dialog';
import { SetSellerRateDialog } from './dialogs/set-seller-rate.dialog';

@Component({
  selector: 'tch-admin-commission-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    LowerCasePipe,
    AdminPageShellComponent,
    AdminSectionCardComponent,
    AdminSectionErrorTargetDirective,
    AdminEmptyStateComponent,
    TchLoading,
    TchErrorPanel,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatTableModule,
  ],
  templateUrl: './admin-commission.page.html',
  styleUrls: ['./admin-commission.page.scss'],
})
export class AdminCommissionPage implements OnInit {
  private readonly api = inject(AdminCommissionApi);
  private readonly dialog = inject(MatDialog);
  private readonly translate = inject(TranslateService);

  readonly sellerColumns = ['terminalCode', 'displayName', 'status', 'commissionRate', 'source', 'actions'];

  readonly loading = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly sectionErrors = signal<readonly AdminSectionTargetError[]>([]);
  readonly overview = signal<CommissionOverviewView | null>(null);
  readonly sellers = signal<SellerTerminalCommissionRow[]>([]);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.sectionErrors.set([]);

    this.api.getOverview({ suppressShellFeedback: true }).subscribe({
      next: v => {
        this.overview.set(v);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.error.set(this.errorViewModel(err, 'admin.commission.overview'));
        this.loading.set(false);
      },
    });

    this.api.listSellers(0, 50, { suppressShellFeedback: true }).subscribe({
      next: v => this.sellers.set(v),
      error: err => this.setSectionError('admin.commission.sellers', err),
    });
  }

  openSetDefaultRate(): void {
    const current = this.overview()?.tenantDefaultRate ?? 0;
    const ref = this.dialog.open(SetDefaultRateDialog, { data: { current }, width: '420px' });
    ref.afterClosed().subscribe((rate: number | undefined) => {
      if (rate == null) return;
      this.clearSectionError('admin.commission.defaultRate');
      this.api.setDefaultRate(rate, { suppressShellFeedback: true }).subscribe({
        next: () => {
          this.load();
        },
        error: err => this.setSectionError('admin.commission.defaultRate', err),
      });
    });
  }

  openSetSellerRate(row: SellerTerminalCommissionRow): void {
    const ref = this.dialog.open(SetSellerRateDialog, { data: { row }, width: '420px' });
    ref.afterClosed().subscribe((rate: number | undefined) => {
      if (rate == null) return;
      this.clearSectionError('admin.commission.sellers');
      this.api.setSellerRate(row.id.value, rate, { suppressShellFeedback: true }).subscribe({
        next: () => {
          this.load();
        },
        error: err => this.setSectionError('admin.commission.sellers', err),
      });
    });
  }

  resetSellerRate(row: SellerTerminalCommissionRow): void {
    this.clearSectionError('admin.commission.sellers');
    this.api.resetSellerRate(row.id.value, { suppressShellFeedback: true }).subscribe({
      next: () => {
        this.load();
      },
      error: err => this.setSectionError('admin.commission.sellers', err),
    });
  }

  private setSectionError(target: string, err: unknown): void {
    const vm = this.errorViewModel(err, target);
    this.sectionErrors.update(errors => [
      ...errors.filter(error => error.target !== target),
      { ...vm, target },
    ]);
  }

  private clearSectionError(target: string): void {
    this.sectionErrors.update(errors => errors.filter(error => error.target !== target));
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
