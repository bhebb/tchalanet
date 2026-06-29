import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'tch-public-empty-state',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="pub-empty">
      <span class="material-symbols-outlined pub-empty__icon" aria-hidden="true">{{ icon() }}</span>
      @if (title()) {
        <h3 class="pub-empty__title">{{ title() }}</h3>
      }
      @if (message()) {
        <p class="pub-empty__msg">{{ message() }}</p>
      }
      <ng-content />
    </div>
  `,
  styles: [
    `
      .pub-empty {
        display: grid;
        justify-items: center;
        gap: 0.75rem;
        padding: 3rem 1rem;
        border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        border-radius: var(--tch-radius-xl, 24px);
        background: var(--tch-color-surface-container-lowest, var(--mat-sys-surface));
        text-align: center;
      }

      .pub-empty__icon {
        font-size: 2.5rem;
        color: var(--tch-color-outline, var(--mat-sys-outline));
      }

      .pub-empty__title {
        margin: 0;
        color: var(--tch-color-primary, var(--mat-sys-primary));
        font-size: var(--tch-font-size-title-md, 1rem);
        font-weight: 800;
      }

      .pub-empty__msg {
        margin: 0;
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        font-size: var(--tch-font-size-body-sm, 0.875rem);
        max-width: 34rem;
      }
    `,
  ],
})
export class PublicEmptyStateComponent {
  readonly icon = input('search_off');
  readonly title = input('');
  readonly message = input('');
}
