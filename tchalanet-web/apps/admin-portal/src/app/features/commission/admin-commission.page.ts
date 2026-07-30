import { LowerCasePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTableModule } from '@angular/material/table';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { mapHttpErrorToProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { finalize } from 'rxjs';

import { TchLoading } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { AdminPageShellComponent } from '@tch/ui/console';
import { AdminDetailLayoutComponent } from '@tch/ui/console';
import { AdminSectionCardComponent } from '@tch/ui/console';
import { AdminEmptyStateComponent } from '@tch/ui/console';
import { AdminSectionErrorTargetDirective, AdminSectionTargetError } from '@tch/ui/console';
import {
  AdminCommissionApi,
  CommissionOverviewView,
  SellerTerminalCommissionRow,
} from './data-access/admin-commission-api.service';
import { SetDefaultRateDialog } from './dialogs/set-default-rate.dialog';
import { SetSellerRateDialog } from './dialogs/set-seller-rate.dialog';

export function adminCommissionErrorView(
  err: unknown,
  source: string,
  translate: (key: string) => string,
): ErrorViewModel {
  const problem = mapHttpErrorToProblemDetail(err);
  const normalized = webAppErrorFromProblemDetail(problem, source, 'section');
  const copy = resolveErrorFeedbackCopy(normalized, translate);
  return toErrorViewModel(normalized, copy);
}

@Component({
  selector: 'tch-admin-commission-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    LowerCasePipe,
    AdminPageShellComponent,
    AdminDetailLayoutComponent,
    AdminSectionCardComponent,
    AdminSectionErrorTargetDirective,
    AdminEmptyStateComponent,
    TchLoading,
    TranslatePipe,
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

  readonly sellerColumns = [
    'terminalCode',
    'displayName',
    'status',
    'commissionRate',
    'source',
    'actions',
  ];

  readonly loading = signal(false);
  readonly sectionErrors = signal<readonly AdminSectionTargetError[]>([]);
  readonly overview = signal<CommissionOverviewView | null>(null);
  readonly sellers = signal<SellerTerminalCommissionRow[]>([]);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.sectionErrors.set([]);
    this.overview.set(null);
    this.sellers.set([]);

    let pending = 2;
    const settle = (): void => {
      pending -= 1;
      if (pending === 0) this.loading.set(false);
    };

    this.api
      .getOverview({ suppressShellFeedback: true })
      .pipe(finalize(settle))
      .subscribe({
        next: v => this.overview.set(v),
        error: (err: unknown) => this.setSectionError('admin.commission.defaultRate', err),
      });

    this.api
      .listSellers(0, 50, { suppressShellFeedback: true })
      .pipe(finalize(settle))
      .subscribe({
        next: v => this.sellers.set(v),
        error: err => this.setSectionError('admin.commission.sellers', err),
      });
  }

  openSetDefaultRate(): void {
    const current = this.overview()?.tenantDefaultRate ?? 0;
    const ref = this.dialog.open(SetDefaultRateDialog, { data: { current }, width: '420px' });
    ref.afterClosed().subscribe((saved?: boolean) => {
      if (saved) this.load();
    });
  }

  openSetSellerRate(row: SellerTerminalCommissionRow): void {
    const ref = this.dialog.open(SetSellerRateDialog, { data: { row }, width: '420px' });
    ref.afterClosed().subscribe((saved?: boolean) => {
      if (saved) this.load();
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
    return adminCommissionErrorView(err, source, key => this.translate.instant(key));
  }
}
