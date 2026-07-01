import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { FormGroup } from '@angular/forms';

import {
  AdminStatusTone,
  TchIdentityCardComponent,
  TchIdentityCardMeta,
} from '@tch/ui/console';
import {
  PromotionCampaignView,
  PromotionConfigItem,
} from '../../../data-access/admin-promotions-api.service';

@Component({
  selector: 'tch-maryaj-config-summary',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TchIdentityCardComponent],
  templateUrl: './maryaj-config-summary.component.html',
  styleUrls: ['./maryaj-config-summary.component.scss'],
})
export class MaryajConfigSummaryComponent {
  readonly gameReady = input.required<boolean>();
  readonly campaign = input<PromotionCampaignView | null>(null);
  readonly effect = input<PromotionConfigItem | null>(null);
  readonly form = input.required<FormGroup>();
  readonly editing = input.required<boolean>();

  readonly regenerableLabel = input.required<string>();

  readonly statusLabel = computed(() => {
    if (!this.gameReady()) return 'À configurer';
    return this.campaign()?.status === 'ACTIVE' ? 'Actif' : 'À activer';
  });

  readonly statusTone = computed<AdminStatusTone>(() => {
    if (!this.gameReady()) return 'warning';
    return this.campaign()?.status === 'ACTIVE' ? 'success' : 'neutral';
  });

  readonly meta = computed<readonly TchIdentityCardMeta[]>(() => [
    { label: 'Jeu', value: this.gameReady() ? 'Prêt pour la vente' : 'À configurer' },
    { label: 'Attribution', value: this.quantityModeSummary() },
    { label: 'Ticket', value: this.ticketSummary() },
    { label: 'Sélection', value: this.selectionSummary() },
    { label: 'Régénération', value: this.regenerationSummary() },
  ]);

  effectParam(name: string): string | null {
    const value = this.effect()?.params?.[name];
    return value == null || value === '' ? null : String(value);
  }

  formValue(name: string): string {
    const value = this.form().get(name)?.value;
    return value == null || value === '' ? '—' : String(value);
  }

  private ticketSummary(): string {
    if (this.quantityMode() === 'TIERED_PAID_AMOUNT') {
      const count = this.effectQuantityTierCount();
      const gain = this.valueFromEffectOrForm('payoutBaseAmount');
      return `${count} palier(s), gain ${gain} HTG`;
    }

    if (this.quantityMode() === 'PER_PAID_AMOUNT') {
      return `${this.stepSummary()}, max ${this.valueFromEffectOrForm('maxQuantity')} ligne(s), gain ${this.valueFromEffectOrForm('payoutBaseAmount')} HTG`;
    }

    if (this.campaign()) {
      return `${this.effectParam('quantity') ?? '—'} Maryaj gratuit(s), gain ${this.effectParam('payoutBaseAmount') ?? '—'} HTG`;
    }
    return `${this.formValue('quantity')} Maryaj gratuit(s), gain ${this.formValue('payoutBaseAmount')} HTG`;
  }

  private quantityModeSummary(): string {
    if (this.quantityMode() === 'TIERED_PAID_AMOUNT') return 'Par paliers de vente';
    if (this.quantityMode() === 'PER_PAID_AMOUNT') return 'Par tranche de vente';
    return 'Quantité fixe par ticket';
  }

  private stepSummary(): string {
    return `${this.valueFromEffectOrForm('quantityPerStep')} par ${this.valueFromEffectOrForm('stepPaidAmount')} HTG`;
  }

  private quantityMode(): string {
    return this.valueFromEffectOrForm('quantityMode');
  }

  private valueFromEffectOrForm(name: string): string {
    if (this.editing()) return this.formValue(name);
    return this.effectParam(name) ?? this.formValue(name);
  }

  private effectQuantityTierCount(): number {
    if (this.editing()) {
      const formTiers = this.form().get('quantityTiers')?.value;
      return Array.isArray(formTiers) ? formTiers.length : 0;
    }
    const tiers = this.effect()?.params?.['quantityTiers'];
    if (Array.isArray(tiers)) return tiers.length;
    const formTiers = this.form().get('quantityTiers')?.value;
    return Array.isArray(formTiers) ? formTiers.length : 0;
  }

  private selectionSummary(): string {
    const choiceMode = this.valueFromEffectOrForm('choiceMode');
    return choiceMode === 'AUTO_GENERATE'
      ? 'Générée automatiquement'
      : 'Choisie par le vendeur';
  }

  private regenerationSummary(): string {
    if (this.editing()) {
      return `${this.regenerableLabel()} · ${this.formValue('maxRegenerationsBeforeConfirm')} tentative(s)`;
    }
    if (this.campaign()) return `${this.effectParam('maxRegenerationsBeforeConfirm') ?? '—'} tentative(s)`;
    return `${this.regenerableLabel()} · ${this.formValue('maxRegenerationsBeforeConfirm')} tentative(s)`;
  }
}
