import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  signal,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { form, submit as submitForm } from '@angular/forms/signals';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { TchConfirmDialog, TchSectionError, type TchConfirmDialogData } from '@tch/ui/components';
import { AdminPageShellComponent } from '@tch/ui/console';
import {
  TchAsyncReadyDirective,
  TchAsyncSource,
  TchAsyncViewComponent,
  resourceErrorVm,
  tchMutation,
} from '@tch/web/async';
import { Observable, concatMap, forkJoin, map, of } from 'rxjs';

import {
  DeleteTenantOddsRequest,
  UpsertTenantOddsRequest,
  AdminGamesPricingApiService,
} from '../../data-access/admin-games-pricing-api.service';
import {
  TenantGameOddGroupView,
  TenantGamePricingView,
} from '../../data-access/admin-games-pricing.models';
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
  GameBetOptionToggle,
  GameDefaultOptionChange,
  GamePricingRuleTypeChange,
  GamePricingValueChange,
  GamePricingVariantRef,
  GameSelectionPolicyChange,
  GameSettingsEditorComponent,
  GameSettingsFormModel,
} from '../../components/game-settings-editor/game-settings-editor.component';

interface SaveGameConfigRequest {
  readonly settings: UpdateGameSettingsRequest;
  readonly betOptions: UpdateTenantGameBetOptionConfigRequest | null;
  readonly pricingOdds: readonly UpsertTenantOddsRequest[];
  readonly deletePricingOdds: readonly DeleteTenantOddsRequest[];
}

const EMPTY_MODEL: GameSettingsFormModel = {
  displayName: '',
  visibleInPos: true,
  minStake: null,
  maxStake: null,
  displayOrder: 0,
  availabilityEnabled: false,
  startLocalTime: '',
  endLocalTime: '',
};

