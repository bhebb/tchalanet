import { ChangeDetectionStrategy, Component, input, output, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';

import { ConfirmedTicketView } from '../../data-access/pos-sale.models';

@Component({
  selector: 'tch-pos-sale-success-notice',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule],
  templateUrl: './pos-sale-success-notice.component.html',
  styleUrls: ['./pos-sale-success-notice.component.scss'],
})
export class PosSaleSuccessNoticeComponent {
  readonly ticket = input.required<ConfirmedTicketView>();

  readonly print = output<ConfirmedTicketView>();
  readonly viewDetails = output<ConfirmedTicketView>();
  readonly newTicket = output<void>();
  readonly dismissed = output<void>();

  readonly visible = signal(true);

  dismiss(): void {
    this.visible.set(false);
    this.dismissed.emit();
  }
}
