import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { FormArray, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatNativeDateModule } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import {
  MaryajQuantityTier,
  PromotionCampaignStatus,
  PromotionCampaignView,
  PromotionConfigItem,
} from '../../../data-access/admin-promotions-api.service';

@Component({
  selector: 'tch-maryaj-offer-panel',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
    DatePipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './maryaj-offer-panel.component.html',
  styleUrls: ['./maryaj-offer-panel.component.scss'],
})
export class MaryajOfferPanelComponent {
  private readonly translate = inject(TranslateService);

  readonly campaign = input<PromotionCampaignView | null>(null);
  readonly effect = input<PromotionConfigItem | null>(null);
  readonly form = input.required<FormGroup>();
  readonly saving = input.required<boolean>();
  readonly gameReady = input.required<boolean>();
  readonly editing = input.required<boolean>();

  readonly activateCampaign = output<PromotionCampaignView>();
  readonly pauseCampaign = output<PromotionCampaignView>();
  readonly instantiateOffer = output<void>();
  readonly editOffer = output<void>();
  readonly cancelEdit = output<void>();
  readonly saveOffer = output<void>();
  readonly addQuantityTier = output<void>();
  readonly removeQuantityTier = output<number>();
  readonly manualSelectionChange = output<boolean>();

  statusLabel(status: PromotionCampaignStatus): string {
    switch (status) {
      case 'ACTIVE':
        return this.translate.instant('admin.maryajGratis.offer.status.active');
      case 'PAUSED':
        return this.translate.instant('admin.maryajGratis.offer.status.paused');
      case 'DRAFT':
        return this.translate.instant('admin.maryajGratis.offer.status.draft');
      case 'INACTIVE':
        return this.translate.instant('admin.maryajGratis.offer.status.inactive');
      case 'ARCHIVED':
        return this.translate.instant('admin.maryajGratis.offer.status.archived');
    }
  }

  effectParam(name: string): string {
    const value = this.effect()?.params?.[name];
    return value == null || value === ''
      ? this.translate.instant('common.not_available')
      : String(value);
  }

  showCampaignEndDate(campaign: PromotionCampaignView): boolean {
    return !!campaign.endsAt && !this.isPermanentCampaign(campaign);
  }

  isPermanentCampaign(campaign: PromotionCampaignView): boolean {
    return !campaign.endsAt || this.isLongRunningMaryajCampaign(campaign);
  }

  selectionLabel(): string {
    return this.effectParam('choiceMode') === 'AUTO_GENERATE'
      ? this.translate.instant('admin.maryajGratis.offer.selection.auto')
      : this.translate.instant('admin.maryajGratis.offer.selection.manual');
  }

  manualSelectionEnabled(): boolean {
    return this.form().get('choiceMode')?.value === 'SELLER_SELECTS';
  }

  autoGenerationEnabled(): boolean {
    return this.form().get('choiceMode')?.value === 'AUTO_GENERATE';
  }

  quantityTiers(): FormArray {
    return this.form().get('quantityTiers') as FormArray;
  }

  effectQuantityTiers(): readonly MaryajQuantityTier[] {
    const raw = this.effect()?.params?.['quantityTiers'];
    if (!Array.isArray(raw)) return [];
    return raw.map(item => {
      const tier = item as Record<string, unknown>;
      return {
        minPaidAmount: this.numberValue(tier['minPaidAmount'], 0),
        maxPaidAmount:
          tier['maxPaidAmount'] == null ? null : this.numberValue(tier['maxPaidAmount'], 0),
        quantity: this.numberValue(tier['quantity'], 0),
      };
    });
  }

  tierRuleLabel(tier: MaryajQuantityTier): string {
    const min = this.formatAmount(tier.minPaidAmount);
    const max = tier.maxPaidAmount == null ? null : this.formatAmount(tier.maxPaidAmount);
    const range = max
      ? this.translate.instant('admin.maryajGratis.offer.tiers.range', { min, max })
      : this.translate.instant('admin.maryajGratis.offer.tiers.openRange', { min });
    return this.translate.instant('admin.maryajGratis.offer.tiers.rule', {
      range,
      quantity: tier.quantity,
    });
  }

  tierLabel(index: number): string {
    return this.translate.instant('admin.maryajGratis.offer.tiers.tier', { index: index + 1 });
  }

  fixedQuantityRule(): string {
    return this.translate.instant('admin.maryajGratis.offer.fixed.rule', {
      quantity: this.effectParam('quantity'),
    });
  }

  perAmountRule(): string {
    return this.translate.instant('admin.maryajGratis.offer.perAmount.rule', {
      quantity: this.effectParam('quantityPerStep'),
      amount: this.formatAmount(this.numberValue(this.effectParam('stepPaidAmount'), 0)),
      max: this.effectParam('maxQuantity'),
    });
  }

  private isLongRunningMaryajCampaign(campaign: PromotionCampaignView): boolean {
    if (!campaign.startsAt || !campaign.endsAt) return false;
    const startsAt = new Date(campaign.startsAt).getTime();
    const endsAt = new Date(campaign.endsAt).getTime();
    if (!Number.isFinite(startsAt) || !Number.isFinite(endsAt)) return false;

    const nineYearsMs = 9 * 365 * 24 * 60 * 60 * 1000;
    return endsAt - startsAt >= nineYearsMs;
  }

  private formatAmount(value: number): string {
    return this.translate.instant('admin.maryajGratis.amount.htg', {
      amount: value.toLocaleString('fr'),
    });
  }

  private numberValue(value: unknown, fallback: number): number {
    if (typeof value === 'number' && Number.isFinite(value)) return value;
    if (typeof value === 'string' && value.trim() !== '') {
      const parsed = Number(value);
      return Number.isFinite(parsed) ? parsed : fallback;
    }
    return fallback;
  }
}
