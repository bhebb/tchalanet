import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  output,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import {
  ActiveLimitItem,
  extractSelections,
  formatActiveLimitParams,
} from '../../data-access/admin-limits.models';

@Component({
  selector: 'tch-admin-limit-item-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, TranslatePipe, MatButtonModule, MatMenuModule, MatSlideToggleModule],
  templateUrl: './admin-limit-item-card.component.html',
  styleUrl: './admin-limit-item-card.component.scss',
})
export class AdminLimitItemCardComponent {
  private readonly translate = inject(TranslateService);

  readonly item = input.required<ActiveLimitItem>();
  readonly saving = input(false);

  readonly edit = output<void>();
  readonly toggleEnabled = output<void>();
  readonly delete = output<void>();

  readonly isActive = computed(() => this.item().enabled);
  readonly detailRoute = computed(() => ['/app/admin/limits', this.item().assignmentId]);
  readonly selections = computed(() => extractSelections(this.item()));
  readonly visibleBalls = computed(() => this.selections().slice(0, 1));
  readonly hiddenCount = computed(() => Math.max(0, this.selections().length - 1));

  readonly ruleLabel = computed(() =>
    this.translate.instant(`admin.limits.rule.${this.item().ruleKey}.label`),
  );

  readonly statusLabel = computed(() =>
    this.translate.instant(
      this.isActive()
        ? 'admin.limits.overview.limitStatus.active'
        : 'admin.limits.overview.limitStatus.disabled',
    ),
  );

  readonly targetLabel = computed(() => {
    const item = this.item();
    if (item.targetType === 'TENANT') {
      return this.translate.instant('admin.limits.overview.target.tenant');
    }
    return item.targetLabel ?? item.targetCode ?? item.targetId;
  });

  readonly paramsLabel = computed(() => formatActiveLimitParams(this.item()));

  readonly scopeTypeLabel = computed(() =>
    this.translate.instant(`admin.limits.overview.scopeType.${this.item().targetType}`),
  );
}
