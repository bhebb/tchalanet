import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'tch-admin-page-shell',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page-shell" data-testid="admin-page-shell">
      <header class="page-shell__header" data-testid="admin-page-header">
        <div class="page-shell__title-group">
          <h1 class="page-shell__title">{{ title() }}</h1>
          @if (description()) {
            <p class="page-shell__description">{{ description() }}</p>
          }
          <ng-content select="[meta]" />
        </div>
        <div class="page-shell__actions" data-testid="admin-page-actions">
          <ng-content select="[actions]" />
        </div>
      </header>
      <div class="page-shell__feedback">
        <ng-content select="[feedback]" />
      </div>
      <div class="page-shell__body" data-testid="admin-page-body">
        <ng-content />
      </div>
    </div>
  `,
  styles: [
    `
      @use 'breakpoints' as ui;

      :host {
        display: block;
        min-height: 100%;
        background: var(--tch-color-surface-container-low, #f4f4f8);
        padding: 1.5rem;
        box-sizing: border-box;
      }

      .page-shell__header {
        display: grid;
        gap: 1rem;
        margin-bottom: 1.25rem;
      }

      .page-shell__title {
        margin: 0 0 0.25rem;
        font-size: clamp(1.5rem, 6vw, 2rem);
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
        justify-content: flex-start;
        gap: 0.5rem;
        flex-wrap: wrap;
      }

      .page-shell__feedback {
        display: grid;
        gap: 0.75rem;
        margin-bottom: 1.25rem;
      }

      .page-shell__feedback:empty {
        display: none;
      }

      @include ui.up(expanded) {
        :host {
          padding: 2rem;
        }

        .page-shell__header {
          display: flex;
          align-items: flex-start;
          justify-content: space-between;
          gap: 1.5rem;
          margin-bottom: 2rem;
        }

        .page-shell__actions {
          justify-content: flex-end;
          flex-shrink: 0;
        }
      }
    `,
  ],
})
export class AdminPageShellComponent {
  readonly title = input.required<string>();
  readonly description = input<string | null>(null);
}
