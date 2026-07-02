import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { LowerCasePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TchCard, TchSectionError, TchSectionErrorSeverity, TchStatusBadge, BadgeStatus } from '@tch/ui/components';
import { AdminStatusPillComponent, AdminStatusTone } from '@tch/ui/console';
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

export interface TenantGameChannelStats {
  readonly offeredChannelCount: number;
  readonly activeChannelCount: number;
  readonly readyChannelCount: number;
  readonly missingStakeChannelCount: number;
  readonly missingLimitChannelCount: number;
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
  readonly channelStats = input<TenantGameChannelStats>({
    offeredChannelCount:       0,
    activeChannelCount:        0,
    readyChannelCount:         0,
    missingStakeChannelCount:  0,
    missingLimitChannelCount:  0,
  });
  readonly actionError = input<TenantGameCardError | null>(null);

  readonly activate  = output<string>();
  readonly disable   = output<string>();
  readonly configure = output<string>();

  readonly statusTone    = computed<AdminStatusTone>(() => STATUS_TONE[this.game().tenantStatus]);
  readonly statusLabel   = computed<string>(() => STATUS_LABEL[this.game().tenantStatus]);
  readonly readinessBadge = computed<BadgeStatus>(() => READINESS_BADGE[this.game().readiness.status]);
  readonly channelLabel = computed(() => {
    const count = this.channelStats().activeChannelCount;
    return count > 0 ? `Actif sur ${count} canal${count > 1 ? 'aux' : ''}` : 'Non actif sur les canaux';
  });
  readonly stakeLabel = computed(() => {
    const missing = this.channelStats().missingStakeChannelCount;
    if (this.game().tenantStatus === 'UNAVAILABLE') return 'Mise indisponible';
    if (missing > 0) return `Mise manquante sur ${missing} canal${missing > 1 ? 'aux' : ''}`;
    return this.hasStakeConfig ? 'Mise configurée' : 'Mise non configurée';
  });
  readonly limitsLabel = computed(() => {
    const missing = this.channelStats().missingLimitChannelCount;
    if (this.game().tenantStatus === 'UNAVAILABLE') return 'Limites indisponibles';
    return missing > 0 ? `Limites à vérifier sur ${missing} canal${missing > 1 ? 'aux' : ''}` : 'Limites OK';
  });

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
