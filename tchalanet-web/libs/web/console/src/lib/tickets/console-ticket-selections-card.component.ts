import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { TchGameSelectionChip } from '@tch/ui/components';
import { AdminSectionCardComponent } from '@tch/ui/console';

export interface ConsoleTicketSelectionView {
  readonly lineNumber?: number | string | null;
  readonly gameCode: string;
  readonly gameLabel: string;
  readonly selection: string;
  readonly betTypeLabel?: string | null;
  readonly amountLabel: string;
  readonly promotional?: boolean;
  readonly promotionLabel?: string | null;
}

@Component({
  selector: 'tch-console-ticket-selections-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AdminSectionCardComponent, TchGameSelectionChip],
  templateUrl: './console-ticket-selections-card.component.html',
  styleUrls: ['./console-ticket-selections-card.component.scss'],
})
export class ConsoleTicketSelectionsCardComponent {
  readonly title = input.required<string>();
  readonly lines = input.required<readonly ConsoleTicketSelectionView[]>();
  readonly emptyLabel = input<string | null>(null);
  readonly icon = input('casino');
}
