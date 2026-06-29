import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

@Component({
  selector: 'tch-error-panel',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="tch-error-panel" role="alert">
      @if (title()) { <h2>{{ title() }}</h2> }
      @if (message()) { <p>{{ message() }}</p> }
      @if (showRetry() && retryLabel()) {
        <button type="button" (click)="retry.emit()">{{ retryLabel() }}</button>
      }
    </section>
  `,
  styles: [`
    :host {
      --comp-error-panel-bg: var(--tch-color-error-container);
      --comp-error-panel-fg: var(--tch-color-on-error-container);
      --comp-error-panel-radius: var(--tch-radius-md);
    }
    .tch-error-panel { display: grid; gap: .75rem; padding: 1rem; border-radius: var(--comp-error-panel-radius); background: var(--comp-error-panel-bg); color: var(--comp-error-panel-fg); }
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
