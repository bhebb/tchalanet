import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { form, submit as submitForm } from '@angular/forms/signals';
import {
  MAT_DIALOG_DATA,
  MatDialog,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import {
  TchSectionError,
  TchConfirmDialog,
  type TchConfirmDialogData,
} from '@tch/ui/components';
import { AdminDialogShellComponent } from '@tch/ui/console';
import { tchMutation } from '@tch/web/async';
import { Observable, concatMap, filter, forkJoin, map, of } from 'rxjs';

import { ConsoleGameNamePipe } from '@tch/web/console';
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
import {
  GameSettingsEditorComponent,
  GameSettingsFormModel,
} from '../game-settings-editor/game-settings-editor.component';

interface SaveGameConfigRequest {
  readonly settings: UpdateGameSettingsRequest;
  readonly betOptions: UpdateTenantGameBetOptionConfigRequest | null;
  readonly pricingOdds: readonly UpsertTenantOddsRequest[];
  readonly deletePricingOdds: readonly DeleteTenantOddsRequest[];
}

export const GAME_SETTINGS_DIALOG_SURFACE_CONFIG = {
  width: 'min(48rem, 100vw)',
  maxWidth: '100vw',
  height: 'min(54rem, 100dvh)',
  maxHeight: '100dvh',
} as const;

@Component({
  selector: 'tch-game-settings-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminDialogShellComponent,
    GameSettingsEditorComponent,
    MatButtonModule,
    MatDialogModule,
    TchSectionError,
    TranslatePipe,
    ConsoleGameNamePipe,
  ],
  templateUrl: './game-settings.dialog.html',
  styleUrls: ['./game-settings.dialog.scss'],
})
export class GameSettingsDialog {
  protected readonly data = inject<{ game: TenantGameView }>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<GameSettingsDialog>);
  private readonly dialog = inject(MatDialog);
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
  readonly editingPricingVariantKey = signal<string | null>(null);
  readonly initialBetOptionSnapshot = signal(this.snapshot(null));
  private readonly initialModelSnapshot = this.snapshot(this.model());
  private readonly initialPricingSnapshot = this.snapshot(this.pricingGroups());
  readonly dirty = computed(
    () =>
      this.snapshot(this.model()) !== this.initialModelSnapshot ||
      this.snapshot(this.pricingGroups()) !== this.initialPricingSnapshot ||
      this.snapshot(this.betOptionConfig()) !== this.initialBetOptionSnapshot(),
  );
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
    this.dialogRef.disableClose = true;
    this.dialogRef
      .backdropClick()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.requestCancel());
    this.dialogRef
      .keydownEvents()
      .pipe(
        filter(event => event.key === 'Escape'),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(event => {
        event.preventDefault();
        this.requestCancel();
      });

    this.api
      .getBetOptionConfig(this.data.game.gameCode, { suppressShellFeedback: true })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: config => {
          this.betOptionConfig.set(config);
          this.initialBetOptionSnapshot.set(this.snapshot(config));
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

  requestCancel(): void {
    if (this.saveSettings.pending()) return;
    if (!this.dirty()) {
      this.dialogRef.close(undefined);
      return;
    }

    this.dialog
      .open<TchConfirmDialog, TchConfirmDialogData, { confirmed: boolean }>(TchConfirmDialog, {
        data: {
          title: this.translate.instant('admin.games.settings.confirmDiscard.title'),
          message: this.translate.instant('admin.games.settings.confirmDiscard.message'),
          confirmLabel: this.translate.instant('admin.games.settings.confirmDiscard.action'),
          cancelLabel: this.translate.instant('common.cancel'),
          icon: 'undo',
        },
      })
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(result => {
        if (result?.confirmed) this.dialogRef.close(undefined);
      });
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

  protected optionDescription(
    betType: string,
    option: TenantBetOptionConfigView,
  ): string | null {
    const key = this.optionDescriptionKey(betType, option.code);
    const translated = this.translate.instant(key);
    if (translated && translated !== key) return translated;
    return option.description;
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

  editPricingVariant(groupId: string, pricingVariantCode: string | null, label: string): void {
    if (!pricingVariantCode) return;
    this.editingPricingVariantKey.set(this.pricingVariantKey(groupId, pricingVariantCode, label));
  }

  isPricingVariantEditing(
    groupId: string,
    pricingVariantCode: string | null,
    label: string,
  ): boolean {
    if (!pricingVariantCode) return false;
    return (
      this.editingPricingVariantKey() ===
      this.pricingVariantKey(groupId, pricingVariantCode, label)
    );
  }

  pricingRuleSummary(variant: TenantGameOddGroupView['variants'][number]): string {
    if (!this.isPricingConfigured(variant)) {
      return this.translate.instant('admin.games.settings.payouts.notConfigured');
    }
    if (variant.payoutRuleType === 'FIXED_AMOUNT') {
      return this.translate.instant('admin.games.settings.payouts.fixedAmountSummary', {
        amount: this.formattedStakeAmount(variant.fixedAmount ?? null),
      });
    }
    return this.translate.instant('admin.games.settings.payouts.multiplierSummary', {
      odds: variant.odds,
    });
  }

  requestClearPricingOdds(
    groupId: string,
    pricingVariantCode: string | null,
    label: string,
  ): void {
    if (!pricingVariantCode) return;
    this.dialog
      .open<TchConfirmDialog, TchConfirmDialogData, { confirmed: boolean }>(TchConfirmDialog, {
        data: {
          title: this.translate.instant('admin.games.settings.payouts.confirmClearTitle', {
            label,
          }),
          message: this.translate.instant('admin.games.settings.payouts.confirmClearMessage'),
          confirmLabel: this.translate.instant('admin.games.settings.payouts.confirmClearAction'),
          cancelLabel: this.translate.instant('common.cancel'),
          destructive: true,
          icon: 'delete',
        },
      })
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(result => {
        if (result?.confirmed) {
          this.clearPricingOdds(groupId, pricingVariantCode);
          this.editingPricingVariantKey.set(null);
        }
      });
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

  private pricingVariantKey(groupId: string, pricingVariantCode: string, label: string): string {
    return `${groupId}:${pricingVariantCode}:${label}`;
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

  private optionDescriptionKey(betType: string, optionCode: number): string {
    const normalizedBetType = betType
      .trim()
      .toUpperCase()
      .replace(/\s+/g, '_')
      .replace(/\*/g, 'STAR')
      .replace(/-/g, '_');
    return `admin.games.settings.salesOptions.descriptionByOption.${normalizedBetType}.${optionCode}`;
  }

  private snapshot(value: unknown): string {
    return JSON.stringify(value);
  }

  protected notConfiguredLabel(): string {
    return this.translate.instant('admin.gamesPricing.card.notConfigured');
  }
}
