import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';
import { Router } from '@angular/router';
import { TchSectionErrorSeverity } from '@tch/ui/components';
import { AdminStatusTone } from '@tch/ui/console';
import {
  ConsoleGameCardActionEvent,
  ConsoleGameCardComponent,
  ConsoleGameCardView,
  consoleGameLogoText,
} from '@tch/web/console';
import { TenantGamePricingView, TenantGameStatus, ReadinessStatus } from '../../data-access/admin-games-pricing.models';

const STATUS_TONE: Record<TenantGameStatus, AdminStatusTone> = {
  ACTIVE:       'success',
  NEEDS_CONFIG: 'warning',
  INACTIVE:     'neutral',
  UNAVAILABLE:  'danger',
};

const STATUS_LABEL: Record<TenantGameStatus, string> = {
  ACTIVE:       'Actif',
  NEEDS_CONFIG: 'À configurer',
  INACTIVE:     'Inactif',
  UNAVAILABLE:  'Non disponible',
};

const READINESS_BADGE: Record<ReadinessStatus, ConsoleGameCardView['badgeTone']> = {
  READY:   'ready',
  TODO:    'warning',
  BLOCKED: 'blocked',
};

export interface TenantGameCardError {
  readonly title: string;
  readonly message: string;
  readonly severity?: TchSectionErrorSeverity;
}

@Component({
  selector: 'tch-tenant-game-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ConsoleGameCardComponent],
  templateUrl: './tenant-game-card.component.html',
})
export class TenantGameCardComponent {
  private readonly router = inject(Router);

  readonly game = input.required<TenantGamePricingView>();
  readonly actionError = input<TenantGameCardError | null>(null);

  readonly activate  = output<string>();
  readonly disable   = output<string>();
  readonly configure = output<string>();

  readonly card = computed<ConsoleGameCardView>(() => {
    const game = this.game();
    return {
      id: game.gameCode,
      code: game.gameCode,
      name: game.gameName,
      logoText: consoleGameLogoText(game.gameCode, game.gameName),
      statusLabel: STATUS_LABEL[game.tenantStatus],
      statusTone: STATUS_TONE[game.tenantStatus],
      badgeLabel: game.readiness.label,
      badgeTone: READINESS_BADGE[game.readiness.status],
      unavailable: game.tenantStatus === 'UNAVAILABLE',
      unavailableLabel: 'Bientôt disponible',
      summaryItems: game.tenantStatus === 'UNAVAILABLE' ? [] : [
        { icon: 'casino', label: `Jeu système · ${game.gameName}` },
        { icon: 'payments', label: this.stakeLabel(game), warning: !this.hasStakeConfig(game) },
        { icon: 'price_change', label: this.pricingLabel(game), warning: game.odds.length === 0 },
        { icon: 'shield', label: this.limitLabel(game), warning: game.limits.maxPerDraw === null },
      ],
      actions: this.primaryActions(game),
      secondaryActions: game.tenantStatus === 'UNAVAILABLE' ? [] : [
        ...(game.gameCode === 'HT_MARYAJ_GRATUIT'
          ? [{ id: 'maryaj-gratis', label: 'Configurer Maryaj gratis', icon: 'redeem' }]
          : []),
        { id: 'limits', label: 'Configurer les limites', icon: 'shield' },
        { id: 'pricing', label: 'Voir les barèmes', icon: 'format_list_numbered' },
      ],
    };
  });

  onCardAction(event: ConsoleGameCardActionEvent): void {
    switch (event.action.id) {
      case 'activate':
        this.activate.emit(event.row.code);
        break;
      case 'disable':
        this.disable.emit(event.row.code);
        break;
      case 'configure':
        this.configure.emit(event.row.code);
        break;
      case 'maryaj-gratis':
        void this.router.navigate(['/app/admin/maryaj-gratis'], { fragment: 'game' });
        break;
      case 'limits':
        void this.router.navigate(['/app/admin/limits']);
        break;
      case 'pricing':
        void this.router.navigate(['/app/admin/pricing']);
        break;
    }
  }

  private hasStakeConfig(game: TenantGamePricingView): boolean {
    const l = game.limits;
    return l.minStake !== null && l.maxStake !== null;
  }

  private stakeLabel(game: TenantGamePricingView): string {
    if (game.tenantStatus === 'UNAVAILABLE') return 'Mise indisponible';
    return this.hasStakeConfig(game) ? 'Mise configurée' : 'Mise non configurée';
  }

  private pricingLabel(game: TenantGamePricingView): string {
    const oddsCount = game.odds.length;
    if (oddsCount === 0) return 'Barème non configuré';
    const profile = game.pricingProfileLabel;
    return profile ? `${profile} · ${oddsCount} option${oddsCount > 1 ? 's' : ''}` : `${oddsCount} option${oddsCount > 1 ? 's' : ''} de barème`;
  }

  private limitLabel(game: TenantGamePricingView): string {
    return game.limits.maxPerDraw === null ? 'Limite tirage non configurée' : 'Limite tirage configurée';
  }

  private primaryActions(game: TenantGamePricingView): ConsoleGameCardView['actions'] {
    switch (game.tenantStatus) {
      case 'ACTIVE':
        return [
          { id: 'configure', label: 'Configurer' },
          { id: 'disable', label: 'Désactiver', tone: 'danger' },
        ];
      case 'NEEDS_CONFIG':
        return [
          { id: 'configure', label: 'Configurer', tone: 'primary' },
          { id: 'activate', label: 'Activer', disabled: true },
        ];
      case 'INACTIVE':
        return [
          { id: 'activate', label: 'Réactiver', tone: 'primary' },
          { id: 'configure', label: 'Modifier' },
        ];
      case 'UNAVAILABLE':
        return [{ id: 'unavailable', label: 'Non disponible', disabled: true }];
    }
  }
}
