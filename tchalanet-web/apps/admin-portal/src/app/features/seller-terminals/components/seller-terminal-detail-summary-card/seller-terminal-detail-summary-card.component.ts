import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import {
  ConsoleActorIdentity,
  ConsoleActorRowComponent,
  consoleSellerTerminalActorIdentity,
} from '@tch/web/console';

import { SellerTerminalView } from '../../data-access/seller-terminal-api.service';

@Component({
  selector: 'tch-seller-terminal-detail-summary-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ConsoleActorRowComponent, TranslatePipe],
  templateUrl: './seller-terminal-detail-summary-card.component.html',
  styleUrls: ['./seller-terminal-detail-summary-card.component.scss'],
})
export class SellerTerminalDetailSummaryCardComponent {
  readonly terminal = input.required<SellerTerminalView>();

  actorIdentity(terminal: SellerTerminalView): ConsoleActorIdentity {
    return consoleSellerTerminalActorIdentity(terminal);
  }
}