@Component({
  selector: 'tch-admin-game-settings-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminPageShellComponent,
    GameSettingsEditorComponent,
    MatButtonModule,
    MatIconModule,
    TchAsyncReadyDirective,
    TchAsyncViewComponent,
    TchSectionError,
    TranslatePipe,
  ],
  templateUrl: './admin-game-settings.page.html',
  styleUrls: ['./admin-game-settings.page.scss'],
})
export class AdminGameSettingsPage {
  private readonly api = inject(GamesAdminApiService);
  private readonly pricingApi = inject(AdminGamesPricingApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly translate = inject(TranslateService);
  private readonly paramMap = toSignal(this.route.paramMap, {
    initialValue: this.route.snapshot.paramMap,
  });
  private readonly queryParamMap = toSignal(this.route.queryParamMap, {
    initialValue: this.route.snapshot.queryParamMap,
  });

  readonly gameCode = computed(() => this.paramMap().get('gameCode'));
  readonly returnTo = computed(() => this.queryParamMap().get('returnTo'));
  readonly fromSetup = computed(() => this.queryParamMap().get('from') === 'setup');
  readonly gamesResource = this.api.listEnabledGamesResource({ suppressShellFeedback: true });
  readonly pricingGamesResource = this.pricingApi.getGamesPricingResource({
    suppressShellFeedback: true,
  });
  readonly gamesAsync: TchAsyncSource<readonly TenantGameView[]> = {
    status: () => this.gamesResource.status(),
    value: () => this.gamesResource.value(),
    error: () => this.gamesResource.error(),
  };
  readonly gamesError = resourceErrorVm(this.gamesAsync, 'admin.games.settings.page');
  readonly game = computed(() => {
    const code = this.gameCode();
    if (!code) return null;
    const game = this.gamesResource.value()?.find(candidate => candidate.gameCode === code) ?? null;
    if (!game) return null;
    return this.withPricingDetails(game, this.pricingGame());
  });
  readonly pricingGame = computed(() => {
    const code = this.gameCode();
    if (!code) return null;
    return this.pricingGamesResource.value()?.find(candidate => candidate.gameCode === code) ?? null;
  });
  readonly betOptionResource = this.api.getBetOptionConfigResource(this.gameCode, {
    suppressShellFeedback: true,
  });
  readonly betOptionConfig = computed(() => this.betOptionResource.value() ?? null);
  readonly betOptionLoading = computed(() => {
    const status = this.betOptionResource.status();
    return status === 'loading' || status === 'reloading';
  });
  readonly betOptionLoadFailed = computed(() => this.betOptionResource.status() === 'error');
  readonly model = signal<GameSettingsFormModel>(EMPTY_MODEL);
  readonly form = form(this.model);
  readonly pricingGroups = signal<readonly TenantGameOddGroupView[]>([]);
  readonly editingPricingVariantKey = signal<string | null>(null);
  readonly initializedGameCode = signal<string | null>(null);
  readonly initialBetOptionSnapshot = signal(this.snapshot(null));
  readonly initialModelSnapshot = signal(this.snapshot(EMPTY_MODEL));
  readonly initialPricingSnapshot = signal(this.snapshot([]));
  readonly dirty = computed(
    () =>
      this.snapshot(this.model()) !== this.initialModelSnapshot() ||
      this.snapshot(this.pricingGroups()) !== this.initialPricingSnapshot() ||
      this.snapshot(this.betOptionConfig()) !== this.initialBetOptionSnapshot(),
  );
  readonly showSalesOptions = computed(() => {
    const config = this.betOptionConfig();
    const game = this.game();
    if (!config || !game || this.isSimpleStakeGame(game)) return false;
    return config.betTypes.some(betType => betType.options.length > 1);
  });
  readonly selectionPolicies: readonly TenantGameSelectionPolicy[] = [
    'EXPLICIT_ONLY',
    'EXPLICIT_WITH_AUTO_OPTION',
    'IMPLICIT_BEST_MATCH',
  ];
  readonly stakeErrorKey = computed(() =>
    this.stakeErrorKeyFor(this.model().minStake, this.model().maxStake),
  );
  readonly pageTitle = computed(() => {
    const game = this.game();
    return game?.displayName ?? game?.catalogName ?? this.gameCode() ?? '';
  });
  readonly saveSettings = tchMutation<SaveGameConfigRequest, void>({
    source: 'admin.games.settings.page.save',
    run: req => this.saveGameConfig(req),
    onSuccess: () => this.navigateBack(),
  });
  readonly feedback = computed(() => this.saveSettings.feedback());

  constructor() {
    effect(() => {
      const game = this.game();
      if (!game || this.initializedGameCode() === game.gameCode) return;
      const model = this.toModel(game);
      const pricingGroups = this.toPricingGroups(game.betOptionGroups ?? []);
      this.model.set(model);
      this.pricingGroups.set(pricingGroups);
      this.initialModelSnapshot.set(this.snapshot(model));
      this.initialPricingSnapshot.set(this.snapshot(pricingGroups));
      this.initializedGameCode.set(game.gameCode);
    });

    effect(() => {
      const game = this.game();
      if (!game) return;
      const pricingGroups = this.toPricingGroups(game.betOptionGroups ?? []);
      const sourceSnapshot = this.snapshot(pricingGroups);
      if (sourceSnapshot === this.initialPricingSnapshot()) return;
      if (this.snapshot(this.pricingGroups()) !== this.initialPricingSnapshot()) return;
      this.pricingGroups.set(pricingGroups);
      this.initialPricingSnapshot.set(sourceSnapshot);
    });

    effect(() => {
      const config = this.betOptionConfig();
      if (!config) return;
      this.initialBetOptionSnapshot.set(this.snapshot(config));
    });
  }

  submit(event: Event): void {
    event.preventDefault();
    const game = this.game();
    if (!game || this.stakeErrorKey()) return;
    submitForm(this.form, async () => {
      this.saveSettings.execute({
        settings: this.toRequest(this.model()),
        betOptions: this.showSalesOptions()
          ? this.toBetOptionsRequest(this.betOptionConfig())
          : null,
        pricingOdds: this.toPricingOddsRequest(game.gameCode),
        deletePricingOdds: this.toPricingDeleteRequest(game.gameCode),
      });
    });
  }

  requestCancel(): void {
    if (this.saveSettings.pending()) return;
    if (!this.dirty()) {
      this.navigateBack();
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
      .subscribe(result => {
        if (result?.confirmed) this.navigateBack();
      });
  }

  reload(): void {
    this.gamesResource.reload();
    this.pricingGamesResource.reload();
    this.betOptionResource.reload();
  }

  updateSelectionPolicy(change: GameSelectionPolicyChange): void {
    this.updateBetType(change.betType, current => ({
      ...current,
      selectionPolicy: change.selectionPolicy,
    }));
  }

  updateDefaultOption(change: GameDefaultOptionChange): void {
    this.updateBetType(change.betType, current => ({
      ...current,
      defaultOption: change.defaultOption,
    }));
  }

  toggleOptionEnabled(change: GameBetOptionToggle): void {
    this.updateBetOption(change.betType, change.optionCode, option => ({
      ...option,
      enabled: change.checked,
      visibleInPos: change.checked ? option.visibleInPos : false,
    }));
  }

  toggleOptionVisibleInPos(change: GameBetOptionToggle): void {
    this.updateBetOption(change.betType, change.optionCode, option => ({
      ...option,
      visibleInPos: change.checked,
    }));
  }

  editPricingVariant(ref: GamePricingVariantRef): void {
    if (!ref.pricingVariantCode) return;
    this.editingPricingVariantKey.set(
      this.pricingVariantKey(ref.groupId, ref.pricingVariantCode, ref.label),
    );
  }

  updatePricingRuleType(change: GamePricingRuleTypeChange): void {
    if (!change.pricingVariantCode) return;
    this.pricingGroups.update(groups =>
      groups.map(group => {
        if (group.id !== change.groupId) return group;
        return {
          ...group,
          variants: group.variants.map(variant =>
            variant.pricingVariantCode === change.pricingVariantCode
              ? {
                  ...variant,
                  payoutRuleType: change.payoutRuleType,
                  odds: change.payoutRuleType === 'FIXED_AMOUNT' ? null : variant.odds,
                  fixedAmount:
                    change.payoutRuleType === 'STAKE_MULTIPLIER' ? null : variant.fixedAmount,
                }
              : variant,
          ),
        };
      }),
    );
  }

  updatePricingOdds(change: GamePricingValueChange): void {
    if (!change.pricingVariantCode) return;
    const normalized = change.rawValue.trim();
    if (normalized === '') {
      this.setPricingOdds(change.groupId, change.pricingVariantCode, null);
      return;
    }
    const odds = Number(change.rawValue);
    if (!Number.isFinite(odds) || odds <= 0) return;
    this.setPricingOdds(change.groupId, change.pricingVariantCode, odds);
  }

  updatePricingFixedAmount(change: GamePricingValueChange): void {
    if (!change.pricingVariantCode) return;
    const normalized = change.rawValue.trim();
    if (normalized === '') {
      this.setPricingFixedAmount(change.groupId, change.pricingVariantCode, null);
      return;
    }
    const fixedAmount = Number(change.rawValue);
    if (!Number.isFinite(fixedAmount) || fixedAmount < 0) return;
    this.setPricingFixedAmount(change.groupId, change.pricingVariantCode, fixedAmount);
  }

  requestClearPricingOdds(ref: GamePricingVariantRef): void {
    if (!ref.pricingVariantCode) return;
    this.dialog
      .open<TchConfirmDialog, TchConfirmDialogData, { confirmed: boolean }>(TchConfirmDialog, {
        data: {
          title: this.translate.instant('admin.games.settings.payouts.confirmClearTitle', {
            label: ref.label,
          }),
          message: this.translate.instant('admin.games.settings.payouts.confirmClearMessage'),
          confirmLabel: this.translate.instant('admin.games.settings.payouts.confirmClearAction'),
          cancelLabel: this.translate.instant('common.cancel'),
          destructive: true,
          icon: 'delete',
        },
      })
      .afterClosed()
      .subscribe(result => {
        if (!result?.confirmed || !ref.pricingVariantCode) return;
        this.clearPricingOdds(ref.groupId, ref.pricingVariantCode);
        this.editingPricingVariantKey.set(null);
      });
  }

  private navigateBack(): void {
    const commands =
      this.returnTo() === 'maryaj-gratis' ? ['/app/admin/maryaj-gratis'] : ['/app/admin/games'];
    void this.router.navigate(commands, {
      queryParams: this.fromSetup() ? { from: 'setup' } : undefined,
      fragment: this.returnTo() === 'maryaj-gratis' ? 'game' : undefined,
    });
  }

  private saveGameConfig(req: SaveGameConfigRequest): Observable<void> {
    const game = this.game();
    if (!game) return of(undefined);
    const options = { suppressShellFeedback: true };
    let save$: Observable<unknown> = this.api.updateGameSettings(
      game.gameCode,
      req.settings,
      options,
    );
    const betOptions = req.betOptions;
    if (betOptions && !this.betOptionLoadFailed()) {
      save$ = save$.pipe(
        concatMap(() => this.api.updateBetOptionConfig(game.gameCode, betOptions, options)),
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

  private toModel(game: TenantGameView): GameSettingsFormModel {
    return {
      displayName: game.displayName ?? '',
      visibleInPos: game.visibleInPos,
      minStake: game.minStake,
      maxStake: game.maxStake,
      displayOrder: game.displayOrder,
      availabilityEnabled: game.availabilityEnabled,
      startLocalTime: game.startLocalTime ?? '',
      endLocalTime: game.endLocalTime ?? '',
    };
  }

  private withPricingDetails(
    game: TenantGameView,
    pricing: TenantGamePricingView | null,
  ): TenantGameView {
    if (!pricing) return game;
    return {
      ...game,
      readyForSale: pricing.readiness.status === 'READY',
      betOptions: pricing.odds,
      betOptionGroups: pricing.oddsGroups,
    };
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

  private stakeErrorKeyFor(minStake: number | null, maxStake: number | null): string | null {
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

  private updateBetType(
    betType: string,
    updater: (value: TenantBetTypeOptionConfigView) => TenantBetTypeOptionConfigView,
  ): void {
    this.betOptionResource.update(config => {
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

  private toPricingOddsRequest(gameCode: string): readonly UpsertTenantOddsRequest[] {
    return this.pricingGroups().flatMap(group =>
      group.variants
        .filter(variant => variant.pricingVariantCode && this.isPricingConfigured(variant))
        .map(variant => ({
          gameCode,
          pricingVariantCode: variant.pricingVariantCode ?? '',
          betType: variant.betType,
          betOption: variant.betOption,
          odds: variant.payoutRuleType === 'FIXED_AMOUNT' ? null : variant.odds,
          payoutRuleType: variant.payoutRuleType,
          fixedAmount: variant.payoutRuleType === 'FIXED_AMOUNT' ? variant.fixedAmount : null,
        })),
    );
  }

  private toPricingDeleteRequest(gameCode: string): readonly DeleteTenantOddsRequest[] {
    return this.pricingGroups().flatMap(group =>
      group.variants
        .filter(variant => variant.pricingVariantCode && !this.isPricingConfigured(variant))
        .map(variant => ({
          gameCode,
          pricingVariantCode: variant.pricingVariantCode ?? '',
        })),
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
              ? { ...variant, odds, payoutRuleType: 'STAKE_MULTIPLIER', fixedAmount: null }
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
              ? { ...variant, odds: null, payoutRuleType: 'FIXED_AMOUNT', fixedAmount }
              : variant,
          ),
        };
      }),
    );
  }

  private clearPricingOdds(groupId: string, pricingVariantCode: string): void {
    this.pricingGroups.update(groups =>
      groups.map(group => {
        if (group.id !== groupId) return group;
        return {
          ...group,
          variants: group.variants.map(variant =>
            variant.pricingVariantCode === pricingVariantCode
              ? { ...variant, odds: null, fixedAmount: null }
              : variant,
          ),
        };
      }),
    );
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

  private isPricingConfigured(variant: TenantGameOddGroupView['variants'][number]): boolean {
    if (variant.payoutRuleType === 'FIXED_AMOUNT') {
      return variant.fixedAmount !== null && variant.fixedAmount !== undefined;
    }
    return variant.odds !== null && variant.odds !== undefined;
  }

  private pricingVariantKey(groupId: string, pricingVariantCode: string, label: string): string {
    return `${groupId}:${pricingVariantCode}:${label}`;
  }

  private isSimpleStakeGame(game: TenantGameView): boolean {
    const code = game.gameCode.toUpperCase();
    return code.includes('BOLET') || code.includes('BORLETTE');
  }

  private snapshot(value: unknown): string {
    return JSON.stringify(value);
  }
}
