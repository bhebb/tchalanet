import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'tch-field-error',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `@if (message()) { <p class="field-error" role="alert">{{ message() }}</p> }`,
  styles: [`
    :host { --comp-field-error-fg: var(--tch-color-error); }
    .field-error { margin: .25rem 0 0; color: var(--comp-field-error-fg); font-size: .875rem; }
  `],
})
export class TchFieldError {
  readonly message = input('');
}
