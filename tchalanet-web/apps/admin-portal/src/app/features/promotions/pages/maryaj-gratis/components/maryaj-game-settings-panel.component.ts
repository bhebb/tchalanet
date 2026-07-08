import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { TenantGamePricingView } from '../../../../games-pricing/data-access/admin-games-pricing.models';

@Component({
  selector: 'tch-maryaj-game-settings-panel',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatIconModule],
  templateUrl: './maryaj-game-settings-panel.component.html',
  styleUrls: ['./maryaj-game-settings-panel.component.scss'],
})
export class MaryajGameSettingsPanelComponent {
  readonly game = input<TenantGamePricingView | null>(null);
  readonly configure = output<TenantGamePricingView>();

  formatAmount(value: number | null, currency: string): string {
    if (value === null) return 'Non défini';
    return `${value.toLocaleString('fr')} ${currency}`;
  }

  gainOptionLabel(pricingVariantCode: string | null, fallback: string): string {
    if (pricingVariantCode === 'MARRIAGE_EXACT_ORDER') return 'Exact';
    if (pricingVariantCode === 'MARRIAGE_REVERSE_ALLOWED') return 'Reverse';
    return fallback;
  }

  oddsLabel(odds: number | null): string {
    if (odds === null) return 'Non configuré';
    return `x${odds.toLocaleString('fr')}`;
  }
}
