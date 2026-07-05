import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslatePipe } from '@ngx-translate/core';

import { TchSectionError } from '@tch/ui/components';
import type { AdminSectionTargetError } from '@tch/ui/console';

export type SetupChecklistStatus = 'READY' | 'MISSING' | 'UNKNOWN';
export type SetupChecklistBadgeKind = 'required' | 'blocking' | 'optional' | 'operational';
export type SetupChecklistBodyVariant = 'default' | 'hint' | 'address';

@Component({
  selector: 'tch-setup-checklist-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, TranslatePipe, MatButtonModule, MatIconModule, TchSectionError],
  templateUrl: './setup-checklist-card.component.html',
  styleUrls: ['./setup-checklist-card.component.scss'],
})
export class SetupChecklistCardComponent {
  readonly icon = input.required<string>();
  readonly titleKey = input.required<string>();
  readonly status = input.required<SetupChecklistStatus>();
  readonly badgeKind = input.required<SetupChecklistBadgeKind>();
  readonly body = input.required<string>();
  readonly bodyVariant = input<SetupChecklistBodyVariant>('default');
  readonly ctaKey = input.required<string>();
  readonly route = input.required<string>();
  readonly fragment = input<string | undefined>(undefined);
  readonly sectionErrors = input<readonly AdminSectionTargetError[]>([]);

  /** Some cards (identity, generated-draws, theme, promotions) never highlight a missing state. */
  readonly emphasizeMissing = input(true);

  readonly statusIcon = computed(() => {
    const status = this.status();
    if (status === 'READY') return 'check_circle';
    if (status === 'MISSING') return 'radio_button_unchecked';
    return 'help_outline';
  });

  readonly statusClass = computed(() => {
    const status = this.status();
    if (status === 'READY') return 'status--ready';
    if (status === 'MISSING') return 'status--missing';
    return 'status--unknown';
  });

  readonly badgeLabelKey = computed(() =>
    this.badgeKind() === 'optional'
      ? 'admin.setup.optional'
      : this.badgeKind() === 'operational'
        ? 'admin.setup.operational'
        : 'admin.setup.required',
  );

  readonly isReady = computed(() => this.status() === 'READY');
  readonly isMissing = computed(() => this.emphasizeMissing() && this.status() === 'MISSING');
}
