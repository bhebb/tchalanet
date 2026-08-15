import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { FormField, form } from '@angular/forms/signals';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { AdminFormSection, AdminFormShell } from '@tch/ui/components';
import { ConsoleBetLabelPipe, ConsoleGameNamePipe } from '@tch/web/console';

import { TenantGameOddGroupView } from '../../data-access/admin-games-pricing.models';
import {
  TenantBetOptionConfigView,
  TenantGameBetOptionConfigView,
  TenantGameSelectionPolicy,
  TenantGameView,
} from '../../data-access/games-admin-api.service';

export interface GameSettingsFormModel {
  readonly displayName: string;
  readonly visibleInPos: boolean;
  readonly minStake: number | null;
  readonly maxStake: number | null;
  readonly displayOrder: number;
  readonly availabilityEnabled: boolean;
  readonly startLocalTime: string;
  readonly endLocalTime: string;
}

export type GameSettingsForm = ReturnType<typeof form<GameSettingsFormModel>>;

export interface GameSelectionPolicyChange {
  readonly betType: string;
  readonly selectionPolicy: TenantGameSelectionPolicy;
}

export interface GameDefaultOptionChange {
  readonly betType: string;
  readonly defaultOption: number | null;
}

export interface GameBetOptionToggle {
  readonly betType: string;
  readonly optionCode: number;
  readonly checked: boolean;
}

export interface GamePricingRuleTypeChange {
  readonly groupId: string;
  readonly pricingVariantCode: string | null;
  readonly payoutRuleType: 'STAKE_MULTIPLIER' | 'FIXED_AMOUNT';
}

export interface GamePricingValueChange {
  readonly groupId: string;
  readonly pricingVariantCode: string | null;
  readonly rawValue: string;
}

export interface GamePricingVariantRef {
  readonly groupId: string;
  readonly pricingVariantCode: string | null;
  readonly label: string;
}

@Component({
  selector: 'tch-game-settings-editor',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormField,
    AdminFormSection,
    AdminFormShell,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    RouterLink,
    TranslatePipe,
    ConsoleBetLabelPipe,
    ConsoleGameNamePipe,
  ],
  templateUrl: './game-settings-editor.component.html',
  styleUrls: ['./game-settings-editor.component.scss'],
})
export class GameSettingsEditorComponent {
  private readonly translate = inject(TranslateService);

  readonly game = input.required<TenantGameView>();
  readonly form = input.required<GameSettingsForm>();
  readonly model = input.required<GameSettingsFormModel>();
  readonly betOptionConfig = input<TenantGameBetOptionConfigView | null>(null);
  readonly pricingGroups = input<readonly TenantGameOddGroupView[]>([]);
  readonly betOptionLoading = input(false);
  readonly betOptionLoadFailed = input(false);
  readonly editingPricingVariantKey = input<string | null>(null);
  readonly showSalesOptions = input(false);
  readonly selectionPolicies = input.required<readonly TenantGameSelectionPolicy[]>();
  readonly stakeErrorKey = input<string | null>(null);

  readonly submitted = output<Event>();
  readonly selectionPolicyChange = output<GameSelectionPolicyChange>();
  readonly defaultOptionChange = output<GameDefaultOptionChange>();
  readonly optionEnabledChange = output<GameBetOptionToggle>();
  readonly optionVisibleInPosChange = output<GameBetOptionToggle>();
  readonly pricingVariantEdit = output<GamePricingVariantRef>();
  readonly pricingRuleTypeChange = output<GamePricingRuleTypeChange>();
  readonly pricingOddsChange = output<GamePricingValueChange>();
  readonly pricingFixedAmountChange = output<GamePricingValueChange>();
  readonly clearPricingRequested = output<GamePricingVariantRef>();

  optionDescription(betType: string, option: TenantBetOptionConfigView): string | null {
    const key = this.optionDescriptionKey(betType, option.code);
    const translated = this.translate.instant(key);
    if (translated && translated !== key) return translated;
    return option.description;
  }

  formattedStakeAmount(value: number | null): string {
    if (value === null) return this.translate.instant('common.not_available');
    return `${value.toLocaleString('fr')} HTG`;
  }

  isPricingVariantEditing(
    groupId: string,
    pricingVariantCode: string | null,
    label: string,
  ): boolean {
    if (!pricingVariantCode) return false;
    return this.editingPricingVariantKey() === this.pricingVariantKey(groupId, pricingVariantCode, label);
  }

  pricingRuleSummary(variant: TenantGameOddGroupView['variants'][number]): string {
    if (!this.isPricingConfigured(variant)) {
      return this.notConfiguredLabel();
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

  notConfiguredLabel(): string {
    return this.translate.instant('admin.gamesPricing.card.notConfigured');
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

  private optionDescriptionKey(betType: string, optionCode: number): string {
    const normalizedBetType = betType
      .trim()
      .toUpperCase()
      .replace(/\s+/g, '_')
      .replace(/\*/g, 'STAR')
      .replace(/-/g, '_');
    return `admin.games.settings.salesOptions.descriptionByOption.${normalizedBetType}.${optionCode}`;
  }
}
