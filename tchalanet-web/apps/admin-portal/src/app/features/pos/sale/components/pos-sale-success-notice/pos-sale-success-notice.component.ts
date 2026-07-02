import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { TranslateService } from '@ngx-translate/core';
import { WebAppError } from '@tch/api';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';

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
  private readonly translate = inject(TranslateService);

  readonly ticket = input.required<ConfirmedTicketView>();
  readonly printing = input(false);

  readonly print = output<ConfirmedTicketView>();
  readonly viewDetails = output<ConfirmedTicketView>();
  readonly newTicket = output<void>();
  readonly dismissed = output<void>();

  readonly visible = signal(true);
  readonly canPrint = computed(() => !!this.ticket().ticketId && !this.printing());

  dismiss(): void {
    this.visible.set(false);
    this.dismissed.emit();
  }

  warningMessage(error: WebAppError): string {
    return resolveErrorFeedbackCopy(error, key => this.translate.instant(key)).message;
  }
}
