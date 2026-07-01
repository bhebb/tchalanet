import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { FormArray, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import {
  PromotionCampaignStatus,
  PromotionCampaignView,
  PromotionConfigItem,
} from '../../../data-access/admin-promotions-api.service';

@Component({
  selector: 'tch-maryaj-offer-panel',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './maryaj-offer-panel.component.html',
  styleUrls: ['./maryaj-offer-panel.component.scss'],
})
export class MaryajOfferPanelComponent {
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

  statusLabel(status: PromotionCampaignStatus): string {
    switch (status) {
      case 'ACTIVE':
        return 'Actif';
      case 'PAUSED':
        return 'En pause';
      case 'DRAFT':
        return 'Brouillon';
      case 'INACTIVE':
        return 'Inactif';
      case 'ARCHIVED':
        return 'Archivé';
    }
  }

  effectParam(name: string): string {
    const value = this.effect()?.params?.[name];
    return value == null || value === '' ? '—' : String(value);
  }

  selectionLabel(): string {
    return this.effectParam('choiceMode') === 'AUTO_GENERATE'
      ? 'Générée automatiquement'
      : 'Choisie par le vendeur';
  }

  quantityTiers(): FormArray {
    return this.form().get('quantityTiers') as FormArray;
  }

  tierLabel(index: number): string {
    return `Palier ${index + 1}`;
  }
}
