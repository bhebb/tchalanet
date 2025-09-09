// libs/web/layout/src/lib/grid-layout.component.ts
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { WidgetRendererComponent } from '@tchl/ui/widget-renderer';
import { Layout, PageElement, Widget } from '@tchl/types';

type Platform = 'web' | 'mobile';

@Component({
  selector: 'tchl-grid-layout',
  standalone: true,
  imports: [CommonModule, WidgetRendererComponent],
  template: `
    <div class="grid-container">
      <div class="grid-inner">
        @for (row of layout().rows; track $index) {
        <div class="grid-row">
          @for (col of row.columns; track $index) {
          <div class="grid-col" [style.--span]="safeSpan(col.span)">
            @for (w of visibleWidgets(col.widgets); track $index) {
            <div class="widget-wrap">
              <tchl-widget-renderer [el]="w" />
            </div>
            }
          </div>
          }
        </div>
        }
      </div>
    </div>
  `,
  styles: [
    `
      :host {
        display: block;
        background: var(--color-surface);
        color: var(--color-on-surface);
      }
      .grid-container {
        display: flex;
        justify-content: center;
        padding-inline: clamp(12px, 4vw, 24px);
      }
      .grid-inner {
        width: min(var(--container-max, 1200px), 100%);
        display: flex;
        flex-direction: column;
        gap: var(--tch-gap-lg, 24px);
      }
      .grid-row {
        display: grid;
        grid-template-columns: repeat(12, minmax(0, 1fr));
        gap: var(--tch-gap-lg, 24px);
      }
      .grid-col {
        grid-column: span var(--span, 12);
        min-width: 0;
      }
      .widget-wrap {
        background: var(--color-surface-container);
        border-radius: var(--radius);
        box-shadow: var(--elev-1);
        padding: clamp(10px, 2vw, 16px);
      }
      @media (max-width: 767px) {
        .grid-row {
          grid-template-columns: 1fr;
        }
        .grid-col {
          grid-column: span 1;
        }
        .grid-inner {
          gap: var(--tch-gap-md, 16px);
        }
        .widget-wrap {
          border-radius: calc(var(--radius) - 2px);
        }
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GridLayoutComponent {
  layout = input.required<Layout>();
  platform = input<Platform>('web');

  safeSpan = (n?: number) => Math.max(1, Math.min(12, n ?? 12));

  visibleWidgets = (widgets: Array<PageElement | Widget>) => {
    const plat = this.platform();
    return (widgets ?? []).filter(w => {
      const showOn = (w as any).showOn as Platform[] | undefined;
      return !showOn || showOn.length === 0 || showOn.includes(plat);
    });
  };
}
