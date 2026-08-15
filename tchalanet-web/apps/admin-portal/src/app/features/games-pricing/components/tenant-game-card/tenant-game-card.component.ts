import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { TchSectionErrorSeverity } from '@tch/ui/components';
import { AdminStatusPillComponent, AdminStatusTone } from '@tch/ui/console';
import {
  consoleGameLogoUrl,
  consoleGameLogoText,
} from '@tch/web/console';
import { TenantGamePricingView, TenantGameStatus } from '../../data-access/admin-games-pricing.models';
import { TenantGameSettingsSummaryComponent } from '../tenant-game-settings-summary/tenant-game-settings-summary.component';

const STATUS_TONE: Record<TenantGameStatus, AdminStatusTone> = {
  ACTIVE:       'success',
  NEEDS_CONFIG: 'warning',
  INACTIVE:     'neutral',
  UNAVAILABLE:  'danger',
};

const MARYAJ_GRATIS_GAME_CODES = new Set(['HT_MARYAJ_GRATIS', 'HT_MARYAJ_GRATUIT']);
type TenantGameListStatus = 'active' | 'attention' | 'inactive' | 'unavailable';

export interface TenantGameCardError {
  readonly title: string;
  readonly message: string;
  readonly severity?: TchSectionErrorSeverity;
}

@Component({
  selector: 'tch-tenant-game-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe, AdminStatusPillComponent, TenantGameSettingsSummaryComponent],
  templateUrl: './tenant-game-card.component.html',
  styleUrls: ['./tenant-game-card.component.scss'],
})
export class TenantGameCardComponent {
  readonly game = input.required<TenantGamePricingView>();
  readonly actionError = input<TenantGameCardError | null>(null);
  readonly saving = input(false);
  readonly pendingEnabled = input<boolean | null>(null);
  readonly canManage = input(true);

  readonly activate  = output<string>();
  readonly disable   = output<string>();
  readonly configure = output<string>();

  readonly logoUrl = computed(() => consoleGameLogoUrl(this.game().gameCode));
  readonly logoText = computed(() => consoleGameLogoText(this.game().gameCode, this.game().gameName));

  onToggle(enabled: boolean): void {
    const game = this.game();
    if (!this.canManage() || game.tenantStatus === 'UNAVAILABLE') return;
    if (enabled) this.activate.emit(game.gameCode);
    else this.disable.emit(game.gameCode);
  }

  onConfigure(game: TenantGamePricingView): void {
    if (!this.canManage()) return;
    this.configure.emit(game.gameCode);
  }

  private hasBlockingItems(game: TenantGamePricingView): boolean {
    return !this.hasStakeConfig(game) || !this.hasPricingConfig(game);
  }

  private hasStakeConfig(game: TenantGamePricingView): boolean {
    const l = game.limits;
    return l.minStake !== null && l.maxStake !== null;
  }

  private hasPricingConfig(game: TenantGamePricingView): boolean {
    return game.odds.length > 0 && game.odds.every(odd => odd.odds !== null && odd.odds !== undefined);
  }

  cardStatusTone(game: TenantGamePricingView): AdminStatusTone {
    if (game.tenantStatus !== 'UNAVAILABLE' && this.hasBlockingItems(game)) return 'warning';
    return STATUS_TONE[game.tenantStatus];
  }

  cardStatus(game: TenantGamePricingView): TenantGameListStatus {
    if (game.tenantStatus === 'UNAVAILABLE') return 'unavailable';
    if (game.tenantStatus === 'INACTIVE') return 'inactive';
    if (this.hasBlockingItems(game)) return 'attention';
    return 'active';
  }

  statusLabelKey(game: TenantGamePricingView): string {
    if (game.tenantStatus !== 'UNAVAILABLE' && this.hasBlockingItems(game)) {
      return 'admin.gamesPricing.readiness.TODO';
    }
    return `admin.gamesPricing.status.${game.tenantStatus}`;
  }

  saleEnabled(game: TenantGamePricingView): boolean {
    const pending = this.pendingEnabled();
    if (pending !== null) return pending;
    return game.tenantStatus === 'ACTIVE' || game.tenantStatus === 'NEEDS_CONFIG';
  }

  isMaryajGratis(gameCode: string): boolean {
    return MARYAJ_GRATIS_GAME_CODES.has(gameCode);
  }

  configureLabelKey(game: TenantGamePricingView): string {
    if (this.isMaryajGratis(game.gameCode)) return 'admin.gamesPricing.card.action.maryaj';
    return this.hasBlockingItems(game)
      ? 'admin.gamesPricing.card.action.complete'
      : 'admin.gamesPricing.card.action.configure';
  }
}
