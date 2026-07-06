import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { TchStatusBadge } from '@tch/ui/components';

import {
  ConsoleActorIdentity,
  consoleActorPrimaryLabel,
  consoleActorSecondaryLabel,
  consoleActorStatusBadge,
} from './console-actor-identity';

@Component({
  selector: 'tch-console-actor-row',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TchStatusBadge],
  templateUrl: './console-actor-row.component.html',
  styleUrls: ['./console-actor-row.component.scss'],
})
export class ConsoleActorRowComponent {
  readonly actor = input.required<ConsoleActorIdentity>();
  readonly avatarIcon = input('account_circle');
  readonly showStatus = input(true);
  readonly statusLabel = input<string | null>(null);

  primaryLabel(actor: ConsoleActorIdentity): string {
    return consoleActorPrimaryLabel(actor);
  }

  secondaryLabel(actor: ConsoleActorIdentity): string | null {
    return consoleActorSecondaryLabel(actor);
  }

  statusBadge(status: string) {
    return consoleActorStatusBadge(status);
  }
}
