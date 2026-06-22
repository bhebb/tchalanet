import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'tch-admin-page-shell',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page-shell">
      <header class="page-shell__header">
        <div class="page-shell__title-group">
          <h1 class="page-shell__title">{{ title() }}</h1>
          @if (description()) {
            <p class="page-shell__description">{{ description() }}</p>
          }
          <ng-content select="[meta]" />
        </div>
        <div class="page-shell__actions">
          <ng-content select="[actions]" />
        </div>
      </header>
      <div class="page-shell__body">
        <ng-content />
      </div>
    </div>
  `,
  styles: [
    `
      :host {
        display: block;
        min-height: 100%;
        background: var(--tch-color-surface-container-low, #f4f4f8);
        padding: 2rem;
        box-sizing: border-box;
      }

      .page-shell__header {
        display: flex;
        align-items: flex-start;
        justify-content: space-between;
        gap: 1.5rem;
        margin-bottom: 2rem;
      }

      .page-shell__title {
        margin: 0 0 0.25rem;
        font-size: 2rem;
        font-weight: 700;
        line-height: 1.25;
        color: var(--tch-color-on-surface, #1a1c1e);
      }

      .page-shell__description {
        margin: 0;
        font-size: 1rem;
        color: var(--tch-color-on-surface-variant, #46464f);
        max-width: 48rem;
      }

      .page-shell__actions {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        flex-shrink: 0;
      }
    `,
  ],
})
export class AdminPageShellComponent {
  readonly title = input.required<string>();
  readonly description = input<string | null>(null);
}
