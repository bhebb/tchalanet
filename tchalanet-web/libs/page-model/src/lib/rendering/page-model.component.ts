import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

import { PageContentRuntime, PageDynamicPayload, WidgetConfig } from '../runtime/pagemodel.types';
import { LabelPipe } from '../widget/label.pipe';
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
      @for (row of visibleRows(); track row.id ?? $index) {
        <section class="page-model__row" [attr.aria-label]="row.labelKey | tchLabel">
          @for (column of row.columns; track $index) {
            <div class="page-model__col" [style.--col-span]="column.span">
              @for (widgetId of column.widgets; track widgetId) {
                <tch-widget-host
                  [widgetId]="widgetId"
                  [config]="widgetConfig(widgetId)"
                  [dynamic]="widgetDynamic(widgetId)"
                  [errors]="errors()"
                  [hideErrors]="hideWidgetErrors()"
                  (retry)="widgetRetry.emit($event)"
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
      @use 'breakpoints' as ui;

      .page-model {
        display: grid;
        gap: 1.25rem;
        max-width: 100%;
        margin: 0 auto;
        padding: 1rem;
      }
      .page-model__row {
        display: grid;
        grid-template-columns: repeat(12, 1fr);
        gap: 1.25rem;
        align-items: stretch;
      }
      .page-model__col {
        grid-column: span min(var(--col-span, 12), 12);
        display: grid;
        gap: 1.25rem;
        min-width: 0;
      }
      @include ui.down(expanded) {
        .page-model__col {
          grid-column: span 12;
        }
      }
      @include ui.up(expanded) {
        .page-model {
          max-width: min(var(--tch-page-max, 1280px), calc(100vw - 2rem));
          padding: 1.5rem;
        }
      }
      @include ui.up(large) {
        .page-model {
          padding: 2rem;
        }
      }
    `,
  ],
})
export class PageModelComponent {
  readonly content = input.required<PageContentRuntime>();
  readonly dynamic = input.required<PageDynamicPayload>();
  readonly widgetRetry = output<string>();

  readonly rows = computed(() => this.content().layout.rows);
  /**
   * Suppress every widget error panel on this page.
   *
   * <p>Whether a degraded widget should announce itself is a property of the audience, not of the
   * widget: an ops console wants the diagnostic, a tenant dashboard wants a page that still looks
   * finished. Set it on the surface rather than tagging each widget in the page-model template.
   */
  readonly hideWidgetErrors = input(false);

  readonly errors = computed(() => this.dynamic().errors);

  /**
   * Rows and columns that still have something to show.
   *
   * <p>A hidden widget stops painting but its column keeps its grid span, so suppressing one half
   * of a two-column row left the other half stranded beside a wide gap. Dropping the emptied
   * columns lets the survivors take the width back, and a row with nothing left disappears with
   * its heading rather than leaving a labelled void.
   */
  readonly visibleRows = computed(() =>
    this.rows()
      .map(row => ({
        ...row,
        columns: row.columns.filter(column => column.widgets.some(id => !this.isHidden(id))),
      }))
      .filter(row => row.columns.length > 0),
  );

  /** Mirrors WidgetHostComponent.hidden — a widget in error that the surface chose not to show. */
  private isHidden(id: string): boolean {
    const errored = this.errors().some(e => e.widgetId === id);
    if (!errored) {
      return false;
    }
    return this.hideWidgetErrors() || !!this.widgetConfig(id)?.props?.['hideOnError'];
  }

  widgetConfig(id: string): WidgetConfig | undefined {
    return this.content().widgets[id];
  }

  widgetDynamic(id: string): unknown {
    return this.dynamic().widgets[id];
  }
}
