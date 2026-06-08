import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

@Component({
  selector: 'tch-error-panel',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="error-panel" role="alert">
      @if (title()) { <h2>{{ title() }}</h2> }
      @if (message()) { <p>{{ message() }}</p> }
      @if (showRetry() && retryLabel()) {
        <button type="button" (click)="retry.emit()">{{ retryLabel() }}</button>
      }
    </section>
  `,
  styles: [`
    :host { --comp-error-bg: var(--tch-color-error-container); --comp-error-fg: var(--tch-color-on-error-container); }
    .error-panel { display: grid; gap: .75rem; padding: 1rem; border-radius: var(--tch-radius-md); background: var(--comp-error-bg); color: var(--comp-error-fg); }
    h2, p { margin: 0; }
    button { width: fit-content; min-height: 2.75rem; padding-inline: 1rem; border: 1px solid currentcolor; border-radius: var(--tch-radius-md); background: transparent; color: inherit; cursor: pointer; }
  `],
})
export class TchErrorPanel {
  readonly title = input('');
  readonly message = input('');
  readonly retryLabel = input('');
  readonly showRetry = input(false);
  readonly retry = output<void>();
}
