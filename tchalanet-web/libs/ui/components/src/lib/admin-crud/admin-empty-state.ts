import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'tch-admin-empty-state',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatIconModule],
  template: `
    <div class="empty-state">
      <mat-icon class="empty-state__icon">{{ icon() }}</mat-icon>
      <h3 class="empty-state__title">{{ title() }}</h3>
      @if (message()) {
        <p class="empty-state__message">{{ message() }}</p>
      }
      @if (actionLabel()) {
        <button mat-flat-button (click)="action.emit()">{{ actionLabel() }}</button>
      }
    </div>
  `,
  styles: [
    `
      .empty-state {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 0.75rem;
        padding: 3rem 1.5rem;
        text-align: center;
      }

      .empty-state__icon {
        font-size: 3rem;
        width: 3rem;
        height: 3rem;
        color: var(--tch-color-on-surface-variant);
        opacity: 0.4;
      }

      .empty-state__title {
        margin: 0;
        font-size: var(--tch-font-size-title-md, 1.125rem);
        font-weight: 600;
        color: var(--tch-color-on-surface);
      }

      .empty-state__message {
        margin: 0;
        max-width: 34rem;
        font-size: var(--tch-font-size-body-md, 1rem);
        color: var(--tch-color-on-surface-variant);
      }
    `,
  ],
})
export class AdminEmptyState {
  readonly icon = input('inbox');
  readonly title = input.required<string>();
  readonly message = input('');
  readonly actionLabel = input('');

  readonly action = output<void>();
}
