import { DestroyRef, Injectable, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormArray, FormBuilder, Validators } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { catchError, forkJoin, map, of, switchMap } from 'rxjs';

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
type MaryajChoiceMode = 'AUTO_GENERATE' | 'SELLER_SELECTS';

const MARYAJ_GRATIS_GAME_CODE = 'HT_MARYAJ_GRATIS';
const MARYAJ_GRATIS_GAME_CODES = new Set([MARYAJ_GRATIS_GAME_CODE, 'HT_MARYAJ_GRATUIT']);

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
    if (!game) return 'Le jeu Maryaj gratis est absent de la configuration tenant.';
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
    startsAt: [new Date(), Validators.required],
    endsAt: [this.addYears(new Date(), 10), Validators.required],
    priority: [100, [Validators.required, Validators.min(0), Validators.max(100000)]],
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
    choiceMode: ['AUTO_GENERATE' as MaryajChoiceMode, Validators.required],
    regenerableBeforeConfirm: [true],
    maxRegenerationsBeforeConfirm: [3, [Validators.required, Validators.min(0), Validators.max(20)]],
  });

  constructor() {
    this.syncQuantityModeControls(this.form.controls.quantityMode.value);
    this.form.controls.quantityMode.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(mode => this.syncQuantityModeControls(mode));
    this.syncChoiceModeControls(this.form.controls.choiceMode.value);
    this.form.controls.choiceMode.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(mode => this.syncChoiceModeControls(mode));
  }

  load(): void {
    this.state.set('loading');
    this.error.set(null);
    forkJoin({
      campaigns: this.api.listCampaigns(),
      games: this.gamesPricingApi.getGamesPricing(),
    }).pipe(
      switchMap(({ campaigns, games }) => {
        const maryajCampaign = this.findMaryajCampaign(campaigns.items);
        const campaignId = promotionIdValue(maryajCampaign?.id);
        if (!campaignId) {
          return of({
            campaigns: campaigns.items,
            games,
            maryajCampaign,
          });
        }
        return this.api.getCampaign(campaignId).pipe(
          map(detailedCampaign => ({
            campaigns: [
              detailedCampaign,
              ...campaigns.items.filter(item => promotionIdValue(item.id) !== campaignId),
            ],
            games,
            maryajCampaign: detailedCampaign,
          })),
          catchError(() => of({
            campaigns: campaigns.items,
            games,
            maryajCampaign,
          })),
        );
      }),
    ).subscribe({
      next: ({ campaigns, games, maryajCampaign }) => {
        this.campaigns.set(campaigns);
        this.maryajGame.set(games.find(g => MARYAJ_GRATIS_GAME_CODES.has(g.gameCode)) ?? null);
        const effect = this.findMaryajEffect(maryajCampaign);
        if (maryajCampaign) {
          this.patchFormFromCampaign(maryajCampaign);
        }
        if (effect) {
          this.patchFormFromEffect(effect);
        }
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
    const tiersError = this.quantityTiersValidationError();
    if (tiersError) {
      this.snackBar.open(tiersError, 'OK', { duration: 5000 });
      return;
    }
    const campaignError = this.campaignMetadataValidationError();
    if (campaignError) {
      this.snackBar.open(campaignError, 'OK', { duration: 5000 });
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
    }).pipe(
      switchMap(campaign => {
        const campaignId = promotionIdValue(campaign.id);
        return campaignId
          ? this.api.updateCampaign(campaignId, this.campaignMetadataPayload(campaign))
          : of(campaign);
      }),
    ).subscribe({
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
    const campaign = this.maryajCampaign();
    if (campaign) {
      this.patchFormFromCampaign(campaign);
    }
    const effect = this.maryajEffect();
    if (effect) {
      this.patchFormFromEffect(effect);
    }
    this.editingOffer.set(true);
  }

  cancelEditingOffer(): void {
    this.editingOffer.set(false);
    const campaign = this.maryajCampaign();
    if (campaign) {
      this.patchFormFromCampaign(campaign);
    }
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
    const tiersError = this.quantityTiersValidationError();
    if (tiersError) {
      this.snackBar.open(tiersError, 'OK', { duration: 5000 });
      return;
    }
    const campaignError = this.campaignMetadataValidationError();
    if (campaignError) {
      this.snackBar.open(campaignError, 'OK', { duration: 5000 });
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
    this.api.updateCampaign(campaignId, this.campaignMetadataPayload(campaign)).pipe(
      switchMap(() => this.api.updateRuleEffects(campaignId, ruleId, {
        items: [this.freeGameLineEffectItem()],
      })),
    ).subscribe({
      next: updated => {
        this.replaceCampaign(updated);
        this.patchFormFromCampaign(updated);
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

  setSellerSelectionEnabled(enabled: boolean): void {
    this.form.controls.choiceMode.setValue(enabled ? 'SELLER_SELECTS' : 'AUTO_GENERATE');
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
      gameCode: MARYAJ_GRATIS_GAME_CODE,
      payoutBaseAmount: value.payoutBaseAmount,
      quantityMode: value.quantityMode,
      quantity: value.quantity,
      choiceMode: value.choiceMode,
      regenerableBeforeConfirm: value.choiceMode === 'AUTO_GENERATE'
        ? value.regenerableBeforeConfirm
        : false,
      maxRegenerationsBeforeConfirm: value.choiceMode === 'AUTO_GENERATE'
        ? value.maxRegenerationsBeforeConfirm
        : 0,
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

  private campaignMetadataPayload(campaign: PromotionCampaignView): {
    readonly name: string;
    readonly description: string | null;
    readonly startsAt: string;
    readonly endsAt: string;
    readonly priority: number;
  } {
    const value = this.form.getRawValue();
    return {
      name: campaign.name || 'Maryaj gratis',
      description: null,
      startsAt: this.instantFromDate(value.startsAt, 'start') ?? new Date().toISOString(),
      endsAt: this.instantFromDate(value.endsAt, 'end') ?? this.addYears(new Date(), 10).toISOString(),
      priority: Number(value.priority),
    };
  }

  private patchFormFromCampaign(campaign: PromotionCampaignView): void {
    const now = new Date();
    this.form.patchValue({
      startsAt: campaign.startsAt
        ? this.dateFromInstant(campaign.startsAt)
        : now,
      endsAt: campaign.endsAt
        ? this.dateFromInstant(campaign.endsAt)
        : this.addYears(now, 10),
      priority: campaign.priority,
    }, { emitEvent: false });
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
      choiceMode: this.stringParam(params, 'choiceMode', 'AUTO_GENERATE') as MaryajChoiceMode,
      regenerableBeforeConfirm: this.booleanParam(params, 'regenerableBeforeConfirm', true),
      maxRegenerationsBeforeConfirm: this.numberParam(params, 'maxRegenerationsBeforeConfirm', 3),
    }, { emitEvent: false });
    this.replaceQuantityTiers(this.quantityTiersParam(params));
    this.syncQuantityModeControls(quantityMode);
    this.syncChoiceModeControls(this.form.controls.choiceMode.value);
  }

  private findMaryajCampaign(campaigns: readonly PromotionCampaignView[]): PromotionCampaignView | null {
    return campaigns.find(c => c.code === 'DEFAULT_MARYAJ_GRATIS' || c.code.includes('MARYAJ')) ?? null;
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
    const gameCode = String(effect.params?.['gameCode'] ?? '');
    return effect.type === 'FREE_GAME_LINE' && MARYAJ_GRATIS_GAME_CODES.has(gameCode);
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

  private syncChoiceModeControls(mode: MaryajChoiceMode): void {
    const regenerable = this.form.controls.regenerableBeforeConfirm;
    const maxRegenerations = this.form.controls.maxRegenerationsBeforeConfirm;

    if (mode === 'SELLER_SELECTS') {
      regenerable.setValue(false, { emitEvent: false });
      maxRegenerations.setValue(0, { emitEvent: false });
      regenerable.disable({ emitEvent: false });
      maxRegenerations.disable({ emitEvent: false });
      return;
    }

    regenerable.enable({ emitEvent: false });
    maxRegenerations.enable({ emitEvent: false });
    if (maxRegenerations.value <= 0) {
      regenerable.setValue(true, { emitEvent: false });
      maxRegenerations.setValue(3, { emitEvent: false });
    }
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

  private quantityTiersValidationError(): string | null {
    if (this.form.controls.quantityMode.value !== 'TIERED_PAID_AMOUNT') return null;
    const tiers = this.quantityTiersPayload();
    if (!tiers.length) return 'Ajoutez au moins un palier Maryaj gratis.';

    let previousMax: number | null = null;
    for (const [index, tier] of tiers.entries()) {
      const position = index + 1;
      if (!Number.isFinite(tier.minPaidAmount) || tier.minPaidAmount <= 0) {
        return `Le montant de départ du palier ${position} est invalide.`;
      }
      if (!Number.isFinite(tier.quantity) || tier.quantity <= 0) {
        return `La quantité du palier ${position} est invalide.`;
      }
      if (tier.maxPaidAmount != null) {
        if (!Number.isFinite(tier.maxPaidAmount) || tier.maxPaidAmount <= 0) {
          return `Le montant final du palier ${position} est invalide.`;
        }
        if (tier.maxPaidAmount < tier.minPaidAmount) {
          return `Le montant final du palier ${position} doit être supérieur au montant de départ.`;
        }
      }
      if (previousMax == null && index > 0) {
        return 'Le palier sans montant final doit être le dernier.';
      }
      if (previousMax != null && tier.minPaidAmount <= previousMax) {
        return `Le palier ${position} chevauche le palier précédent.`;
      }
      previousMax = tier.maxPaidAmount;
    }

    return null;
  }

  private campaignMetadataValidationError(): string | null {
    const value = this.form.getRawValue();
    const startsAt = this.instantFromDate(value.startsAt, 'start');
    const endsAt = this.instantFromDate(value.endsAt, 'end');
    if (!startsAt || !endsAt) return 'Renseignez une date de début et une date de fin valides.';
    if (new Date(startsAt).getTime() >= new Date(endsAt).getTime()) {
      return 'La date de fin doit être après la date de début.';
    }
    return null;
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

  private dateFromInstant(value: string): Date {
    const date = new Date(value);
    return Number.isFinite(date.getTime()) ? date : new Date();
  }

  private instantFromDate(value: Date | string | null | undefined, boundary: 'start' | 'end'): string | null {
    if (!value) return null;
    const date = new Date(value instanceof Date ? value.getTime() : new Date(value).getTime());
    if (boundary === 'start') {
      date.setHours(0, 0, 0, 0);
    } else {
      date.setHours(23, 59, 59, 999);
    }
    return Number.isFinite(date.getTime()) ? date.toISOString() : null;
  }

  private addYears(date: Date, years: number): Date {
    const copy = new Date(date);
    copy.setFullYear(copy.getFullYear() + years);
    return copy;
  }
}
