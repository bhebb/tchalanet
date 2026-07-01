import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';

import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import {
  AdminEmptyStateComponent,
  AdminPageShellComponent,
  AdminSectionCardComponent,
} from '@tch/ui/console';
import {
  AdminPromotionsApiService,
  PromotionConfigItem,
  PromotionCampaignStatus,
  PromotionCampaignView,
  PromotionIdRef,
  promotionIdValue,
} from '../../data-access/admin-promotions-api.service';

type PageState = 'loading' | 'ready' | 'error';

interface PromotionCatalogEntry {
  readonly title: string;
  readonly description: string;
  readonly icon: string;
  readonly effectType: string;
  readonly route?: string;
  readonly fragment?: string;
  readonly status: 'ready' | 'planned';
  readonly statusLabel: string;
}

@Component({
  selector: 'tch-admin-promotion-campaigns-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    RouterLink,
    MatButtonModule,
    MatExpansionModule,
    MatIconModule,
    AdminEmptyStateComponent,
    AdminPageShellComponent,
    AdminSectionCardComponent,
    TchErrorPanel,
    TchLoading,
  ],
  templateUrl: './admin-promotion-campaigns.page.html',
  styleUrls: ['./admin-promotion-campaigns.page.scss'],
})
export class AdminPromotionCampaignsPage implements OnInit {
  private readonly api = inject(AdminPromotionsApiService);
  private readonly snackBar = inject(MatSnackBar);

  readonly state = signal<PageState>('loading');
  readonly error = signal<string | null>(null);
  readonly saving = signal(false);
  readonly campaigns = signal<readonly PromotionCampaignView[]>([]);

  readonly activeCampaigns = computed(() => this.campaigns().filter(c => c.status === 'ACTIVE'));
  readonly promotionCatalog: readonly PromotionCatalogEntry[] = [
    {
      title: 'Maryaj gratis',
      description: 'Ajoute des lignes Maryaj gratuites selon le montant payé ou une quantité fixe.',
      icon: 'redeem',
      effectType: 'FREE_GAME_LINE',
      route: '/app/admin/maryaj-gratis',
      fragment: 'offer',
      status: 'ready',
      statusLabel: 'Workflow dédié',
    },
    {
      title: 'Bonus de cote',
      description: 'Applique une cote bonifiée sur un jeu, une période ou une condition de vente.',
      icon: 'trending_up',
      effectType: 'BOOST_ODDS',
      status: 'planned',
      statusLabel: 'Écran à créer',
    },
    {
      title: 'Frais offerts',
      description: 'Annule un frais configuré, par exemple une charge de service ou notification.',
      icon: 'money_off',
      effectType: 'WAIVE_CHARGE',
      status: 'planned',
      statusLabel: 'Écran à créer',
    },
  ];

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.state.set('loading');
    this.error.set(null);
    this.api.listCampaigns().subscribe({
      next: page => {
        this.campaigns.set(page.items);
        this.state.set('ready');
      },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string; detail?: string } })?.error;
        this.error.set(pd?.title ?? pd?.detail ?? 'Impossible de charger les campagnes.');
        this.state.set('error');
      },
    });
  }

  statusLabel(status: PromotionCampaignStatus): string {
    switch (status) {
      case 'ACTIVE': return 'Actif';
      case 'PAUSED': return 'En pause';
      case 'DRAFT': return 'Brouillon';
      case 'INACTIVE': return 'Inactif';
      case 'ARCHIVED': return 'Archivé';
    }
  }

  eligibilityLabel(item: PromotionConfigItem): string {
    switch (item.type) {
      case 'MIN_PAID_TOTAL':
        return `Vente payante minimum ${this.param(item, 'amount', '1')}`;
      case 'PAID_LINE_COUNT':
        return `Au moins ${this.param(item, 'minCount', '1')} ligne(s) ${this.param(item, 'gameCode', '')}`.trim();
      case 'BEFORE_LOCAL_TIME':
        return `Avant ${this.param(item, 'time', 'heure configurée')}`;
      default:
        return this.humanizeCode(item.type);
    }
  }

  effectLabel(item: PromotionConfigItem): string {
    switch (item.type) {
      case 'FREE_GAME_LINE':
        return [
          `${this.param(item, 'quantity', '1')} ligne(s) gratuite(s)`,
          this.param(item, 'gameCode', 'jeu configuré'),
          `${this.param(item, 'payoutBaseAmount', '50')} HTG`,
          this.choiceModeLabel(this.param(item, 'choiceMode', 'AUTO_GENERATE')),
        ].join(' · ');
      case 'BOOST_ODDS':
        return `Cote bonifiée ${this.param(item, 'oddsOverride', '')}`.trim();
      case 'WAIVE_CHARGE':
        return `Frais supprimés ${this.param(item, 'chargeType', '')}`.trim();
      default:
        return this.humanizeCode(item.type);
    }
  }

  activate(campaign: PromotionCampaignView): void {
    this.transition(campaign, 'activate');
  }

  pause(campaign: PromotionCampaignView): void {
    this.transition(campaign, 'pause');
  }

  idValue(id: PromotionIdRef): string {
    return promotionIdValue(id) ?? '';
  }

  private transition(campaign: PromotionCampaignView, action: 'activate' | 'pause'): void {
    if (this.saving()) return;
    this.saving.set(true);
    const campaignId = promotionIdValue(campaign.id);
    if (!campaignId) {
      this.saving.set(false);
      this.snackBar.open('Impossible de retrouver la campagne à modifier.', 'OK', { duration: 5000 });
      return;
    }
    const request =
      action === 'activate'
        ? this.api.activateCampaign(campaignId)
        : this.api.pauseCampaign(campaignId);

    request.subscribe({
      next: updated => {
        const updatedId = promotionIdValue(updated.id);
        this.campaigns.update(items => items.map(i => (promotionIdValue(i.id) === updatedId ? updated : i)));
        this.saving.set(false);
      },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string; detail?: string } })?.error;
        this.saving.set(false);
        this.snackBar.open(pd?.title ?? pd?.detail ?? 'Erreur lors de la mise à jour.', 'OK', {
          duration: 5000,
        });
      },
    });
  }

  private param(item: PromotionConfigItem, name: string, fallback: string): string {
    const value = item.params?.[name];
    return value == null || value === '' ? fallback : String(value);
  }

  private choiceModeLabel(value: string): string {
    return value === 'SELLER_SELECTS' ? 'choix vendeur' : 'génération automatique';
  }

  private humanizeCode(value: string): string {
    return value.toLowerCase().replace(/_/g, ' ');
  }
}
