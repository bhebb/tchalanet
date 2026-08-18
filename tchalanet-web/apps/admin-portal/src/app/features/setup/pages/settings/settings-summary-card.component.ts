import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { TranslatePipe } from '@ngx-translate/core';

import { AdminSectionCardComponent } from '@tch/ui/console';

export interface SettingsSummaryFact {
  readonly label: string;
  readonly value: string;
  readonly badge: boolean;
  readonly active: boolean;
}

export interface SettingsSummarySectionVm {
  readonly id: string;
  readonly icon: string;
  readonly title: string;
  readonly route: string;
  readonly queryParams?: Record<string, string>;
  readonly facts: readonly SettingsSummaryFact[] | null;
  readonly emptyMessage: string;
}

@Component({
  selector: 'tch-settings-summary-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, MatButtonModule, TranslatePipe, AdminSectionCardComponent],
  templateUrl: './settings-summary-card.component.html',
  styleUrls: ['./settings-summary-card.component.scss'],
})
export class SettingsSummaryCardComponent {
  readonly section = input.required<SettingsSummarySectionVm>();
}
