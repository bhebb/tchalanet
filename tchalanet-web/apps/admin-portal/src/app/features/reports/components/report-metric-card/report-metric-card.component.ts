import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'tch-report-metric-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './report-metric-card.component.html',
  styleUrls: ['./report-metric-card.component.scss'],
})
export class ReportMetricCardComponent {
  readonly label = input.required<string>();
  readonly value = input.required<string>();
  readonly hint = input<string | null>(null);
}
