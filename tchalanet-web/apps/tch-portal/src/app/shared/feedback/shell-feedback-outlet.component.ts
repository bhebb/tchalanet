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
      <div class="sfo" role="region" aria-label="Notifications système">
        @for (item of store.items(); track item.id) {
          <tch-shell-feedback-banner
            [item]="item"
            [verbosity]="verbosity"
            (dismissed)="store.dismiss($event)"
          />
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
  `],
})
export class ShellFeedbackOutletComponent {
  @Input({ required: true }) verbosity!: ShellFeedbackVerbosity;

  protected readonly store = inject(ShellFeedbackStore);
}
