import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { AdminSectionCardComponent } from '@tch/ui/console';

export interface SellerTerminalDetailFact {
  readonly labelKey: string;
  readonly value?: string | number | boolean | null;
  readonly valueKey?: string | null;
  readonly kind?: 'text' | 'date' | 'percent' | 'boolean';
}

@Component({
  selector: 'tch-seller-terminal-detail-facts-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, DecimalPipe, AdminSectionCardComponent, TranslatePipe],
  templateUrl: './seller-terminal-detail-facts-card.component.html',
  styleUrls: ['./seller-terminal-detail-facts-card.component.scss'],
})
export class SellerTerminalDetailFactsCardComponent {
  readonly titleKey = input.required<string>();
  readonly descriptionKey = input<string | null>(null);
  readonly icon = input.required<string>();
  readonly facts = input.required<readonly SellerTerminalDetailFact[]>();
  readonly compact = input(false);
}
