import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { MatrixSummary } from '../../data-access/admin-draw-sales-matrix-api.service';

interface MatrixSummaryItem {
  readonly labelKey: string;
  readonly value: string | number;
  readonly tone?: 'neutral' | 'warning';
}

@Component({
  selector: 'tch-draw-sales-matrix-summary',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe],
  templateUrl: './matrix-summary.component.html',
  styleUrls: ['./matrix-summary.component.scss'],
})
export class DrawSalesMatrixSummaryComponent {
  readonly summary = input.required<MatrixSummary>();

  protected readonly items = computed<readonly MatrixSummaryItem[]>(() => {
    const s = this.summary();
    return [
      {
        labelKey: 'admin.drawSalesMatrix.summary.saleReady',
        value: s.saleReadyChannelGameCount,
      },
      {
        labelKey: 'admin.drawSalesMatrix.summary.offeredGames',
        value: s.offeredChannelGameCount,
      },
      {
        labelKey: 'admin.drawSalesMatrix.summary.activeChannels',
        value: `${s.activeChannelCount} / ${s.configuredChannelCount}`,
      },
      {
        labelKey: 'admin.drawSalesMatrix.summary.availableGames',
        value: s.supportedTenantGameCount,
      },
      ...(s.missingStakeConfigCount > 0
        ? [{
            labelKey: 'admin.drawSalesMatrix.summary.missingStake',
            value: s.missingStakeConfigCount,
            tone: 'warning' as const,
          }]
        : []),
      ...(s.missingLimitCount > 0
        ? [{
            labelKey: 'admin.drawSalesMatrix.summary.missingLimits',
            value: s.missingLimitCount,
            tone: 'warning' as const,
          }]
        : []),
    ];
  });
}
