import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormField, form, submit as submitForm } from '@angular/forms/signals';
import { RouterLink } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import {
  AdminFormSection,
  AdminFormShell,
  BadgeStatus,
  TchSectionError,
  TchStatusBadge,
} from '@tch/ui/components';
import { AdminDialogShellComponent } from '@tch/ui/console';
import { tchMutation } from '@tch/web/async';
import { Observable, concatMap, forkJoin, map, of } from 'rxjs';

import { ConsoleBetLabelPipe, ConsoleGameNamePipe } from '@tch/web/console';
import { TenantGameOddGroupView } from '../../data-access/admin-games-pricing.models';
import {
  AdminGamesPricingApiService,
  DeleteTenantOddsRequest,
  UpsertTenantOddsRequest,
} from '../../data-access/admin-games-pricing-api.service';
import {
  GamesAdminApiService,
  TenantBetOptionConfigView,
  TenantBetTypeOptionConfigView,
  TenantGameBetOptionConfigView,
  TenantGameSelectionPolicy,
  TenantGameView,
  UpdateGameSettingsRequest,
  UpdateTenantGameBetOptionConfigRequest,
} from '../../data-access/games-admin-api.service';

interface GameSettingsFormModel {
  readonly displayName: string;
  readonly visibleInPos: boolean;
  readonly minStake: number | null;
  readonly maxStake: number | null;
  readonly displayOrder: number;
  readonly availabilityEnabled: boolean;
  readonly startLocalTime: string;
  readonly endLocalTime: string;
}

interface SaveGameConfigRequest {
  readonly settings: UpdateGameSettingsRequest;
  readonly betOptions: UpdateTenantGameBetOptionConfigRequest | null;
  readonly pricingOdds: readonly UpsertTenantOddsRequest[];
  readonly deletePricingOdds: readonly DeleteTenantOddsRequest[];
}

