import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

export interface DailySalesChartPoint {
  readonly label: string;
  readonly ticketsSold: number;
  readonly grossSales: number;
}

@Component({
  selector: 'tch-daily-sales-chart',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DecimalPipe],
  templateUrl: './daily-sales-chart.component.html',
  styleUrls: ['./daily-sales-chart.component.scss'],
})
export class DailySalesChartComponent {
  readonly points = input.required<readonly DailySalesChartPoint[]>();
  readonly currency = input('HTG');

  readonly maxSales = computed(() =>
    this.points().reduce((max, point) => Math.max(max, point.grossSales), 0),
  );

  barWidth(point: DailySalesChartPoint): string {
    const max = this.maxSales();
    if (max === 0) return '0%';
    return `${Math.max((point.grossSales / max) * 100, 4)}%`;
  }
}
