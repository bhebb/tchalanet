import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { AdminEmptyStateComponent } from '@tch/ui/console';

import { DrawCombinationGameSection } from './console-draw-combinations';

@Component({
  selector: 'tch-console-draw-result-combinations',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AdminEmptyStateComponent],
  templateUrl: './console-draw-result-combinations.component.html',
  styleUrls: ['./console-draw-result-combinations.component.scss'],
})
export class ConsoleDrawResultCombinationsComponent {
  readonly rows = input.required<readonly DrawCombinationGameSection[]>();

  settlementLabel(mode: 'single' | 'bestOf' | 'cumulative'): string {
    switch (mode) {
      case 'cumulative':
        return 'Cumulatif';
      case 'bestOf':
        return 'Meilleur gain';
      case 'single':
        return 'Direct';
    }
  }

  settlementHint(mode: 'single' | 'bestOf' | 'cumulative'): string {
    switch (mode) {
      case 'cumulative':
        return 'Si le même numéro apparaît dans plusieurs lots, les gains compatibles se cumulent.';
      case 'bestOf':
        return 'Si plusieurs couvertures gagnent, le règlement garde le meilleur gain.';
      case 'single':
        return 'Une seule couverture règle cette option.';
    }
  }
}
