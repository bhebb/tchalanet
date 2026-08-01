import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { AdminSectionCardComponent } from '@tch/ui/console';

import { ConsoleFactsComponent } from '../entity-detail/console-facts.component';
import { ConsoleFact } from '../entity-detail/console-entity-detail.models';
import { ConsoleDrawSlotIdentityComponent } from '../draw-slots/console-draw-slot-identity.component';
import { ConsoleDrawSlotIdentity } from '../draw-slots/console-draw-slot-identity.models';

@Component({
  selector: 'tch-console-ticket-draw-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AdminSectionCardComponent, ConsoleFactsComponent, ConsoleDrawSlotIdentityComponent],
  templateUrl: './console-ticket-draw-card.component.html',
  styleUrls: ['./console-ticket-draw-card.component.scss'],
})
export class ConsoleTicketDrawCardComponent {
  readonly title = input.required<string>();
  readonly identity = input.required<ConsoleDrawSlotIdentity>();
  readonly facts = input<readonly ConsoleFact[]>([]);
  readonly description = input<string | null>(null);
  readonly icon = input('event');
}
