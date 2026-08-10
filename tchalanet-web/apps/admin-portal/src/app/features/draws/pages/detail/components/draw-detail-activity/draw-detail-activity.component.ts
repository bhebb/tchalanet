import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { Router } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { TchLoading } from '@tch/ui/components';
import { AdminMetricCardComponent } from '@tch/ui/console';

import { DrawTopSelectionItem } from '../../../../../reports/data-access/admin-financials-api.service';

type DrawActivityState = 'idle' | 'loading' | 'ready' | 'error';
type DrawTopSelectionsState = 'idle' | 'loading' | 'ready' | 'error';

export interface DrawActivityReport {
  readonly ticketsSold: number;
  readonly grossSales: number;
  readonly sellerCommission: number;
  readonly activeSellerCount: number;
  readonly netRevenueEstimated: number;
  readonly promotionPayoutBase: number;
}

export interface DrawDetailActivityView {
  readonly countdown: string;
  readonly salesStatus: string;
  readonly salesOpen: boolean;
  readonly activityState: DrawActivityState;
  readonly activityReport: DrawActivityReport | null;
  readonly activityErrorMessage: string | null;
  readonly noSalesHint: string | null;
  readonly sellersReportQueryParams: Record<string, string>;
  readonly topSelectionsState: DrawTopSelectionsState;
  readonly topSelections: readonly DrawTopSelectionItem[];
  readonly topSelectionsErrorMessage: string | null;
}

@Component({
  selector: 'tch-draw-detail-activity',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DecimalPipe, AdminMetricCardComponent, TchLoading, TranslatePipe],
  templateUrl: './draw-detail-activity.component.html',
  styleUrl: './draw-detail-activity.component.scss',
})
export class DrawDetailActivityComponent {
  private readonly router = inject(Router);
  private readonly translate = inject(TranslateService);

  readonly view = input.required<DrawDetailActivityView>();

  topSelectionStake(selection: DrawTopSelectionItem): number {
    return selection.totalStakeCents / 100;
  }

  topSelectionsEyebrow(): string {
    const key = this.view().salesOpen
      ? 'admin.drawActivity.hotSelectionsOpen'
      : 'admin.drawActivity.hotSelectionsClosed';
    return this.translate.instant(key);
  }

  topSelectionsTitle(): string {
    const key = this.view().salesOpen
      ? 'admin.drawActivity.top5Open'
      : 'admin.drawActivity.top5Closed';
    return this.translate.instant(key);
  }

  openSellersReport(): void {
    void this.router.navigate(['/app/admin/reports/draws'], {
      queryParams: this.view().sellersReportQueryParams,
    });
  }
}
