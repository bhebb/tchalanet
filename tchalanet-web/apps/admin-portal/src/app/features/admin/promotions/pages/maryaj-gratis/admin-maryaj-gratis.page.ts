import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';

import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import {
  AdminEmptyStateComponent,
  AdminPageShellComponent,
  AdminSectionCardComponent,
} from '@tch/ui/console';
import {
  AdminPromotionsApiService,
  PromotionCampaignStatus,
  PromotionCampaignView,
} from '../../data-access/admin-promotions-api.service';
import { AdminGamesPricingApiService } from '../../../games-pricing/data-access/admin-games-pricing-api.service';
import { TenantGamePricingView } from '../../../games-pricing/data-access/admin-games-pricing.models';

type PageState = 'loading' | 'ready' | 'error';

@Component({
  selector: 'tch-admin-maryaj-gratis-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    AdminEmptyStateComponent,
    AdminPageShellComponent,
    AdminSectionCardComponent,
    TchErrorPanel,
    TchLoading,
  ],
  templateUrl: './admin-maryaj-gratis.page.html',
  styleUrls: ['./admin-maryaj-gratis.page.scss'],
})
export class AdminMaryajGratisPage implements OnInit {
  private readonly api = inject(AdminPromotionsApiService);
  private readonly gamesPricingApi = inject(AdminGamesPricingApiService);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);

  readonly state = signal<PageState>('loading');
  readonly error = signal<string | null>(null);
  readonly saving = signal(false);
  readonly campaigns = signal<readonly PromotionCampaignView[]>([]);
  readonly maryajGame = signal<TenantGamePricingView | null>(null);

  readonly maryajCampaign = computed(() =>
    this.campaigns().find(c => c.code === 'DEFAULT_MARYAJ_GRATIS' || c.code.includes('MARYAJ')),
  );

  readonly isConfigured = computed(() => !!this.maryajCampaign());
  readonly isMaryajGameReady = computed(() => this.maryajGame()?.tenantStatus === 'ACTIVE');
  readonly maryajGameMissingReason = computed(() => {
    const game = this.maryajGame();
    if (!game) return 'Le jeu HT_MARYAJ_GRATUIT est absent de la configuration tenant.';
    if (game.tenantStatus === 'INACTIVE') return 'Le jeu Maryaj gratuit est désactivé pour ce tenant.';
    if (game.tenantStatus === 'NEEDS_CONFIG') return 'Le jeu Maryaj gratuit doit avoir ses limites et son barème configurés.';
    if (game.tenantStatus === 'UNAVAILABLE') return 'Le jeu Maryaj gratuit n’est pas disponible pour ce tenant.';
    return null;
  });
  readonly maryajEffect = computed(() =>
    this.maryajCampaign()
      ?.rules.flatMap(r => r.effects)
      .find(e => e.type === 'FREE_GAME_LINE') ?? null,
  );

  readonly form = this.fb.nonNullable.group({
    payoutBaseAmount: [50, [Validators.required, Validators.min(1)]],
    quantity: [1, [Validators.required, Validators.min(1), Validators.max(10)]],
    choiceMode: ['AUTO_GENERATE' as 'AUTO_GENERATE' | 'SELLER_SELECTS', Validators.required],
    regenerableBeforeConfirm: [true],
    maxRegenerationsBeforeConfirm: [3, [Validators.required, Validators.min(0), Validators.max(20)]],
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.state.set('loading');
    this.error.set(null);
    forkJoin({
      campaigns: this.api.listCampaigns(),
      games: this.gamesPricingApi.getGamesPricing(),
    }).subscribe({
      next: ({ campaigns, games }) => {
        this.campaigns.set(campaigns.items);
        this.maryajGame.set(games.find(g => g.gameCode === 'HT_MARYAJ_GRATUIT') ?? null);
        this.state.set('ready');
      },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string; detail?: string } })?.error;
        this.error.set(pd?.title ?? pd?.detail ?? 'Impossible de charger les promotions.');
        this.state.set('error');
      },
    });
  }

  instantiate(): void {
    if (this.saving()) return;
    if (!this.isMaryajGameReady()) {
      this.snackBar.open(
        this.maryajGameMissingReason() ?? 'Configurez le jeu Maryaj gratuit avant activation.',
        'OK',
        { duration: 5000 },
      );
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    const value = this.form.getRawValue();
    this.api.instantiateDefaultMaryajGratis({
      payoutBaseAmount: value.payoutBaseAmount,
      quantity: value.quantity,
      choiceMode: value.choiceMode,
      generationStrategy: value.choiceMode === 'AUTO_GENERATE' ? 'RANDOM' : null,
      regenerableBeforeConfirm: value.regenerableBeforeConfirm,
      maxRegenerationsBeforeConfirm: value.maxRegenerationsBeforeConfirm,
    }).subscribe({
      next: campaign => {
        this.campaigns.update(items => [campaign, ...items.filter(i => i.id.value !== campaign.id.value)]);
        this.saving.set(false);
        this.snackBar.open('Maryaj gratis activé.', 'OK', { duration: 3000 });
      },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string; detail?: string } })?.error;
        this.saving.set(false);
        this.snackBar.open(pd?.title ?? pd?.detail ?? 'Erreur lors de l’activation.', 'OK', {
          duration: 5000,
        });
      },
    });
  }

  activate(campaign: PromotionCampaignView): void {
    this.transition(campaign, 'activate');
  }

  pause(campaign: PromotionCampaignView): void {
    this.transition(campaign, 'pause');
  }

  statusLabel(status: PromotionCampaignStatus): string {
    switch (status) {
      case 'ACTIVE':
        return 'Actif';
      case 'PAUSED':
        return 'En pause';
      case 'DRAFT':
        return 'Brouillon';
      case 'INACTIVE':
        return 'Inactif';
      case 'ARCHIVED':
        return 'Archivé';
    }
  }

  effectParam(name: string): string {
    const value = this.maryajEffect()?.params?.[name];
    return value == null || value === '' ? '—' : String(value);
  }

  private transition(campaign: PromotionCampaignView, action: 'activate' | 'pause'): void {
    if (this.saving()) return;
    this.saving.set(true);
    const request =
      action === 'activate'
        ? this.api.activateCampaign(campaign.id.value)
        : this.api.pauseCampaign(campaign.id.value);

    request.subscribe({
      next: updated => {
        this.campaigns.update(items => items.map(i => (i.id.value === updated.id.value ? updated : i)));
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
}
