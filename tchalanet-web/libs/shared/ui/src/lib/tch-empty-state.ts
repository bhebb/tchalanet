import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

@Component({
  selector: 'tch-empty-state',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="empty">
      <ng-content select="[slot=icon]" />
      @if (title()) {
        <h3 class="empty__title">{{ title() }}</h3>
      }
      @if (message()) {
        <p class="empty__message">{{ message() }}</p>
      }
      @if (actionLabel()) {
        <button class="empty__action" type="button" (click)="action.emit()">
          {{ actionLabel() }}
        </button>
      }
    </div>
  `,
  styles: [
    `
      .empty {
        display: grid;
        justify-items: center;
        gap: 0.75rem;
        padding: 2rem 1rem;
        text-align: center;
        color: var(--tch-color-on-surface-variant, #464652);
      }
      .empty__title {
        margin: 0;
        font-size: var(--tch-font-size-title-md, 1.125rem);
        color: var(--tch-color-on-surface, #1a1c1e);
      }
      .empty__message {
        margin: 0;
        max-width: 34rem;
        font-size: var(--tch-font-size-body-md, 1rem);
      }
      .empty__action {
        min-height: var(--tch-touch-target, 48px);
        padding: 0 1.25rem;
        border-radius: var(--tch-radius-control, 8px);
        background: var(--tch-color-primary, #1a1b4b);
        color: var(--tch-color-on-primary, #fff);
        font-weight: 600;
        border: none;
        cursor: pointer;
      }
    `,
  ],
})
export class TchEmptyState {
  readonly title = input('');
  readonly message = input('');
  readonly actionLabel = input('');
  readonly action = output<void>();
}
