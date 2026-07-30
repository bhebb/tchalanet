import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { AdminStatusTone, TchIdentityCardComponent, TchIdentityCardMeta } from '@tch/ui/console';
import {
  PromotionCampaignView,
  PromotionConfigItem,
} from '../../../data-access/admin-promotions-api.service';

@Component({
  selector: 'tch-maryaj-config-summary',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe, TchIdentityCardComponent],
  templateUrl: './maryaj-config-summary.component.html',
  styleUrls: ['./maryaj-config-summary.component.scss'],
})
export class MaryajConfigSummaryComponent {
  private readonly translate = inject(TranslateService);

  readonly gameReady = input.required<boolean>();
  readonly campaign = input<PromotionCampaignView | null>(null);
  readonly effect = input<PromotionConfigItem | null>(null);
  readonly form = input.required<FormGroup>();
  readonly editing = input.required<boolean>();

  readonly regenerableLabel = input.required<string>();

  readonly statusLabel = computed(() => {
    if (!this.gameReady()) return this.t('admin.maryajGratis.summary.status.configure');
    return this.campaign()?.status === 'ACTIVE'
      ? this.t('admin.maryajGratis.summary.status.active')
      : this.t('admin.maryajGratis.summary.status.activate');
  });

  readonly statusTone = computed<AdminStatusTone>(() => {
    if (!this.gameReady()) return 'warning';
    return this.campaign()?.status === 'ACTIVE' ? 'success' : 'neutral';
  });

  readonly meta = computed<readonly TchIdentityCardMeta[]>(() => [
    {
      label: this.t('admin.maryajGratis.summary.meta.game'),
      value: this.gameReady()
        ? this.t('admin.maryajGratis.summary.game.ready')
        : this.t('admin.maryajGratis.summary.game.configure'),
    },
    {
      label: this.t('admin.maryajGratis.summary.meta.attribution'),
      value: this.quantityModeSummary(),
    },
    { label: this.t('admin.maryajGratis.summary.meta.offered'), value: this.ticketSummary() },
    { label: this.t('admin.maryajGratis.summary.meta.selection'), value: this.selectionSummary() },
    {
      label: this.t('admin.maryajGratis.summary.meta.regeneration'),
      value: this.regenerationSummary(),
    },
  ]);

  effectParam(name: string): string | null {
    const value = this.effect()?.params?.[name];
    return value == null || value === '' ? null : String(value);
  }

  formValue(name: string): string {
    const value = this.form().get(name)?.value;
    return value == null || value === '' ? this.t('common.not_available') : String(value);
  }

  private ticketSummary(): string {
    const mode = this.quantityMode();
    if (mode === this.t('common.not_available')) return this.t('common.not_available');
    if (mode === 'TIERED_PAID_AMOUNT') {
      const count = this.effectQuantityTierCount();
      return this.t('admin.maryajGratis.summary.quantity.tiers', { count });
    }

    if (mode === 'PER_PAID_AMOUNT') {
      return this.t('admin.maryajGratis.summary.quantity.perAmount', {
        step: this.stepSummary(),
        max: this.valueFromEffectOrForm('maxQuantity'),
      });
    }

    if (mode === 'FIXED' && this.campaign()) {
      return this.t('admin.maryajGratis.summary.quantity.fixed', {
        count: this.effectParam('quantity') ?? this.t('common.not_available'),
      });
    }
    return this.t('admin.maryajGratis.summary.quantity.fixed', {
      count: this.formValue('quantity'),
    });
  }

  private quantityModeSummary(): string {
    const mode = this.quantityMode();
    if (mode === 'TIERED_PAID_AMOUNT') return this.t('admin.maryajGratis.offer.mode.tiered');
    if (mode === 'PER_PAID_AMOUNT') return this.t('admin.maryajGratis.offer.mode.perAmount');
    if (mode === 'FIXED') return this.t('admin.maryajGratis.offer.mode.fixed');
    return this.t('common.not_available');
  }

  private stepSummary(): string {
    return this.t('admin.maryajGratis.summary.quantity.step', {
      quantity: this.valueFromEffectOrForm('quantityPerStep'),
      amount: this.valueFromEffectOrForm('stepPaidAmount'),
    });
  }

  private quantityMode(): string {
    return this.valueFromEffectOrForm('quantityMode');
  }

  private valueFromEffectOrForm(name: string): string {
    if (this.editing()) return this.formValue(name);
    if (this.campaign() && !this.effect()) return this.t('common.not_available');
    return this.effectParam(name) ?? this.formValue(name);
  }

  private effectQuantityTierCount(): number {
    if (this.editing()) {
      const formTiers = this.form().get('quantityTiers')?.value;
      return Array.isArray(formTiers) ? formTiers.length : 0;
    }
    const tiers = this.effect()?.params?.['quantityTiers'];
    if (Array.isArray(tiers)) return tiers.length;
    if (this.campaign()) return 0;
    const formTiers = this.form().get('quantityTiers')?.value;
    return Array.isArray(formTiers) ? formTiers.length : 0;
  }

  private selectionSummary(): string {
    const choiceMode = this.valueFromEffectOrForm('choiceMode');
    if (choiceMode === this.t('common.not_available')) return this.t('common.not_available');
    return choiceMode === 'AUTO_GENERATE'
      ? this.t('admin.maryajGratis.summary.selection.auto')
      : this.t('admin.maryajGratis.summary.selection.manual');
  }

  private regenerationSummary(): string {
    if (this.editing()) {
      return this.t('admin.maryajGratis.summary.regeneration.count', {
        label: this.regenerableLabel(),
        count: this.formValue('maxRegenerationsBeforeConfirm'),
      });
    }
    if (this.campaign() && !this.effect()) return this.t('common.not_available');
    if (this.campaign()) {
      return this.t('admin.maryajGratis.summary.regeneration.attempts', {
        count: this.effectParam('maxRegenerationsBeforeConfirm') ?? this.t('common.not_available'),
      });
    }
    return this.t('admin.maryajGratis.summary.regeneration.count', {
      label: this.regenerableLabel(),
      count: this.formValue('maxRegenerationsBeforeConfirm'),
    });
  }

  private t(key: string, params?: Record<string, unknown>): string {
    return this.translate.instant(key, params);
  }
}
