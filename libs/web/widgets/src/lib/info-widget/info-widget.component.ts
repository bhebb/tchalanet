// libs/web/widgets-info/src/lib/info-widget.component.ts
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface InfoProps {
  title?: string;
  text?: string;
  icon?: string;
  variant?: 'info' | 'success' | 'warning' | 'error';
}

@Component({
  standalone: true,
  selector: 'tchl-info-widget',
  imports: [CommonModule],
  template: `
    <section class="info" [attr.data-variant]="props()?.variant || 'info'">
      <div class="head">
        @if (props()?.icon) { <span class="material-icons icon">{{ props()?.icon }}</span> }
        <h3 class="h3">{{ props()?.title }}</h3>
      </div>
      @if (props()?.text) {
      <p class="text">{{ props()?.text }}</p>
      }
    </section>
  `,
  styles: [
    `
      .info {
        background: var(--color-surface-container);
        color: var(--color-on-surface);
        border-left: 4px solid var(--color-info);
        border-radius: var(--radius);
        box-shadow: var(--elev-1);
        padding: clamp(10px, 2vw, 16px);
      }
      .head {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        margin-bottom: 0.25rem;
      }
      .icon {
        font-size: 20px;
        opacity: 0.75;
      }
      .text {
        opacity: 0.9;
        margin: 0;
      }

      .info[data-variant='success'] {
        border-left-color: var(--color-success);
      }
      .info[data-variant='warning'] {
        border-left-color: var(--color-warning);
      }
      .info[data-variant='error'] {
        border-left-color: var(--mdc-theme-warn);
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InfoWidgetComponent {
  props = input.required<InfoProps>();
}
