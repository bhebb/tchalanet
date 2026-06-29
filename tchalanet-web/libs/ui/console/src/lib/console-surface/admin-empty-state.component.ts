import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { TchEmptyState } from '@tch/ui/components';

@Component({
  selector: 'tch-admin-empty-state',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TchEmptyState],
  template: `
    <tch-empty-state [title]="title()" [message]="message() ?? ''">
      <span slot="icon" class="empty-icon material-symbols-outlined" aria-hidden="true">
        {{ icon() }}
      </span>
    </tch-empty-state>
    <ng-content />
  `,
  styles: [
    `
      .empty-icon {
        font-size: 2rem;
        color: var(--tch-color-on-surface-variant, #46464f);
        font-variation-settings:
          'FILL' 0,
          'wght' 300,
          'GRAD' 0,
          'opsz' 24;
      }

      .material-symbols-outlined {
        font-family: 'Material Symbols Outlined';
      }
    `,
  ],
})
export class AdminEmptyStateComponent {
  readonly icon = input<string>('construction');
  readonly title = input.required<string>();
  readonly message = input<string | null>(null);
}
