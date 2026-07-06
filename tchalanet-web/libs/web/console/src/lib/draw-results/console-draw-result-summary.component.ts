import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { AdminSectionCardComponent } from '@tch/ui/console';

import { ConsoleFactsComponent } from '../entity-detail/console-facts.component';
import {
  ConsoleDrawResultSummaryFacts,
  ConsoleDrawResultSummaryView,
} from './console-draw-result-detail.models';

@Component({
  selector: 'tch-console-draw-result-summary',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AdminSectionCardComponent, ConsoleFactsComponent],
  templateUrl: './console-draw-result-summary.component.html',
  styleUrls: ['./console-draw-result-summary.component.scss'],
})
export class ConsoleDrawResultSummaryComponent {
  readonly summary = input.required<ConsoleDrawResultSummaryView>();
  readonly facts = input.required<ConsoleDrawResultSummaryFacts>();

  readonly identity = computed(() => this.summary().identity);
  readonly logo = computed(() => this.identity().providerLogoUrl);
  readonly providerCode = computed(() => this.identity().providerCode ?? '—');
  readonly numbers = computed(() => this.summary().numbers);
  readonly title = computed(() =>
    this.identity().channelName ??
    this.identity().slotLabel ??
    this.identity().slotKey ??
    'Résultat',
  );
}
