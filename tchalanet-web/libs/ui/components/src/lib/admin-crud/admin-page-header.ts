import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'tch-admin-page-header',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page-header">
      <div class="page-header__text">
        <h1 class="page-header__title">{{ title() }}</h1>
        @if (description()) {
          <p class="page-header__description">{{ description() }}</p>
        }
      </div>
      <div class="page-header__actions">
        <ng-content select="[actions]" />
      </div>
    </div>
    <ng-content />
  `,
  styles: [
    `
      .page-header {
        display: flex;
        align-items: flex-start;
        justify-content: space-between;
        gap: 1.5rem;
        margin-bottom: 2rem;
        flex-wrap: wrap;
      }

      .page-header__title {
        margin: 0 0 0.25rem;
        font-size: var(--tch-font-size-display-sm, 2rem);
        font-weight: 700;
        line-height: 1.2;
        color: var(--tch-color-on-surface);
      }

      .page-header__description {
        margin: 0;
        font-size: var(--tch-font-size-body-md, 1rem);
        color: var(--tch-color-on-surface-variant);
        max-width: 48rem;
      }

      .page-header__actions {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        flex-shrink: 0;
      }
    `,
  ],
})
export class AdminPageHeader {
  readonly title = input.required<string>();
  readonly description = input<string | null>(null);
}
