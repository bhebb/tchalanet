import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { ConsoleFact } from './console-entity-detail.models';

@Component({
  selector: 'tch-console-facts',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './console-facts.component.html',
  styleUrls: ['./console-facts.component.scss'],
})
export class ConsoleFactsComponent {
  readonly facts = input.required<readonly ConsoleFact[]>();
  readonly compact = input(false);
}
