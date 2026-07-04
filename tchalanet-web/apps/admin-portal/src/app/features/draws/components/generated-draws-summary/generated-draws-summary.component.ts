import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, input, signal } from '@angular/core';
import { TchCard } from '@tch/ui/components';
import { GeneratedDrawView, isGeneratedDrawSellableNow } from '../../data-access/admin-generated-draws.models';

@Component({
  selector: 'tch-generated-draws-summary',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TchCard],
  templateUrl: './generated-draws-summary.component.html',
  styleUrls: ['./generated-draws-summary.component.scss'],
})
export class GeneratedDrawsSummaryComponent {
  private readonly destroyRef = inject(DestroyRef);

  readonly draws    = input.required<GeneratedDrawView[]>();
  readonly today    = input<string>('');
  readonly nowMs    = signal(Date.now());

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
