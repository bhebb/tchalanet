import { DestroyRef, Injectable, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormArray, FormBuilder, Validators } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { forkJoin } from 'rxjs';

import {
  AdminPromotionsApiService,
  MaryajQuantityTier,
  PromotionConfigItem,
  PromotionCampaignView,
  PromotionRuleView,
  promotionIdValue,
} from '../../data-access/admin-promotions-api.service';
import { AdminGamesPricingApiService } from '../../../games-pricing/data-access/admin-games-pricing-api.service';
import { TenantGamePricingView } from '../../../games-pricing/data-access/admin-games-pricing.models';

export type AdminMaryajGratisPageState = 'loading' | 'ready' | 'error';

@Injectable()
export class AdminMaryajGratisStore {
  private readonly api = inject(AdminPromotionsApiService);
  private readonly gamesPricingApi = inject(AdminGamesPricingApiService);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);

  readonly state = signal<AdminMaryajGratisPageState>('loading');
  readonly error = signal<string | null>(null);
  readonly saving = signal(false);
  readonly editingOffer = signal(false);
  readonly campaigns = signal<readonly PromotionCampaignView[]>([]);
  readonly maryajGame = signal<TenantGamePricingView | null>(null);

  readonly maryajCampaign = computed(() =>
    this.campaigns().find(c => c.code === 'DEFAULT_MARYAJ_GRATIS' || c.code.includes('MARYAJ')),
  );

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
    this.findMaryajEffect(this.maryajCampaign() ?? null),
  );
  readonly maryajRule = computed(() => this.findMaryajRule(this.maryajCampaign() ?? null));

  readonly form = this.fb.nonNullable.group({
    payoutBaseAmount: [50, [Validators.required, Validators.min(1)]],
    quantityMode: ['TIERED_PAID_AMOUNT' as 'FIXED' | 'PER_PAID_AMOUNT' | 'TIERED_PAID_AMOUNT', Validators.required],
    quantity: [5, [Validators.required, Validators.min(1), Validators.max(10)]],
    stepPaidAmount: [1000, [Validators.required, Validators.min(1)]],
    quantityPerStep: [2, [Validators.required, Validators.min(1), Validators.max(10)]],
    maxQuantity: [10, [Validators.required, Validators.min(1), Validators.max(50)]],
    quantityTiers: this.fb.array([
      this.quantityTierGroup(100, 199, 1),
      this.quantityTierGroup(200, 499, 2),
      this.quantityTierGroup(500, null, 3),
    ]),
    choiceMode: ['AUTO_GENERATE' as 'AUTO_GENERATE' | 'SELLER_SELECTS', Validators.required],
    regenerableBeforeConfirm: [true],
    maxRegenerationsBeforeConfirm: [3, [Validators.required, Validators.min(0), Validators.max(20)]],
  });

  constructor() {
    this.syncQuantityModeControls(this.form.controls.quantityMode.value);
    this.form.controls.quantityMode.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(mode => this.syncQuantityModeControls(mode));
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
    const quantityTiers = this.quantityTiersPayload();
    this.api.instantiateDefaultMaryajGratis({
      payoutBaseAmount: value.payoutBaseAmount,
      quantityMode: value.quantityMode,
      quantity: value.quantity,
      stepPaidAmount: value.quantityMode === 'PER_PAID_AMOUNT' ? value.stepPaidAmount : null,
      quantityPerStep: value.quantityMode === 'PER_PAID_AMOUNT' ? value.quantityPerStep : null,
      maxQuantity: value.quantityMode === 'PER_PAID_AMOUNT'
        ? value.maxQuantity
        : value.quantityMode === 'TIERED_PAID_AMOUNT'
          ? Math.max(...quantityTiers.map(tier => tier.quantity))
          : value.quantity,
      quantityTiers: value.quantityMode === 'TIERED_PAID_AMOUNT' ? quantityTiers : null,
      choiceMode: value.choiceMode,
      generationStrategy: value.choiceMode === 'AUTO_GENERATE' ? 'RANDOM' : null,
      regenerableBeforeConfirm: value.regenerableBeforeConfirm,
      maxRegenerationsBeforeConfirm: value.maxRegenerationsBeforeConfirm,
    }).subscribe({
      next: campaign => {
        const campaignId = promotionIdValue(campaign.id);
        this.campaigns.update(items => [campaign, ...items.filter(i => promotionIdValue(i.id) !== campaignId)]);
        const effect = this.findMaryajEffect(campaign);
        if (effect) {
          this.patchFormFromEffect(effect);
        }
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

  startEditingOffer(): void {
    const effect = this.maryajEffect();
    if (effect) {
      this.patchFormFromEffect(effect);
    }
    this.editingOffer.set(true);
  }

  cancelEditingOffer(): void {
    this.editingOffer.set(false);
    const effect = this.maryajEffect();
    if (effect) {
      this.patchFormFromEffect(effect);
    }
  }

  saveOffer(): void {
    const campaign = this.maryajCampaign();
    const rule = this.maryajRule();
    if (!campaign || !rule) {
      this.instantiate();
      return;
    }
    if (this.saving()) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const campaignId = promotionIdValue(campaign.id);
    const ruleId = promotionIdValue(rule.id);
    if (!campaignId || !ruleId) {
      this.snackBar.open('Impossible de retrouver la règle Maryaj gratis à modifier.', 'OK', {
        duration: 5000,
      });
      return;
    }

    this.saving.set(true);
    this.api.updateRuleEffects(campaignId, ruleId, {
      items: [this.freeGameLineEffectItem()],
    }).subscribe({
      next: updated => {
        this.replaceCampaign(updated);
        const effect = this.findMaryajEffect(updated);
        if (effect) {
          this.patchFormFromEffect(effect);
        }
        this.editingOffer.set(false);
        this.saving.set(false);
        this.snackBar.open('Configuration Maryaj gratis mise à jour.', 'OK', { duration: 3000 });
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

  quantityTiers(): FormArray {
    return this.form.controls.quantityTiers;
  }

  addQuantityTier(): void {
    const tiers = this.quantityTiers();
    const last = tiers.at(tiers.length - 1)?.getRawValue() as Partial<MaryajQuantityTier> | undefined;
    const nextMin = typeof last?.maxPaidAmount === 'number' ? last.maxPaidAmount + 1 : 1000;
    tiers.push(this.quantityTierGroup(nextMin, null, 1));
  }

  removeQuantityTier(index: number): void {
    const tiers = this.quantityTiers();
    if (tiers.length <= 1) return;
    tiers.removeAt(index);
  }

  activate(campaign: PromotionCampaignView): void {
    this.transition(campaign, 'activate');
  }

  pause(campaign: PromotionCampaignView): void {
    this.transition(campaign, 'pause');
  }

  private transition(campaign: PromotionCampaignView, action: 'activate' | 'pause'): void {
    if (this.saving()) return;
    this.saving.set(true);
    const campaignId = promotionIdValue(campaign.id);
    if (!campaignId) {
      this.saving.set(false);
      this.snackBar.open('Impossible de retrouver la campagne Maryaj gratis.', 'OK', {
        duration: 5000,
      });
      return;
    }
    const request =
      action === 'activate'
        ? this.api.activateCampaign(campaignId)
        : this.api.pauseCampaign(campaignId);

    request.subscribe({
      next: updated => {
        this.replaceCampaign(updated);
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

  private freeGameLineEffectItem(): PromotionConfigItem {
    const value = this.form.getRawValue();
    const quantityTiers = this.quantityTiersPayload();
    const params: Record<string, unknown> = {
      gameCode: 'HT_MARYAJ_GRATUIT',
      payoutBaseAmount: value.payoutBaseAmount,
      quantityMode: value.quantityMode,
      quantity: value.quantity,
      choiceMode: value.choiceMode,
      regenerableBeforeConfirm: value.regenerableBeforeConfirm,
      maxRegenerationsBeforeConfirm: value.maxRegenerationsBeforeConfirm,
    };

    if (value.quantityMode === 'PER_PAID_AMOUNT') {
      params['stepPaidAmount'] = value.stepPaidAmount;
      params['quantityPerStep'] = value.quantityPerStep;
      params['maxQuantity'] = value.maxQuantity;
    }
    if (value.quantityMode === 'TIERED_PAID_AMOUNT') {
      params['quantityTiers'] = quantityTiers;
      params['maxQuantity'] = Math.max(...quantityTiers.map(tier => tier.quantity));
    }
    if (value.choiceMode === 'AUTO_GENERATE') {
      params['generationStrategy'] = 'RANDOM';
    }

    return {
      type: 'FREE_GAME_LINE',
      params,
    };
  }

  private patchFormFromEffect(effect: PromotionConfigItem): void {
    const params = effect.params;
    const quantityMode = this.stringParam(params, 'quantityMode', 'TIERED_PAID_AMOUNT') as
      | 'FIXED'
      | 'PER_PAID_AMOUNT'
      | 'TIERED_PAID_AMOUNT';
    this.form.patchValue({
      payoutBaseAmount: this.numberParam(params, 'payoutBaseAmount', 50),
      quantityMode,
      quantity: this.numberParam(params, 'quantity', 1),
      stepPaidAmount: this.numberParam(params, 'stepPaidAmount', 1000),
      quantityPerStep: this.numberParam(params, 'quantityPerStep', 1),
      maxQuantity: this.numberParam(params, 'maxQuantity', 3),
      choiceMode: this.stringParam(params, 'choiceMode', 'AUTO_GENERATE') as 'AUTO_GENERATE' | 'SELLER_SELECTS',
      regenerableBeforeConfirm: this.booleanParam(params, 'regenerableBeforeConfirm', true),
      maxRegenerationsBeforeConfirm: this.numberParam(params, 'maxRegenerationsBeforeConfirm', 3),
    }, { emitEvent: false });
    this.replaceQuantityTiers(this.quantityTiersParam(params));
    this.syncQuantityModeControls(quantityMode);
  }

  private replaceQuantityTiers(tiers: readonly MaryajQuantityTier[]): void {
    const formArray = this.quantityTiers();
    while (formArray.length > 0) {
      formArray.removeAt(0);
    }
    for (const tier of tiers.length ? tiers : [
      { minPaidAmount: 100, maxPaidAmount: 199, quantity: 1 },
      { minPaidAmount: 200, maxPaidAmount: 499, quantity: 2 },
      { minPaidAmount: 500, maxPaidAmount: null, quantity: 3 },
    ]) {
      formArray.push(this.quantityTierGroup(tier.minPaidAmount, tier.maxPaidAmount, tier.quantity));
    }
  }

  private findMaryajRule(campaign: PromotionCampaignView | null): PromotionRuleView | null {
    return campaign?.rules.find(rule =>
      rule.ruleKey === 'maryaj-gratis-default'
      || rule.effects.some(effect => this.isMaryajFreeGameEffect(effect))
    ) ?? null;
  }

  private findMaryajEffect(campaign: PromotionCampaignView | null): PromotionConfigItem | null {
    const effects = campaign?.rules.flatMap(rule => rule.effects) ?? [];
    return [...effects].reverse().find(effect => this.isMaryajFreeGameEffect(effect))
      ?? [...effects].reverse().find(effect => effect.type === 'FREE_GAME_LINE')
      ?? null;
  }

  private isMaryajFreeGameEffect(effect: PromotionConfigItem): boolean {
    return effect.type === 'FREE_GAME_LINE' && effect.params?.['gameCode'] === 'HT_MARYAJ_GRATUIT';
  }

  private replaceCampaign(updated: PromotionCampaignView): void {
    const updatedId = promotionIdValue(updated.id);
    this.campaigns.update(items =>
      items.map(item => (promotionIdValue(item.id) === updatedId ? updated : item)),
    );
  }

  private syncQuantityModeControls(mode: 'FIXED' | 'PER_PAID_AMOUNT' | 'TIERED_PAID_AMOUNT'): void {
    const stepPaidAmount = this.form.controls.stepPaidAmount;
    const quantityPerStep = this.form.controls.quantityPerStep;
    const maxQuantity = this.form.controls.maxQuantity;
    const quantityTiers = this.quantityTiers();

    if (mode === 'FIXED') {
      stepPaidAmount.setValue(0, { emitEvent: false });
      quantityPerStep.setValue(0, { emitEvent: false });
      maxQuantity.setValue(0, { emitEvent: false });
      stepPaidAmount.disable({ emitEvent: false });
      quantityPerStep.disable({ emitEvent: false });
      maxQuantity.disable({ emitEvent: false });
      quantityTiers.disable({ emitEvent: false });
      return;
    }

    if (mode === 'TIERED_PAID_AMOUNT') {
      stepPaidAmount.setValue(0, { emitEvent: false });
      quantityPerStep.setValue(0, { emitEvent: false });
      maxQuantity.setValue(0, { emitEvent: false });
      stepPaidAmount.disable({ emitEvent: false });
      quantityPerStep.disable({ emitEvent: false });
      maxQuantity.disable({ emitEvent: false });
      quantityTiers.enable({ emitEvent: false });
      return;
    }

    stepPaidAmount.enable({ emitEvent: false });
    quantityPerStep.enable({ emitEvent: false });
    maxQuantity.enable({ emitEvent: false });
    quantityTiers.disable({ emitEvent: false });
    if (stepPaidAmount.value <= 0) stepPaidAmount.setValue(1000, { emitEvent: false });
    if (quantityPerStep.value <= 0) quantityPerStep.setValue(2, { emitEvent: false });
    if (maxQuantity.value <= 0) maxQuantity.setValue(10, { emitEvent: false });
  }

  private quantityTierGroup(minPaidAmount: number, maxPaidAmount: number | null, quantity: number) {
    return this.fb.group({
      minPaidAmount: [minPaidAmount, [Validators.required, Validators.min(1)]],
      maxPaidAmount: [maxPaidAmount, [Validators.min(1)]],
      quantity: [quantity, [Validators.required, Validators.min(1), Validators.max(50)]],
    });
  }

  private quantityTiersPayload(): readonly MaryajQuantityTier[] {
    return this.quantityTiers().getRawValue().map(value => ({
      minPaidAmount: Number(value.minPaidAmount),
      maxPaidAmount: value.maxPaidAmount == null || value.maxPaidAmount === ''
        ? null
        : Number(value.maxPaidAmount),
      quantity: Number(value.quantity),
    }));
  }

  private quantityTiersParam(params: Record<string, unknown>): readonly MaryajQuantityTier[] {
    const raw = params['quantityTiers'];
    if (!Array.isArray(raw)) return [];
    return raw.map(item => {
      const tier = item as Record<string, unknown>;
      return {
        minPaidAmount: this.numberParam(tier, 'minPaidAmount', 100),
        maxPaidAmount: tier['maxPaidAmount'] == null ? null : this.numberParam(tier, 'maxPaidAmount', 0),
        quantity: this.numberParam(tier, 'quantity', 1),
      };
    });
  }

  private numberParam(params: Record<string, unknown>, key: string, fallback: number): number {
    const value = params[key];
    if (typeof value === 'number') return value;
    if (typeof value === 'string' && value.trim() !== '') return Number(value);
    return fallback;
  }

  private stringParam(params: Record<string, unknown>, key: string, fallback: string): string {
    const value = params[key];
    return typeof value === 'string' && value.trim() !== '' ? value : fallback;
  }

  private booleanParam(params: Record<string, unknown>, key: string, fallback: boolean): boolean {
    const value = params[key];
    if (typeof value === 'boolean') return value;
    if (typeof value === 'string') return value === 'true';
    return fallback;
  }
}
