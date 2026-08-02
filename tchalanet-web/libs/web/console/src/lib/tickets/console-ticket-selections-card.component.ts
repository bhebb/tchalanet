import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
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
  readonly pricingLabels?: readonly ConsoleTicketPricingLabel[];
}

export interface ConsoleTicketPricingLabel {
  readonly value: string;
  readonly source?: string | null;
}

export interface ConsoleTicketSelectionGroup {
  readonly gameCode: string;
  readonly gameLabel: string;
  readonly lines: readonly ConsoleTicketSelectionView[];
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

  readonly groups = computed<readonly ConsoleTicketSelectionGroup[]>(() => {
    const grouped = new Map<string, { gameLabel: string; lines: ConsoleTicketSelectionView[] }>();
    for (const line of this.lines()) {
      const current = grouped.get(line.gameCode) ?? { gameLabel: line.gameLabel, lines: [] };
      current.lines.push(line);
      grouped.set(line.gameCode, current);
    }
    return [...grouped.entries()].map(([gameCode, value]) => ({
      gameCode,
      gameLabel: value.gameLabel,
      lines: value.lines,
    }));
  });
}
