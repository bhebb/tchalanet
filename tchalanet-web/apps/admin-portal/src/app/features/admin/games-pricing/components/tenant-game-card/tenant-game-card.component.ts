import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { LowerCasePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TchCard, TchSectionError, TchSectionErrorSeverity, TchStatusBadge, BadgeStatus } from '@tch/ui/components';
import { AdminStatusPillComponent, AdminStatusTone } from '@tch/ui/console';
import { TenantGamePricingView, TenantGameStatus, ReadinessStatus } from '../../data-access/admin-games-pricing.models';
import { adminGameLogoText } from '../../data-access/admin-game-display';

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

const READINESS_BADGE: Record<ReadinessStatus, BadgeStatus> = {
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
  imports: [
    LowerCasePipe,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    TchCard,
    TchSectionError,
    TchStatusBadge,
    AdminStatusPillComponent,
  ],
  templateUrl: './tenant-game-card.component.html',
  styleUrls: ['./tenant-game-card.component.scss'],
})
export class TenantGameCardComponent {
  readonly game = input.required<TenantGamePricingView>();
  readonly actionError = input<TenantGameCardError | null>(null);

  readonly activate  = output<string>();
  readonly disable   = output<string>();
  readonly configure = output<string>();

  readonly statusTone    = computed<AdminStatusTone>(() => STATUS_TONE[this.game().tenantStatus]);
  readonly statusLabel   = computed<string>(() => STATUS_LABEL[this.game().tenantStatus]);
  readonly readinessBadge = computed<BadgeStatus>(() => READINESS_BADGE[this.game().readiness.status]);
  readonly logoText = computed(() => adminGameLogoText(this.game().gameCode, this.game().gameName));
  readonly systemLabel = computed(() => `Jeu système · ${this.game().gameName}`);
  readonly stakeLabel = computed(() => {
    if (this.game().tenantStatus === 'UNAVAILABLE') return 'Mise indisponible';
    return this.hasStakeConfig ? 'Mise configurée' : 'Mise non configurée';
  });
  readonly pricingLabel = computed(() => {
    const oddsCount = this.game().odds.length;
    if (oddsCount === 0) return 'Barème non configuré';
    const profile = this.game().pricingProfileLabel;
    return profile ? `${profile} · ${oddsCount} option${oddsCount > 1 ? 's' : ''}` : `${oddsCount} option${oddsCount > 1 ? 's' : ''} de barème`;
  });
  readonly limitLabel = computed(() => this.game().limits.maxPerDraw === null ? 'Limite tirage non configurée' : 'Limite tirage configurée');

  get hasStakeConfig(): boolean {
    const l = this.game().limits;
    return l.minStake !== null && l.maxStake !== null;
  }

  formatAmount(value: number | null, currency: string): string {
    if (value === null) return '—';
    return value >= 1000
      ? `${(value / 1000).toLocaleString('fr')}k ${currency}`
      : `${value.toLocaleString('fr')} ${currency}`;
  }
}
