import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { AdminStatusPillComponent, type AdminStatusTone } from '@tch/ui/console';
import {
  type MaryajQuantityTier,
  type PromotionCampaignView,
  type PromotionConfigItem,
} from '../../../../data-access/admin-promotions-api.service';

type OfferCardStatus = 'active' | 'scheduled' | 'ended' | 'paused' | 'inactive' | 'empty';

export interface OfferCardError {
  readonly title: string;
  readonly message: string;
}

@Component({
  selector: 'tch-maryaj-offer-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, TranslatePipe, MatButtonModule, MatIconModule, AdminStatusPillComponent],
  templateUrl: './maryaj-offer-card.component.html',
  styleUrls: ['./maryaj-offer-card.component.scss'],
})
export class MaryajOfferCardComponent {
  private readonly translate = inject(TranslateService);

  readonly campaign = input<PromotionCampaignView | null>(null);
  readonly effect = input<PromotionConfigItem | null>(null);
  readonly actionError = input<OfferCardError | null>(null);
  readonly saving = input(false);
  readonly gameReady = input(true);
  readonly canManage = input(true);

  readonly edit = output<void>();
  readonly activateCampaign = output<PromotionCampaignView>();
  readonly pauseCampaign = output<PromotionCampaignView>();
  readonly createOffer = output<void>();

  readonly cardStatus = computed<OfferCardStatus>(() => {
    const c = this.campaign();
    if (!c) return 'empty';
    if (c.status === 'PAUSED') return 'paused';
    if (c.status !== 'ACTIVE') return 'inactive';
    const lc = this.lifecycleOf(c);
    if (lc === 'SCHEDULED') return 'scheduled';
    if (lc === 'ENDED') return 'ended';
    return 'active';
  });

  readonly statusTone = computed<AdminStatusTone>(() => {
    switch (this.cardStatus()) {
      case 'active': return 'success';
      case 'scheduled':
      case 'paused':
      case 'empty': return 'warning';
      default: return 'neutral';
    }
  });

  readonly inlineWarningKey = computed<string | null>(() => {
    if (this.cardStatus() === 'ended') return 'admin.maryajGratis.offer.warning.ended';
    const mode = this.effect()?.params?.['quantityMode'];
    if (mode === 'TIERED_PAID_AMOUNT' && this.effectQuantityTiers().length === 0) {
      return 'admin.maryajGratis.offer.warning.noTiers';
    }
    return null;
  });

  readonly statusLabelKey = computed<string>(() => {
    switch (this.cardStatus()) {
      case 'active':    return 'admin.maryajGratis.offer.status.active';
      case 'scheduled': return 'admin.maryajGratis.offer.status.scheduled';
      case 'ended':     return 'admin.maryajGratis.offer.status.ended';
      case 'paused':    return 'admin.maryajGratis.offer.status.paused';
      case 'empty':     return 'admin.maryajGratis.offer.status.needsSetup';
      default:          return 'admin.maryajGratis.offer.status.inactive';
    }
  });

  isPermanent(campaign: PromotionCampaignView): boolean {
    return !campaign.endsAt || this.isLongRunning(campaign);
  }

  effectParam(name: string): string {
    const value = this.effect()?.params?.[name];
    return value == null || value === '' ? '' : String(value);
  }

  effectQuantityTiers(): readonly MaryajQuantityTier[] {
    const raw = this.effect()?.params?.['quantityTiers'];
    if (!Array.isArray(raw)) return [];
    return raw.map(item => {
      const t = item as Record<string, unknown>;
      return {
        minPaidAmount: this.toNum(t['minPaidAmount'], 0),
        maxPaidAmount: t['maxPaidAmount'] == null ? null : this.toNum(t['maxPaidAmount'], 0),
        quantity: this.toNum(t['quantity'], 0),
      };
    });
  }

  tierRuleLabel(tier: MaryajQuantityTier): string {
    const min = this.fmtAmount(tier.minPaidAmount);
    const max = tier.maxPaidAmount == null ? null : this.fmtAmount(tier.maxPaidAmount);
    const range = max
      ? this.t('admin.maryajGratis.offer.tiers.range', { min, max })
      : this.t('admin.maryajGratis.offer.tiers.openRange', { min });
    return this.t('admin.maryajGratis.offer.tiers.rule', { range, quantity: tier.quantity });
  }

  fixedQuantityRule(): string {
    return this.t('admin.maryajGratis.offer.fixed.rule', {
      quantity: this.effectParam('quantity'),
    });
  }

  perAmountRule(): string {
    return this.t('admin.maryajGratis.offer.perAmount.rule', {
      quantity: this.effectParam('quantityPerStep'),
      amount: this.fmtAmount(this.toNum(this.effectParam('stepPaidAmount'), 0)),
      max: this.effectParam('maxQuantity'),
    });
  }

  private lifecycleOf(c: PromotionCampaignView): 'CURRENT' | 'SCHEDULED' | 'ENDED' {
    const now = Date.now();
    const startsAt = c.startsAt ? new Date(c.startsAt).getTime() : null;
    if (startsAt !== null && Number.isFinite(startsAt) && startsAt > now) return 'SCHEDULED';
    if (this.isPermanent(c)) return 'CURRENT';
    const endsAt = c.endsAt ? new Date(c.endsAt).getTime() : null;
    if (endsAt !== null && Number.isFinite(endsAt) && endsAt < now) return 'ENDED';
    return 'CURRENT';
  }

  private isLongRunning(c: PromotionCampaignView): boolean {
    if (!c.startsAt || !c.endsAt) return false;
    const s = new Date(c.startsAt).getTime();
    const e = new Date(c.endsAt).getTime();
    if (!Number.isFinite(s) || !Number.isFinite(e)) return false;
    return e - s >= 9 * 365 * 24 * 60 * 60 * 1000;
  }

  private fmtAmount(value: number): string {
    return this.t('admin.maryajGratis.amount.htg', { amount: value.toLocaleString('fr') });
  }

  private toNum(value: unknown, fallback: number): number {
    if (typeof value === 'number' && Number.isFinite(value)) return value;
    if (typeof value === 'string' && value.trim() !== '') {
      const n = Number(value);
      return Number.isFinite(n) ? n : fallback;
    }
    return fallback;
  }

  private t(key: string, params?: Record<string, unknown>): string {
    return this.translate.instant(key, params);
  }
}
