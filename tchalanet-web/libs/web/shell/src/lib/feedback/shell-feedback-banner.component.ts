import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  Output,
  computed,
  signal,
} from '@angular/core';

import { copyToClipboard } from './copy-error-details';
import { ShellFeedbackItem, ShellFeedbackVerbosity } from './shell-feedback.model';

@Component({
  selector: 'tch-shell-feedback-banner',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './shell-feedback-banner.component.html',
  styleUrl: './shell-feedback-banner.component.scss',
})
export class ShellFeedbackBannerComponent {
  @Input({ required: true }) item!: ShellFeedbackItem;
  @Input({ required: true }) verbosity!: ShellFeedbackVerbosity;
  @Output() dismissed = new EventEmitter<string>();

  protected readonly copied = signal(false);
  protected readonly expanded = signal(false);

  protected get icon(): string {
    switch (this.item.severity) {
      case 'error':
        return 'error';
      case 'warn':
        return 'warning';
      default:
        return 'info';
    }
  }

  protected showDetails = computed(
    () => this.verbosity === 'standard' || this.verbosity === 'verbose',
  );

  protected hasDiagnostic(): boolean {
    return !!(this.item.requestId || this.item.traceId || this.item.spanId || this.item.errorId);
  }

  protected toggleDetails(): void {
    this.expanded.update(value => !value);
  }

  protected copy(): void {
    if (!this.item.copyText) return;
    copyToClipboard(this.item.copyText);
    this.copied.set(true);
    setTimeout(() => this.copied.set(false), 2000);
  }
}
