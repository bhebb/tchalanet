import { ChangeDetectionStrategy, Component, Input, inject } from '@angular/core';

import { ShellFeedbackBannerComponent } from './shell-feedback-banner.component';
import { ShellFeedbackVerbosity } from './shell-feedback.model';
import { ShellFeedbackStore } from './shell-feedback.store';

@Component({
  selector: 'tch-shell-feedback-outlet',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ShellFeedbackBannerComponent],
  template: `
    @if (store.hasItems()) {
      <div class="sfo" role="region" aria-label="Notifications systeme">
        @for (item of store.visibleItems(); track item.id) {
          <tch-shell-feedback-banner
            [item]="item"
            [verbosity]="verbosity"
            (dismissed)="store.dismiss($event)"
          />
        }
        @if (store.overflowCount() > 0) {
          <div class="sfo__overflow" role="status">
            {{ store.overflowCount() }} autre{{ store.overflowCount() > 1 ? 's' : '' }} notification{{ store.overflowCount() > 1 ? 's' : '' }}
            <button type="button" class="sfo__clear" (click)="store.clear()">Tout fermer</button>
          </div>
        }
      </div>
    }
  `,
  styles: [`
    .sfo {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
      padding: 0.75rem 1rem;
    }

    .sfo__overflow {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 0.75rem;
      padding: 0.5rem 0.75rem;
      border-radius: var(--tch-radius-sm, 4px);
      background: color-mix(in srgb, var(--tch-color-on-surface, #1f2937) 6%, transparent);
      color: var(--tch-color-on-surface-variant, var(--tch-color-on-surface));
      font-size: 0.8125rem;
    }

    .sfo__clear {
      border: none;
      background: transparent;
      color: var(--tch-color-primary);
      cursor: pointer;
      font: inherit;
      font-weight: 600;
      padding: 0.25rem 0;
    }
  `],
})
export class ShellFeedbackOutletComponent {
  @Input({ required: true }) verbosity!: ShellFeedbackVerbosity;

  protected readonly store = inject(ShellFeedbackStore);
}
