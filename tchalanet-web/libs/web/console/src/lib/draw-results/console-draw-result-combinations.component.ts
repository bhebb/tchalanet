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
}
