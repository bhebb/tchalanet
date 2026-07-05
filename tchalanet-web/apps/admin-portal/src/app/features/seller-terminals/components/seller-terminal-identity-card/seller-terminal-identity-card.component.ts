import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { AdminSectionCardComponent } from '@tch/ui/console';
import {
  ConsoleActorIdentity,
  ConsoleActorRowComponent,
  ConsolePersonIdentitySummaryComponent,
  consoleSellerTerminalActorIdentity,
} from '@tch/web/console';

import { SellerTerminalView } from '../../data-access/seller-terminal-api.service';

@Component({
  selector: 'tch-seller-terminal-identity-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminSectionCardComponent,
    ConsoleActorRowComponent,
    ConsolePersonIdentitySummaryComponent,
    TranslatePipe,
  ],
  templateUrl: './seller-terminal-identity-card.component.html',
})
export class SellerTerminalIdentityCardComponent {
  readonly terminal = input.required<SellerTerminalView>();

  actorIdentity(terminal: SellerTerminalView): ConsoleActorIdentity {
    return consoleSellerTerminalActorIdentity(terminal);
  }
}
