import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'tch-section-header',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="section-header">
      <div class="section-header__text">
        <h2 class="section-header__title">{{ title() }}</h2>
        @if (subtitle()) {
          <p class="section-header__subtitle">{{ subtitle() }}</p>
        }
      </div>
      <ng-content select="[slot=action]" />
    </div>
  `,
  styles: [
    `
      .section-header {
        --comp-section-title: var(--tch-color-primary);
        --comp-section-subtitle: var(--tch-color-on-surface-variant);
        display: flex;
        justify-content: space-between;
        align-items: flex-end;
        gap: 1rem;
      }
      .section-header__title,
      .section-header__subtitle {
        margin: 0;
      }
      .section-header__title {
        font-size: var(--tch-font-size-headline-lg, 2rem);
        line-height: var(--tch-line-height-headline-lg, 2.5rem);
        color: var(--comp-section-title);
      }
      .section-header__subtitle {
        color: var(--comp-section-subtitle);
        font-size: var(--tch-font-size-body-md, 1rem);
      }
      @media (max-width: 839.98px) {
        .section-header {
          display: grid;
        }
        .section-header__title {
          font-size: var(--tch-font-size-headline-mobile, 1.5rem);
          line-height: var(--tch-line-height-headline-mobile, 2rem);
        }
      }
    `,
  ],
})
export class TchSectionHeader {
  readonly title = input.required<string>();
  readonly subtitle = input('');
}
