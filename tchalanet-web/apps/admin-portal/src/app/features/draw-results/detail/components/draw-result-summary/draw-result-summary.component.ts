import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { AdminSectionCardComponent } from '@tch/ui/console';
import { ConsoleFact, ConsoleFactsComponent, consoleDrawIdentity } from '@tch/web/console';

import { DrawResultView } from '../../../data-access/admin-draw-results-api.service';

/** Tab « Résultats » — présentation seule du résultat appliqué et du tirage lié. */
@Component({
  selector: 'tch-draw-result-summary',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AdminSectionCardComponent, ConsoleFactsComponent],
  templateUrl: './draw-result-summary.component.html',
  styleUrls: ['./draw-result-summary.component.scss'],
})
export class DrawResultSummaryComponent {
  readonly result = input.required<DrawResultView>();
  readonly resultFacts = input.required<readonly ConsoleFact[]>();
  readonly linkedDrawFacts = input.required<readonly ConsoleFact[]>();

  readonly identity = computed(() => consoleDrawIdentity({
    providerCode: this.result().provider,
    channelCode: this.result().channelCode,
    channelName: this.result().channelName,
    slotKey: this.result().slotKey,
    slotLabel: this.result().slotLabel,
  }));
  readonly logo = computed(() => this.identity().providerLogoUrl);
  readonly providerCode = computed(() => this.identity().providerCode ?? '—');
  readonly numbers = computed(() => this.result().numbers ?? []);
}
