import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { PromotionConfigItem } from '../../../data-access/admin-promotions-api.service';

@Component({
  selector: 'tch-maryaj-generation-panel',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatIconModule, TranslatePipe],
  templateUrl: './maryaj-generation-panel.component.html',
  styleUrls: ['./maryaj-generation-panel.component.scss'],
})
export class MaryajGenerationPanelComponent {
  private readonly translate = inject(TranslateService);

  readonly effect = input<PromotionConfigItem | null>(null);
  readonly form = input.required<FormGroup>();
  readonly editing = input.required<boolean>();

  selectionLabel(): string {
    const choiceMode = this.value('choiceMode');
    return choiceMode === 'SELLER_SELECTS'
      ? this.translate.instant('admin.maryajGratis.offer.selection.manual')
      : this.translate.instant('admin.maryajGratis.offer.selection.auto');
  }

  regenerationLabel(): string {
    if (this.value('choiceMode') === 'SELLER_SELECTS') {
      return this.translate.instant('admin.maryajGratis.generation.notUsed');
    }
    return this.translate.instant('admin.maryajGratis.generation.retryCount', {
      count: this.value('maxRegenerationsBeforeConfirm') ?? '0',
    });
  }

  private value(name: string): string | null {
    if (this.editing()) {
      const value = this.form().get(name)?.value;
      return value == null || value === '' ? null : String(value);
    }
    const value = this.effect()?.params?.[name];
    return value == null || value === '' ? null : String(value);
  }
}
