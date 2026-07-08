import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormField, form, submit as submitForm } from '@angular/forms/signals';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { TranslatePipe } from '@ngx-translate/core';
import { BadgeStatus, TchSectionError, TchStatusBadge } from '@tch/ui/components';
import { AdminDialogShellComponent } from '@tch/ui/console';
import { tchMutation } from '@tch/web/async';
import { Observable, concatMap, forkJoin, map, of } from 'rxjs';

import { ConsoleBetLabelPipe, ConsoleGameNamePipe } from '@tch/web/console';
import { TenantGameOddGroupView } from '../../data-access/admin-games-pricing.models';
import {
  AdminGamesPricingApiService,
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
}

@Component({
  selector: 'tch-game-settings-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormField,
    AdminDialogShellComponent,
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
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
  readonly pricingGroups = signal<readonly TenantGameOddGroupView[]>(this.data.game.betOptionGroups ?? []);
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
  ];

  readonly saveSettings = tchMutation<SaveGameConfigRequest, void>({
    source: 'admin.games.settings',
    run: req => this.saveGameConfig(req),
    onSuccess: () => this.dialogRef.close(true),
  });
  readonly feedback = computed(() => this.saveSettings.feedback());

  constructor() {
    this.api.getBetOptionConfig(this.data.game.gameCode, { suppressShellFeedback: true })
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
    submitForm(this.form, async () => {
      this.saveSettings.execute({
        settings: this.toRequest(this.model()),
        betOptions: this.showSalesOptions() ? this.toBetOptionsRequest(this.betOptionConfig()) : null,
        pricingOdds: this.toPricingOddsRequest(),
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

  updatePricingOdds(groupId: string, pricingVariantCode: string | null, rawValue: string): void {
    if (!pricingVariantCode) return;
    const odds = Number(rawValue);
    if (!Number.isFinite(odds) || odds <= 0) return;

    this.pricingGroups.update(groups => groups.map(group => {
      if (group.id !== groupId) return group;
      return {
        ...group,
        variants: group.variants.map(variant => variant.pricingVariantCode === pricingVariantCode
          ? { ...variant, odds, value: `×${odds}` }
          : variant),
      };
    }));
  }

  private toRequest(value: GameSettingsFormModel): UpdateGameSettingsRequest {
    return {
      displayName: value.displayName || null,
      visibleInPos: value.visibleInPos,
      minStake: value.minStake,
      maxStake: value.maxStake,
      displayOrder: value.displayOrder,
      availabilityEnabled: value.availabilityEnabled,
      startLocalTime: value.availabilityEnabled ? (value.startLocalTime || null) : null,
      endLocalTime: value.availabilityEnabled ? (value.endLocalTime || null) : null,
    };
  }

  private saveGameConfig(req: SaveGameConfigRequest): Observable<void> {
    const options = { suppressShellFeedback: true };
    let save$: Observable<unknown> = this.api.updateGameSettings(this.data.game.gameCode, req.settings, options);
    const betOptions = req.betOptions;
    if (betOptions && !this.betOptionLoadFailed()) {
      save$ = save$.pipe(
        concatMap(() => this.api.updateBetOptionConfig(this.data.game.gameCode, betOptions, options)),
      );
    }

    if (req.pricingOdds.length > 0) {
      save$ = save$.pipe(
        concatMap(() => forkJoin(req.pricingOdds.map(item => this.pricingApi.upsertTenantOdds(item, options)))
          .pipe(map(() => undefined))),
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
        betTypes: config.betTypes.map(item => item.betType === betType ? updater(item) : item),
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
      options: current.options.map(option => option.code === optionCode ? updater(option) : option),
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
    return this.pricingGroups().flatMap(group => group.variants
      .filter(variant => variant.pricingVariantCode)
      .map(variant => ({
        gameCode: this.data.game.gameCode,
        pricingVariantCode: variant.pricingVariantCode ?? '',
        betType: variant.betType,
        betOption: variant.betOption,
        odds: variant.odds,
      })));
  }

  private isSimpleStakeGame(): boolean {
    const code = this.data.game.gameCode.toUpperCase();
    return code.includes('BOLET') || code.includes('BORLETTE');
  }
}