@Component({
  selector: 'tch-game-settings-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormField,
    AdminFormSection,
    AdminFormShell,
    AdminDialogShellComponent,
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    RouterLink,
    TchSectionError,
    TchStatusBadge,
    TranslatePipe,
    ConsoleBetLabelPipe,
    ConsoleGameNamePipe,
  ],
  templateUrl: './game-settings.dialog.html',
  styleUrls: ['./game-settings.dialog.scss'],
})
export class GameSettingsDialog {
  protected readonly data = inject<{ game: TenantGameView }>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<GameSettingsDialog>);
  private readonly api = inject(GamesAdminApiService);
  private readonly pricingApi = inject(AdminGamesPricingApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly translate = inject(TranslateService);

  readonly model = signal<GameSettingsFormModel>({
    displayName: this.data.game.displayName ?? '',
    visibleInPos: this.data.game.visibleInPos,
    minStake: this.data.game.minStake,
    maxStake: this.data.game.maxStake,
    displayOrder: this.data.game.displayOrder,
    availabilityEnabled: this.data.game.availabilityEnabled,
    startLocalTime: this.data.game.startLocalTime ?? '',
    endLocalTime: this.data.game.endLocalTime ?? '',
  });
  readonly form = form(this.model);
  readonly betOptionConfig = signal<TenantGameBetOptionConfigView | null>(null);
  readonly pricingGroups = signal<readonly TenantGameOddGroupView[]>(
    this.toPricingGroups(this.data.game.betOptionGroups ?? []),
  );
  readonly betOptionLoading = signal(true);
  readonly betOptionLoadFailed = signal(false);
  readonly showSalesOptions = computed(() => {
    const config = this.betOptionConfig();
    if (!config || this.isSimpleStakeGame()) return false;
    return config.betTypes.some(betType => betType.options.length > 1);
  });
  readonly selectionPolicies: readonly TenantGameSelectionPolicy[] = [
    'EXPLICIT_ONLY',
    'EXPLICIT_WITH_AUTO_OPTION',
    'IMPLICIT_BEST_MATCH',
  ];

  readonly saveSettings = tchMutation<SaveGameConfigRequest, void>({
    source: 'admin.games.settings',
    run: req => this.saveGameConfig(req),
    onSuccess: () => this.dialogRef.close(true),
  });
  readonly feedback = computed(() => this.saveSettings.feedback());
  readonly stakeErrorKey = computed(() =>
    this.stakeErrorKeyFor(this.model().minStake, this.model().maxStake),
  );

  constructor() {
    this.api
      .getBetOptionConfig(this.data.game.gameCode, { suppressShellFeedback: true })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: config => {
          this.betOptionConfig.set(config);
          this.betOptionLoading.set(false);
          this.betOptionLoadFailed.set(false);
        },
        error: () => {
          this.betOptionLoading.set(false);
          this.betOptionLoadFailed.set(true);
        },
      });
  }

  submit(event: Event): void {
    event.preventDefault();
    if (this.stakeErrorKey()) return;
    submitForm(this.form, async () => {
      this.saveSettings.execute({
        settings: this.toRequest(this.model()),
        betOptions: this.showSalesOptions()
          ? this.toBetOptionsRequest(this.betOptionConfig())
          : null,
        pricingOdds: this.toPricingOddsRequest(),
        deletePricingOdds: this.toPricingDeleteRequest(),
      });
    });
  }

  updateSelectionPolicy(betType: string, selectionPolicy: TenantGameSelectionPolicy): void {
    this.updateBetType(betType, current => ({
      ...current,
      selectionPolicy,
    }));
  }

  updateDefaultOption(betType: string, defaultOption: number | null): void {
    this.updateBetType(betType, current => ({
      ...current,
      defaultOption,
    }));
  }

  toggleOptionEnabled(betType: string, optionCode: number, enabled: boolean): void {
    this.updateBetOption(betType, optionCode, option => ({
      ...option,
      enabled,
      visibleInPos: enabled ? option.visibleInPos : false,
    }));
  }

  toggleOptionVisibleInPos(betType: string, optionCode: number, visibleInPos: boolean): void {
    this.updateBetOption(betType, optionCode, option => ({
      ...option,
      visibleInPos,
    }));
  }

  protected optionStatus(option: TenantBetOptionConfigView): BadgeStatus {
    if (!option.enabled) return 'blocked';
    return option.visibleInPos ? 'ready' : 'pending';
  }

  protected optionStatusLabelKey(option: TenantBetOptionConfigView): string {
    if (!option.enabled) return 'admin.games.settings.salesOptions.status.disabled';
    return option.visibleInPos
      ? 'admin.games.settings.salesOptions.status.visible'
      : 'admin.games.settings.salesOptions.status.hidden';
  }

  formattedStakeAmount(value: number | null): string {
    if (value === null) return this.translate.instant('common.not_available');
    return `${value.toLocaleString('fr')} HTG`;
  }

  stakeErrorKeyFor(minStake: number | null, maxStake: number | null): string | null {
    if (minStake !== null && (!Number.isFinite(minStake) || minStake <= 0)) {
      return 'admin.games.settings.stakes.error.minPositive';
    }
    if (maxStake !== null && (!Number.isFinite(maxStake) || maxStake <= 0)) {
      return 'admin.games.settings.stakes.error.maxPositive';
    }
    if (minStake !== null && maxStake !== null && minStake > maxStake) {
      return 'admin.games.settings.stakes.error.maxAfterMin';
    }
    return null;
  }

  updatePricingOdds(groupId: string, pricingVariantCode: string | null, rawValue: string): void {
    if (!pricingVariantCode) return;
    const normalized = rawValue.trim();
    if (normalized === '') {
      this.setPricingOdds(groupId, pricingVariantCode, null);
      return;
    }
    const odds = Number(rawValue);
    if (!Number.isFinite(odds) || odds <= 0) return;

    this.setPricingOdds(groupId, pricingVariantCode, odds);
  }

  updatePricingRuleType(
    groupId: string,
    pricingVariantCode: string | null,
    payoutRuleType: 'STAKE_MULTIPLIER' | 'FIXED_AMOUNT',
  ): void {
    if (!pricingVariantCode) return;
    this.pricingGroups.update(groups =>
      groups.map(group => {
        if (group.id !== groupId) return group;
        return {
          ...group,
          variants: group.variants.map(variant =>
            variant.pricingVariantCode === pricingVariantCode
              ? {
                  ...variant,
                  payoutRuleType,
                  odds: payoutRuleType === 'FIXED_AMOUNT' ? null : variant.odds,
                  fixedAmount: payoutRuleType === 'STAKE_MULTIPLIER' ? null : variant.fixedAmount,
                  value:
                    payoutRuleType === 'FIXED_AMOUNT'
                      ? variant.fixedAmount === null || variant.fixedAmount === undefined
                        ? this.notConfiguredLabel()
                        : `${variant.fixedAmount}`
                      : variant.odds === null || variant.odds === undefined
                        ? this.notConfiguredLabel()
                        : `×${variant.odds}`,
                }
              : variant,
          ),
        };
      }),
    );
  }

  updatePricingFixedAmount(
    groupId: string,
    pricingVariantCode: string | null,
    rawValue: string,
  ): void {
    if (!pricingVariantCode) return;
    const normalized = rawValue.trim();
    if (normalized === '') {
      this.setPricingFixedAmount(groupId, pricingVariantCode, null);
      return;
    }
    const fixedAmount = Number(rawValue);
    if (!Number.isFinite(fixedAmount) || fixedAmount < 0) return;

    this.setPricingFixedAmount(groupId, pricingVariantCode, fixedAmount);
  }

  clearPricingOdds(groupId: string, pricingVariantCode: string | null): void {
    if (!pricingVariantCode) return;
    this.pricingGroups.update(groups =>
      groups.map(group => {
        if (group.id !== groupId) return group;
        return {
          ...group,
          variants: group.variants.map(variant =>
            variant.pricingVariantCode === pricingVariantCode
              ? { ...variant, odds: null, fixedAmount: null, value: this.notConfiguredLabel() }
              : variant,
          ),
        };
      }),
    );
  }

  private setPricingOdds(groupId: string, pricingVariantCode: string, odds: number | null): void {
    this.pricingGroups.update(groups =>
      groups.map(group => {
        if (group.id !== groupId) return group;
        return {
          ...group,
          variants: group.variants.map(variant =>
            variant.pricingVariantCode === pricingVariantCode
              ? {
                  ...variant,
                  odds,
                  payoutRuleType: 'STAKE_MULTIPLIER',
                  fixedAmount: null,
                  value: odds === null ? this.notConfiguredLabel() : `×${odds}`,
                }
              : variant,
          ),
        };
      }),
    );
  }

  private setPricingFixedAmount(
    groupId: string,
    pricingVariantCode: string,
    fixedAmount: number | null,
  ): void {
    this.pricingGroups.update(groups =>
      groups.map(group => {
        if (group.id !== groupId) return group;
        return {
          ...group,
          variants: group.variants.map(variant =>
            variant.pricingVariantCode === pricingVariantCode
              ? {
                  ...variant,
                  odds: null,
                  payoutRuleType: 'FIXED_AMOUNT',
                  fixedAmount,
                  value: fixedAmount === null ? this.notConfiguredLabel() : `${fixedAmount}`,
                }
              : variant,
          ),
        };
      }),
    );
  }

  private toRequest(value: GameSettingsFormModel): UpdateGameSettingsRequest {
    return {
      displayName: value.displayName || null,
      visibleInPos: value.visibleInPos,
      minStake: value.minStake,
      maxStake: value.maxStake,
      displayOrder: value.displayOrder,
      availabilityEnabled: value.availabilityEnabled,
      startLocalTime: value.availabilityEnabled ? value.startLocalTime || null : null,
      endLocalTime: value.availabilityEnabled ? value.endLocalTime || null : null,
    };
  }

  private saveGameConfig(req: SaveGameConfigRequest): Observable<void> {
    const options = { suppressShellFeedback: true };
    let save$: Observable<unknown> = this.api.updateGameSettings(
      this.data.game.gameCode,
      req.settings,
      options,
    );
    const betOptions = req.betOptions;
    if (betOptions && !this.betOptionLoadFailed()) {
      save$ = save$.pipe(
        concatMap(() =>
          this.api.updateBetOptionConfig(this.data.game.gameCode, betOptions, options),
        ),
      );
    }

    if (req.pricingOdds.length > 0) {
      save$ = save$.pipe(
        concatMap(() =>
          forkJoin(
            req.pricingOdds.map(item => this.pricingApi.upsertTenantOdds(item, options)),
          ).pipe(map(() => undefined)),
        ),
      );
    }

    if (req.deletePricingOdds.length > 0) {
      save$ = save$.pipe(
        concatMap(() =>
          forkJoin(
            req.deletePricingOdds.map(item => this.pricingApi.deleteTenantOdds(item, options)),
          ).pipe(map(() => undefined)),
        ),
      );
    }

    return save$.pipe(concatMap(() => of(undefined)));
  }

  private updateBetType(
    betType: string,
    updater: (value: TenantBetTypeOptionConfigView) => TenantBetTypeOptionConfigView,
  ): void {
    this.betOptionConfig.update(config => {
      if (!config) return config;
      return {
        ...config,
        betTypes: config.betTypes.map(item => (item.betType === betType ? updater(item) : item)),
      };
    });
  }

  private updateBetOption(
    betType: string,
    optionCode: number,
    updater: (value: TenantBetOptionConfigView) => TenantBetOptionConfigView,
  ): void {
    this.updateBetType(betType, current => ({
      ...current,
      options: current.options.map(option =>
        option.code === optionCode ? updater(option) : option,
      ),
    }));
  }

  private toBetOptionsRequest(
    config: TenantGameBetOptionConfigView | null,
  ): UpdateTenantGameBetOptionConfigRequest | null {
    if (!config) return null;

    return {
      betTypes: config.betTypes.map(betType => ({
        betType: betType.betType,
        selectionPolicy: betType.selectionPolicy,
        defaultOption: betType.defaultOption,
        options: betType.options.map(option => ({
          code: option.code,
          enabled: option.enabled,
          visibleInPos: option.visibleInPos,
          displayOrder: option.displayOrder,
        })),
      })),
    };
  }

  private toPricingOddsRequest(): readonly UpsertTenantOddsRequest[] {
    return this.pricingGroups().flatMap(group =>
      group.variants
        .filter(variant => variant.pricingVariantCode && this.isPricingConfigured(variant))
        .map(variant => ({
          gameCode: this.data.game.gameCode,
          pricingVariantCode: variant.pricingVariantCode ?? '',
          betType: variant.betType,
          betOption: variant.betOption,
          odds: variant.payoutRuleType === 'FIXED_AMOUNT' ? null : variant.odds,
          payoutRuleType: variant.payoutRuleType,
          fixedAmount: variant.payoutRuleType === 'FIXED_AMOUNT' ? variant.fixedAmount : null,
        })),
    );
  }

  private toPricingDeleteRequest(): readonly DeleteTenantOddsRequest[] {
    return this.pricingGroups().flatMap(group =>
      group.variants
        .filter(variant => variant.pricingVariantCode && !this.isPricingConfigured(variant))
        .map(variant => ({
          gameCode: this.data.game.gameCode,
          pricingVariantCode: variant.pricingVariantCode ?? '',
        })),
    );
  }

  private isPricingConfigured(variant: TenantGameOddGroupView['variants'][number]): boolean {
    if (variant.payoutRuleType === 'FIXED_AMOUNT') {
      return variant.fixedAmount !== null && variant.fixedAmount !== undefined;
    }
    return variant.odds !== null && variant.odds !== undefined;
  }

  private toPricingGroups(
    groups: NonNullable<TenantGameView['betOptionGroups']>,
  ): readonly TenantGameOddGroupView[] {
    return groups.map(group => ({
      ...group,
      variants: group.variants.map(variant => ({
        ...variant,
        payoutRuleType: variant.payoutRuleType ?? 'STAKE_MULTIPLIER',
        fixedAmount: variant.fixedAmount ?? null,
      })),
    }));
  }

  private isSimpleStakeGame(): boolean {
    const code = this.data.game.gameCode.toUpperCase();
    return code.includes('BOLET') || code.includes('BORLETTE');
  }

  protected notConfiguredLabel(): string {
    return this.translate.instant('admin.gamesPricing.card.notConfigured');
  }
}
