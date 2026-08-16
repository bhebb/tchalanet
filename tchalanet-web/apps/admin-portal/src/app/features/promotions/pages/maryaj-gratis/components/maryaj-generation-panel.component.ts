import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { AdminSectionCardComponent } from '@tch/ui/console';
import { PromotionConfigItem } from '../../../data-access/admin-promotions-api.service';

@Component({
  selector: 'tch-maryaj-generation-panel',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    ReactiveFormsModule,
    AdminSectionCardComponent,
    TranslatePipe,
  ],
  templateUrl: './maryaj-generation-panel.component.html',
  styleUrls: ['./maryaj-generation-panel.component.scss'],
})
export class MaryajGenerationPanelComponent {
  private readonly translate = inject(TranslateService);

  readonly effect = input<PromotionConfigItem | null>(null);
  readonly form = input.required<FormGroup>();
  readonly editing = input.required<boolean>();
  readonly manualSelectionChange = output<boolean>();

  configured(): boolean {
    return this.editing() || this.effect() !== null;
  }

  selectionLabel(): string {
    if (!this.configured()) {
      return this.translate.instant('admin.maryajGratis.generation.notConfigured');
    }
    const choiceMode = this.value('choiceMode');
    return choiceMode === 'SELLER_SELECTS'
      ? this.translate.instant('admin.maryajGratis.offer.selection.manual')
      : this.translate.instant('admin.maryajGratis.offer.selection.auto');
  }

  regenerationLabel(): string {
    if (!this.configured()) {
      return this.translate.instant('admin.maryajGratis.generation.notConfigured');
    }
    if (this.value('choiceMode') === 'SELLER_SELECTS') {
      return this.translate.instant('admin.maryajGratis.generation.notUsed');
    }
    return this.translate.instant('admin.maryajGratis.generation.retryCount', {
      count: this.value('maxRegenerationsBeforeConfirm') ?? '0',
    });
  }

  manualSelectionEnabled(): boolean {
    return this.form().get('choiceMode')?.value === 'SELLER_SELECTS';
  }

  autoGenerationEnabled(): boolean {
    return this.form().get('choiceMode')?.value === 'AUTO_GENERATE';
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
