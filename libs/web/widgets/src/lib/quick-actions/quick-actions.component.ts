// libs/web/widgets-actions/src/lib/quick-actions-widget.component.ts
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TchLink } from '@tchl/types';

export interface QuickActionsProps {
  title?: string;
  actions: TchLink[];
}

@Component({
  standalone: true,
  selector: 'tchl-quick-actions-widget',
  imports: [CommonModule, RouterModule],
  template: `
    <section class="qa">
      <h4>{{ props()?.title || 'Actions rapides' }}</h4>
      <div class="list">
        @for (a of props()?.actions; track a.path) {
        <a [routerLink]="a.path" class="pill">
          @if (a.icon) { <span class="material-icons">{{ a.icon }}</span> }
          {{ a.labelKey }}
        </a>
        }
      </div>
    </section>
  `,
  styles: [
    `
      .qa {
        background: var(--color-surface-container);
        border-radius: var(--radius);
        box-shadow: var(--elev-1);
        padding: clamp(10px, 2vw, 16px);
      }
      h4 {
        margin: 0 0 0.5rem;
        font-size: 1rem;
      }
      .list {
        display: flex;
        flex-wrap: wrap;
        gap: 0.5rem;
      }
      .pill {
        display: inline-flex;
        align-items: center;
        gap: 0.25rem;
        padding: 0.45rem 0.85rem;
        border-radius: 999px;
        text-decoration: none;
        background: var(--mdc-theme-primary);
        color: var(--mdc-theme-on-primary);
        transition: filter 0.15s;
      }
      .pill:hover {
        filter: brightness(0.95);
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuickActionsWidgetComponent {
  props = input.required<QuickActionsProps>();
}
