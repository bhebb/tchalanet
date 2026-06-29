import { ChangeDetectionStrategy, Component, input } from '@angular/core';

export type TchSectionErrorSeverity = 'info' | 'warn' | 'error';

@Component({
  selector: 'tch-section-error',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (title() || message()) {
      <section class="tch-section-error" [attr.data-severity]="severity()" role="alert">
        @if (title()) { <h3>{{ title() }}</h3> }
        @if (message()) { <p>{{ message() }}</p> }
      </section>
    }
  `,
  styles: [`
    :host {
      --comp-section-error-bg: var(--tch-color-error-container);
      --comp-section-error-fg: var(--tch-color-on-error-container);
      --comp-section-error-accent: var(--tch-color-error);
      --comp-section-error-border: var(--tch-color-outline-variant);
      --comp-section-error-radius: var(--tch-radius-sm);
      display: block;
    }
    .tch-section-error {
      display: grid;
      gap: .375rem;
      padding: .75rem .875rem;
      border: 1px solid var(--comp-section-error-border);
      border-left: .25rem solid var(--comp-section-error-accent);
      border-radius: var(--comp-section-error-radius);
      background: var(--comp-section-error-bg);
      color: var(--comp-section-error-fg);
    }
    .tch-section-error[data-severity='warn'] {
      --comp-section-error-bg: var(--tch-color-secondary-container);
      --comp-section-error-fg: var(--tch-color-on-secondary-container);
      --comp-section-error-accent: var(--tch-color-accent);
    }
    .tch-section-error[data-severity='info'] {
      --comp-section-error-bg: var(--tch-color-primary-container);
      --comp-section-error-fg: var(--tch-color-on-primary-container);
      --comp-section-error-accent: var(--tch-color-primary);
    }
    h3, p { margin: 0; }
    h3 { font-size: var(--tch-font-size-title-md); font-weight: var(--tch-weight-bold, 700); }
    p { font-size: var(--tch-font-size-body-md); line-height: 1.4; }
  `],
})
export class TchSectionError {
  readonly title = input('');
  readonly message = input('');
  readonly severity = input<TchSectionErrorSeverity>('error');
}
