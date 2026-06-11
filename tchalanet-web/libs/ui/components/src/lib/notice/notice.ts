import { ChangeDetectionStrategy, Component, input } from '@angular/core';

export type NoticeType = 'info' | 'success' | 'warning' | 'error';

@Component({
  selector: 'tch-notice',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="notice" [attr.data-type]="type()">
      <ng-content />
    </div>
  `,
  styles: [
    `
      .notice {
        --comp-notice-bg: var(--tch-color-surface-container);
        --comp-notice-fg: var(--tch-color-on-surface);
        display: block;
        border-radius: var(--tch-radius-lg, 12px);
        padding: 0.75rem 1rem;
        font-size: var(--tch-font-size-body-md, 1rem);
        line-height: var(--tch-line-height-body-md, 1.5rem);
        border-left: 3px solid currentColor;
        background: var(--comp-notice-bg);
        color: var(--comp-notice-fg);
      }
      .notice[data-type='info'] {
        background: color-mix(in oklab, var(--tch-color-primary, #1a1b4b) 8%, transparent);
        color: var(--tch-color-primary, #1a1b4b);
      }
      .notice[data-type='success'] {
        background: color-mix(in oklab, var(--tch-color-status-ready, #10b981) 10%, transparent);
        color: var(--tch-color-status-ready, #10b981);
      }
      .notice[data-type='warning'] {
        background: color-mix(in oklab, var(--tch-color-status-warning, #f59e0b) 10%, transparent);
        color: color-mix(in oklab, var(--tch-color-status-warning, #f59e0b) 80%, #000 20%);
      }
      .notice[data-type='error'] {
        background: var(--tch-color-error-container, #ffdad6);
        color: var(--tch-color-on-error-container, #93000a);
      }
    `,
  ],
})
export class TchNotice {
  readonly type = input<NoticeType>('info');
}
