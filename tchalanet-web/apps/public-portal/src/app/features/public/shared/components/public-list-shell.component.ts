import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'tch-public-list-shell',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="pub-list-shell">
      @if (title()) {
        <header class="pub-list-shell__header">
          <h1 class="pub-list-shell__title">{{ title() }}</h1>
          @if (description()) {
            <p class="pub-list-shell__desc">{{ description() }}</p>
          }
        </header>
      }
      <ng-content select="[filters]" />
      <ng-content select="[content]" />
      <ng-content select="[footer]" />
    </div>
  `,
  styles: [
    `
      .pub-list-shell {
        display: grid;
        gap: 1rem;
      }

      .pub-list-shell__header {
        display: grid;
        gap: 0.375rem;
      }

      .pub-list-shell__title {
        margin: 0;
        color: var(--tch-color-primary, var(--mat-sys-primary));
        font-size: var(--tch-font-size-headline-mobile, 1.5rem);
        line-height: var(--tch-line-height-headline-mobile, 2rem);
        font-weight: 800;
      }

      .pub-list-shell__desc {
        margin: 0;
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
      }

      @media (min-width: 760px) {
        .pub-list-shell {
          gap: 1.5rem;
        }

        .pub-list-shell__title {
          font-size: var(--tch-font-size-headline-lg, 2rem);
          line-height: var(--tch-line-height-headline-lg, 2.5rem);
        }
      }
    `,
  ],
})
export class PublicListShellComponent {
  readonly title = input('');
  readonly description = input('');
}
