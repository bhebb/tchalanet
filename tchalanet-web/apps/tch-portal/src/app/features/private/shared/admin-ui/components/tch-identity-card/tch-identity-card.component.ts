import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import {
  AdminStatusPillComponent,
  AdminStatusTone,
} from '../../admin-status-pill.component';

export interface TchIdentityCardMeta {
  readonly label: string;
  readonly value: string | number | null | undefined;
}

export type TchIdentityCardVariant = 'default' | 'compact';

@Component({
  selector: 'tch-identity-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AdminStatusPillComponent],
  templateUrl: './tch-identity-card.component.html',
  styleUrls: ['./tch-identity-card.component.scss'],
  host: { '[class.is-compact]': "variant() === 'compact'" },
})
export class TchIdentityCardComponent {
  readonly eyebrow = input.required<string>();
  readonly title = input.required<string>();
  readonly code = input<string | null>(null);
  readonly status = input<string | null>(null);
  readonly statusTone = input<AdminStatusTone>('neutral');
  readonly meta = input<readonly TchIdentityCardMeta[]>([]);
  readonly icon = input<string | null>(null);
  /** `compact` = denser layout for the desktop right rail. */
  readonly variant = input<TchIdentityCardVariant>('default');
}
