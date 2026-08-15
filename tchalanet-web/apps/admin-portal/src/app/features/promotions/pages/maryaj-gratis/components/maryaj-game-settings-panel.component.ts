import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { TenantGamePricingView } from '../../../../games-pricing/data-access/admin-games-pricing.models';

@Component({
  selector: 'tch-maryaj-game-settings-panel',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe, MatButtonModule, MatIconModule, RouterLink],
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

  oddsSummary(): string {
    const odds = this.game()?.odds ?? [];
    const labels = odds.map(odd => this.gainOptionLabel(odd.pricingVariantCode, odd.label));
    return labels.length ? labels.join(' + ') : this.translate.instant('common.not_available');
  }

  readinessLabel(status: string): string {
    return this.translate.instant(`admin.maryajGratis.game.readiness.${status}`);
  }
}
