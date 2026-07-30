import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { TenantGamePricingView } from '../../../../games-pricing/data-access/admin-games-pricing.models';

@Component({
  selector: 'tch-maryaj-game-settings-panel',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe, MatButtonModule, MatIconModule],
  templateUrl: './maryaj-game-settings-panel.component.html',
  styleUrls: ['./maryaj-game-settings-panel.component.scss'],
})
export class MaryajGameSettingsPanelComponent {
  private readonly translate = inject(TranslateService);

  readonly game = input<TenantGamePricingView | null>(null);
  readonly configure = output<TenantGamePricingView>();

  formatAmount(value: number | null, currency: string): string {
    if (value === null) return this.translate.instant('admin.maryajGratis.game.notDefined');
    return `${value.toLocaleString('fr')} ${currency}`;
  }

  gainOptionLabel(pricingVariantCode: string | null, fallback: string): string {
    if (pricingVariantCode === 'MARRIAGE_EXACT_ORDER') {
      return this.translate.instant('admin.maryajGratis.game.exact');
    }
    if (pricingVariantCode === 'MARRIAGE_REVERSE_ALLOWED') {
      return this.translate.instant('admin.maryajGratis.game.reverse');
    }
    return fallback;
  }

  oddsLabel(odds: number | null): string {
    if (odds === null) return this.translate.instant('admin.maryajGratis.game.notConfigured');
    return `x${odds.toLocaleString('fr')}`;
  }

  readinessLabel(status: string): string {
    return this.translate.instant(`admin.maryajGratis.game.readiness.${status}`);
  }
}
