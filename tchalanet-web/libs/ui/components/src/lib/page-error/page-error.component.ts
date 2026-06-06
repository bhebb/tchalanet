import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'tch-page-error',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <main class="page-error" role="alert">
      <p class="page-error__code">{{ code() }}</p>
      <h1>{{ title() }}</h1>
      @if (message()) { <p>{{ message() }}</p> }
      @if (backRoute()) { <a [routerLink]="backRoute()">{{ backLabel() }}</a> }
    </main>
  `,
  styles: [`
    :host { --comp-page-error-fg: var(--tch-color-on-surface); display: block; }
    .page-error { display: grid; place-content: center; gap: 1rem; min-height: 60vh; max-width: var(--tch-page-max); margin: auto; padding: var(--tch-page-gutter); color: var(--comp-page-error-fg); text-align: center; }
    .page-error__code { color: var(--tch-color-error); font-weight: 800; }
    h1, p { margin: 0; }
    a { color: var(--tch-color-primary); }
  `],
})
export class TchPageError {
  readonly code = input('');
  readonly title = input.required<string>();
  readonly message = input('');
  readonly backRoute = input('');
  readonly backLabel = input('Retour');
}
