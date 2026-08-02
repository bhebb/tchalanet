import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
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
  readonly pricingTooltip?: string | null;
  readonly pricingAppliedLabel?: string | null;
}

export interface ConsoleTicketPricingLabel {
  readonly value: string;
  readonly source?: string | null;
}

export interface ConsoleTicketSelectionGroup {
  readonly gameCode: string;
  readonly gameLabel: string;
  readonly pricingTooltip?: string | null;
  readonly lines: readonly ConsoleTicketSelectionView[];
}

@Component({
  selector: 'tch-console-ticket-selections-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminSectionCardComponent,
    TchGameSelectionChip,
    MatIconModule,
    MatMenuModule,
    MatTooltipModule,
  ],
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
      pricingTooltip: this.sharedPricingTooltip(value.lines),
      lines: value.lines,
    }));
  });

  private sharedPricingTooltip(lines: readonly ConsoleTicketSelectionView[]): string | null {
    if (lines.length === 0) return null;
    if (lines.some(line => !!line.pricingAppliedLabel)) return null;
    const first = lines[0]?.pricingTooltip?.trim();
    if (!first) return null;
    return lines.every(line => line.pricingTooltip?.trim() === first) ? first : null;
  }
}
