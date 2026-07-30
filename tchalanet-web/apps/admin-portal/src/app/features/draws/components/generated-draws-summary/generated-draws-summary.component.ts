import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, input, output, signal } from '@angular/core';
import { AdminMetricCardComponent } from '@tch/ui/console';
import { GeneratedDrawView, isGeneratedDrawSellableNow } from '../../data-access/admin-generated-draws.models';

export type GeneratedDrawsSummaryKpi = 'today' | 'salesOpen' | 'expected' | 'confirmed';

@Component({
  selector: 'tch-generated-draws-summary',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AdminMetricCardComponent],
  templateUrl: './generated-draws-summary.component.html',
})
export class GeneratedDrawsSummaryComponent {
  private readonly destroyRef = inject(DestroyRef);

  readonly draws    = input.required<GeneratedDrawView[]>();
  readonly today    = input<string>('');
  readonly nowMs    = signal(Date.now());
  readonly kpiSelected = output<GeneratedDrawsSummaryKpi>();

  constructor() {
    const timer = globalThis.setInterval(() => this.nowMs.set(Date.now()), 1000);
    this.destroyRef.onDestroy(() => globalThis.clearInterval(timer));
  }

  readonly todayCount      = computed(() =>
    this.draws().filter(d => d.businessDate === this.today()).length,
  );
  readonly salesOpenCount  = computed(() =>
    this.draws().filter(d => isGeneratedDrawSellableNow(d, this.nowMs())).length,
  );
  readonly expectedCount   = computed(() =>
    this.draws().filter(d => d.resultStatus === 'EXPECTED' || d.resultStatus === 'MISSING').length,
  );
  readonly confirmedCount  = computed(() =>
    this.draws().filter(d => d.resultStatus === 'CONFIRMED').length,
  );
}
