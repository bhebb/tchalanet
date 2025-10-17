// libs/web/widgets-kpi/src/lib/kpi-widget.component.ts
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface KpiProps {
  label: string;
  value: string | number;
  trend?: 'up' | 'down' | 'flat';
}

@Component({
  standalone: true,
  selector: 'tchl-kpi-widget',
  imports: [CommonModule],
  template: `
    <div class="kpi">
      <div class="label">{{ props()?.label }}</div>
      <div class="value tabnums">{{ props()?.value }}</div>
      <div class="trend" [attr.data-trend]="props()?.trend"></div>
    </div>
  `,
  styles: [
    `
      .kpi {
        background: var(--color-surface-container);
        border-radius: var(--radius);
        box-shadow: var(--elev-1);
        padding: clamp(10px, 2vw, 16px);
      }
      .label {
        color: var(--tch-muted, #6b7280);
        font-size: 0.9rem;
      }
      .value {
        font-weight: 700;
        font-size: clamp(1.1rem, 2.2vw, 1.4rem);
        line-height: 1.1;
      }
      .trend[data-trend='up']::after {
        content: '▲';
        color: #2e7d32;
        margin-left: 0.25rem;
      }
      .trend[data-trend='down']::after {
        content: '▼';
        color: #c62828;
        margin-left: 0.25rem;
      }
      .trend[data-trend='flat']::after {
        content: '—';
        color: #616161;
        margin-left: 0.25rem;
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class KpiWidgetComponent {
  props = input.required<KpiProps>();
}
