import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { DatePipe, DecimalPipe, UpperCasePipe } from '@angular/common';

import {
  ConfirmedTicketView,
  PosGameView,
  PosOpenDrawView,
  PosSellerTerminalView,
  PosTicketDraftLine,
} from '../../data-access/pos-sale.models';

@Component({
  selector: 'tch-pos-ticket-preview',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, DecimalPipe, UpperCasePipe],
  templateUrl: './pos-ticket-preview.component.html',
  styleUrls: ['./pos-ticket-preview.component.scss'],
})
export class PosTicketPreviewComponent {
  readonly sellerTerminal = input<PosSellerTerminalView | null>(null);
  readonly draw = input<PosOpenDrawView | null>(null);
  readonly lines = input<PosTicketDraftLine[]>([]);
  readonly confirmedTicket = input<ConfirmedTicketView | null>(null);
  readonly games = input<PosGameView[]>([]);

  readonly now = new Date();

  readonly gameLabel = computed(() => {
    const lines = this.lines();
    if (!lines.length) return null;
    const code = lines[0].gameCode;
    return this.games().find(g => g.gameCode === code)?.label ?? code;
  });

  readonly displayLines = computed(() => {
    const gl = this.gameLabel() ?? '';
    return this.lines().map(l => ({
      gameLabel: gl,
      selection: l.selection,
      betType: l.betType,
      betTypeLabel: l.betTypeLabel,
      stakeAmount: l.stakeAmount,
    }));
  });

  readonly totalAmount = computed(() =>
    this.lines().reduce((s, l) => s + l.stakeAmount, 0),
  );

  readonly isConfirmed = computed(() => !!this.confirmedTicket());
}
