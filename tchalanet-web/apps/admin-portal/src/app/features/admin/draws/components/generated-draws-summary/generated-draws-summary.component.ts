import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { TchCard } from '@tch/ui/components';
import { GeneratedDrawView } from '../../data-access/admin-generated-draws.models';

@Component({
  selector: 'tch-generated-draws-summary',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TchCard],
  templateUrl: './generated-draws-summary.component.html',
  styleUrls: ['./generated-draws-summary.component.scss'],
})
export class GeneratedDrawsSummaryComponent {
  readonly draws    = input.required<GeneratedDrawView[]>();
  readonly today    = input<string>('');

  readonly todayCount      = computed(() =>
    this.draws().filter(d => d.businessDate === this.today()).length,
  );
  readonly salesOpenCount  = computed(() =>
    this.draws().filter(d => d.salesStatus === 'OPEN').length,
  );
  readonly expectedCount   = computed(() =>
    this.draws().filter(d => d.resultStatus === 'EXPECTED' || d.resultStatus === 'MISSING').length,
  );
  readonly confirmedCount  = computed(() =>
    this.draws().filter(d => d.resultStatus === 'CONFIRMED').length,
  );
}
