import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { PageContentRuntime, PageDynamicPayload, WidgetConfig } from '../../shared/types';
import { LabelPipe } from './label.pipe';
import { WidgetHostComponent } from './widget-host.component';

/**
 * Renders runtime content by walking `layout.rows[].columns[]` in order and delegating
 * each widget id to a `tch-widget-host`. A 12-column grid maps each column's `span`. The renderer
 * is engine-only: it does not interpret widget payloads, that is each widget's job.
 */
@Component({
  selector: 'tch-page-model',
  imports: [WidgetHostComponent, LabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page-model">
      @for (row of rows(); track row.id ?? $index) {
        <section class="page-model__row" [attr.aria-label]="row.labelKey | tchLabel">
          @for (column of row.columns; track $index) {
            <div class="page-model__col" [style.--col-span]="column.span">
              @for (widgetId of column.widgets; track widgetId) {
                <tch-widget-host
                  [widgetId]="widgetId"
                  [config]="widgetConfig(widgetId)"
                  [dynamic]="widgetDynamic(widgetId)"
                  [errors]="errors()"
                />
              }
            </div>
          }
        </section>
      }
    </div>
  `,
  styles: [
    `
      .page-model {
        display: grid;
        gap: 1.5rem;
        max-width: 1120px;
        margin: 0 auto;
        padding: 1.5rem;
      }
      .page-model__row {
        display: grid;
        grid-template-columns: repeat(12, 1fr);
        gap: 1.5rem;
      }
      .page-model__col {
        grid-column: span min(var(--col-span, 12), 12);
        display: grid;
        gap: 1.5rem;
      }
      @media (max-width: 720px) {
        .page-model__col {
          grid-column: span 12;
        }
      }
    `,
  ],
})
export class PageModelComponent {
  readonly content = input.required<PageContentRuntime>();
  readonly dynamic = input.required<PageDynamicPayload>();

  readonly rows = computed(() => this.content().layout.rows);
  readonly errors = computed(() => this.dynamic().errors);

  widgetConfig(id: string): WidgetConfig | undefined {
    return this.content().widgets[id];
  }

  widgetDynamic(id: string): unknown {
    return this.dynamic().widgets[id];
  }
}
