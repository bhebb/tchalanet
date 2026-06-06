import { ChangeDetectionStrategy, Component, input } from '@angular/core';

export type ActionButtonVariant = 'primary' | 'secondary' | 'tertiary';

/**
 * Apply as an attribute on a native <button> or <a>:
 *   <button tch-action variant="primary">Label</button>
 *   <a tch-action variant="secondary" href="/path">Link</a>
 */
@Component({
  selector: 'button[tch-action], a[tch-action]',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<ng-content />`,
  host: {
    '[attr.data-variant]': 'variant()',
    class: 'tch-action',
  },
  styles: [
    `
      :host {
        --comp-action-bg: var(--tch-color-secondary-container);
        --comp-action-fg: var(--tch-color-on-secondary-container);
        --comp-action-outline: var(--tch-color-outline);
        display: inline-flex;
        align-items: center;
        justify-content: center;
        min-height: var(--tch-touch-target, 48px);
        padding: 0 1.25rem;
        border-radius: var(--tch-radius-control, 8px);
        font-weight: 600;
        font-size: 1rem;
        text-decoration: none;
        cursor: pointer;
        border: none;
        transition: opacity var(--tch-duration-fast, 150ms) var(--tch-ease-standard, ease);
      }
      :host:disabled,
      :host[disabled] {
        opacity: 0.38;
        pointer-events: none;
      }
      :host([data-variant='primary']) {
        --comp-action-bg: var(--tch-color-primary);
        --comp-action-fg: var(--tch-color-on-primary);
        background: var(--comp-action-bg);
        color: var(--comp-action-fg);
      }
      :host([data-variant='primary']:hover) {
        opacity: 0.88;
      }
      :host([data-variant='secondary']),
      :host(:not([data-variant])) {
        background: var(--comp-action-bg);
        color: var(--comp-action-fg);
      }
      :host([data-variant='secondary']:hover),
      :host(:not([data-variant]):hover) {
        opacity: 0.88;
      }
      :host([data-variant='tertiary']) {
        background: transparent;
        color: var(--tch-color-primary);
        border: 1px solid var(--comp-action-outline);
      }
      :host([data-variant='tertiary']:hover) {
        background: color-mix(in oklab, var(--tch-color-primary, #1a1b4b) 8%, transparent);
      }
    `,
  ],
})
export class TchActionButton {
  readonly variant = input<ActionButtonVariant>('secondary');
}
