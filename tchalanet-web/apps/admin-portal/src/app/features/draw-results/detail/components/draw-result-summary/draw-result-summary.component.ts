import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { AdminSectionCardComponent } from '@tch/ui/console';
import { ConsoleFact, ConsoleFactsComponent } from '@tch/web/console';

import { DrawResultView } from '../../../data-access/admin-draw-results-api.service';
import { lotteryLogoForSlot, lotteryProviderCodeFromSlot } from '../../../../../shared/lottery/lottery-assets';

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

  readonly logo = computed(() => lotteryLogoForSlot(this.result().slotKey ?? this.result().provider));
  readonly providerCode = computed(
    () =>
      lotteryProviderCodeFromSlot(this.result().slotKey ?? undefined)?.toUpperCase() ??
      this.result().provider?.toUpperCase() ??
      this.result().channelCode?.toUpperCase() ??
      '—',
  );
  readonly numbers = computed(() => this.result().numbers ?? []);
}
