import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { AdminSectionCardComponent } from '@tch/ui/console';

import { ConsoleFactsComponent } from '../entity-detail/console-facts.component';
import { ConsoleDrawSlotIdentityComponent } from '../draw-slots/console-draw-slot-identity.component';
import {
  ConsoleDrawResultSummaryFacts,
  ConsoleDrawResultSummaryView,
} from './console-draw-result-detail.models';

@Component({
  selector: 'tch-console-draw-result-summary',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AdminSectionCardComponent, ConsoleFactsComponent, ConsoleDrawSlotIdentityComponent],
  templateUrl: './console-draw-result-summary.component.html',
  styleUrls: ['./console-draw-result-summary.component.scss'],
})
export class ConsoleDrawResultSummaryComponent {
  readonly summary = input.required<ConsoleDrawResultSummaryView>();
  readonly facts = input.required<ConsoleDrawResultSummaryFacts>();

  readonly identity = computed(() => this.summary().identity);
  readonly numbers = computed(() => this.summary().numbers);
}
