import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { AdminMetricCardComponent } from '@tch/ui/console';
import { GeneratedDrawsSummary } from '../../data-access/admin-generated-draws.models';

export type GeneratedDrawsSummaryKpi = 'today' | 'salesOpen' | 'expected' | 'confirmed';

@Component({
  selector: 'tch-generated-draws-summary',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AdminMetricCardComponent],
  styles: [':host { display: block; margin-block-end: 1.5rem; }'],
  templateUrl: './generated-draws-summary.component.html',
})
export class GeneratedDrawsSummaryComponent {
  readonly summary = input<GeneratedDrawsSummary | undefined>();
  readonly kpiSelected = output<GeneratedDrawsSummaryKpi>();

  readonly todayValue = computed(() => this.value(this.summary()?.todayCount));
  readonly salesOpenValue = computed(() => this.value(this.summary()?.salesOpenCount));
  readonly expectedValue = computed(() => this.value(this.summary()?.expectedCount));
  readonly confirmedValue = computed(() => this.value(this.summary()?.confirmedCount));

  private value(value: number | undefined): string {
    return value == null ? '—' : value.toString();
  }
}
