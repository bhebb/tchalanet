import { ChangeDetectionStrategy, Component, Input, inject } from '@angular/core';

import { ShellFeedbackBannerComponent } from './shell-feedback-banner.component';
import { ShellFeedbackVerbosity } from './shell-feedback.model';
import { ShellFeedbackStore } from './shell-feedback.store';

@Component({
  selector: 'tch-shell-feedback-outlet',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ShellFeedbackBannerComponent],
  templateUrl: './shell-feedback-outlet.component.html',
  styleUrl: './shell-feedback-outlet.component.scss',
})
export class ShellFeedbackOutletComponent {
  @Input({ required: true }) verbosity!: ShellFeedbackVerbosity;

  protected readonly store = inject(ShellFeedbackStore);
}
