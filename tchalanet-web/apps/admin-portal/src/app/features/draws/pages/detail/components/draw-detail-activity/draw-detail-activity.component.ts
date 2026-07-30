import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TchLoading } from '@tch/ui/components';

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
  imports: [DecimalPipe, RouterLink, TchLoading],
  templateUrl: './draw-detail-activity.component.html',
  styleUrl: './draw-detail-activity.component.scss',
})
export class DrawDetailActivityComponent {
  readonly view = input.required<DrawDetailActivityView>();

  topSelectionStake(selection: DrawTopSelectionItem): number {
    return selection.totalStakeCents / 100;
  }
}
