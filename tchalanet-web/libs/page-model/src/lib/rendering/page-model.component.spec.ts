import { TestBed } from '@angular/core/testing';

import { PageContentRuntime, PageDynamicPayload } from '../runtime/pagemodel.types';
import { PageModelComponent } from './page-model.component';

const content: PageContentRuntime = {
  layout: {
    rows: [
      { id: 'hero', columns: [{ span: 12, widgets: ['home.hero'] }] },
      { id: 'news', columns: [{ span: 12, widgets: ['home.news'] }] },
    ],
  },
  widgets: {
    'home.hero': { type: 'HeroWidget' },
    'home.news': { type: 'NewsTickerWidget' },
  },
};

const dynamic: PageDynamicPayload = {
  widgets: { 'home.news': { items: [{ id: 'n1' }] } },
  errors: [{ widgetId: 'home.hero', code: 'X' }],
};

/** A two-column row, like the dashboard's trend + breakdown pair. */
const splitContent: PageContentRuntime = {
  layout: {
    rows: [
      {
        id: 'insights',
        columns: [
          { span: 7, widgets: ['dash.trend'] },
          { span: 5, widgets: ['dash.breakdown'] },
        ],
      },
    ],
  },
  widgets: {
    'dash.trend': { type: 'TrendChartWidget' },
    'dash.breakdown': { type: 'BreakdownListWidget' },
  },
};

describe('PageModelComponent', () => {
  function setup() {
    const fixture = TestBed.createComponent(PageModelComponent);
    fixture.componentRef.setInput('content', content);
    fixture.componentRef.setInput('dynamic', dynamic);
    return fixture.componentInstance;
  }

  function split(inputs: { errors: string[]; hideWidgetErrors?: boolean }) {
    const fixture = TestBed.createComponent(PageModelComponent);
    fixture.componentRef.setInput('content', splitContent);
    fixture.componentRef.setInput('dynamic', {
      widgets: {},
      errors: inputs.errors.map(widgetId => ({ widgetId, code: 'X' })),
    } as PageDynamicPayload);
    fixture.componentRef.setInput('hideWidgetErrors', inputs.hideWidgetErrors ?? false);
    return fixture.componentInstance;
  }

  it('exposes layout rows in order', () => {
    expect(setup().rows().map(r => r.id)).toEqual(['hero', 'news']);
  });

  it('resolves a widget config and its dynamic payload by id', () => {
    const cmp = setup();
    expect(cmp.widgetConfig('home.news')?.type).toBe('NewsTickerWidget');
    expect(cmp.widgetDynamic('home.news')).toEqual({ items: [{ id: 'n1' }] });
    expect(cmp.widgetConfig('missing')).toBeUndefined();
  });

  it('exposes contained dynamic errors', () => {
    expect(setup().errors()).toHaveLength(1);
  });

  it('keeps both columns while errors are still shown', () => {
    // The error panel occupies the column, so the layout must not shift under it.
    const cmp = split({ errors: ['dash.trend'] });
    expect(cmp.visibleRows()[0].columns).toHaveLength(2);
  });

  it('drops a column whose only widget is hidden, so the survivor takes the width back', () => {
    const cmp = split({ errors: ['dash.trend'], hideWidgetErrors: true });
    const columns = cmp.visibleRows()[0].columns;
    expect(columns).toHaveLength(1);
    expect(columns[0].widgets).toEqual(['dash.breakdown']);
  });

  it('drops the row entirely when every column is hidden', () => {
    const cmp = split({ errors: ['dash.trend', 'dash.breakdown'], hideWidgetErrors: true });
    expect(cmp.visibleRows()).toHaveLength(0);
  });

  it('respects a per-widget hideOnError without a page-level opt-out', () => {
    const fixture = TestBed.createComponent(PageModelComponent);
    fixture.componentRef.setInput('content', {
      ...splitContent,
      widgets: {
        ...splitContent.widgets,
        'dash.trend': { type: 'TrendChartWidget', props: { hideOnError: true } },
      },
    } as PageContentRuntime);
    fixture.componentRef.setInput('dynamic', {
      widgets: {},
      errors: [{ widgetId: 'dash.trend', code: 'X' }],
    } as PageDynamicPayload);

    expect(fixture.componentInstance.visibleRows()[0].columns).toHaveLength(1);
  });
});
