import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

export interface GamesSetupSummaryItem {
  readonly labelKey: string;
  readonly value: number;
  readonly helpKey: string;
  readonly tone?: 'neutral' | 'success' | 'warning';
}

@Component({
  selector: 'tch-games-setup-summary',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe],
  styles: [`
    :host {
      display: block;
      margin-block: 0.75rem 1rem;
    }

    .games-setup-summary {
      align-items: center;
      color: var(--tch-color-on-surface-variant, #46464f);
      display: flex;
      flex-wrap: wrap;
      font-size: var(--tch-font-size-body-sm, 0.8125rem);
      gap: 0.35rem 0.75rem;
      line-height: 1.4;
      margin: 0;
    }

    .games-setup-summary__item {
      align-items: baseline;
      display: inline-flex;
      gap: 0.25rem;
      white-space: nowrap;
    }

    .games-setup-summary__value {
      color: var(--tch-color-on-surface, #1a1c1e);
      font-weight: var(--tch-weight-bold, 700);
    }

    .games-setup-summary__item--success .games-setup-summary__value {
      color: var(--tch-color-status-success, #0f8b5f);
    }

    .games-setup-summary__item--warning .games-setup-summary__value {
      color: var(--tch-color-status-warning, #b26a00);
    }
  `],
  templateUrl: './games-setup-summary.component.html',
})
export class GamesSetupSummaryComponent {
  readonly items = input.required<readonly GamesSetupSummaryItem[]>();
}
