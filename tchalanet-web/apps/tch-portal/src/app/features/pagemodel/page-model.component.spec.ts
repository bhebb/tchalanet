import { TestBed } from '@angular/core/testing';

import { PageDynamicPayload, PageModelDoc } from '../../shared/types';
import { PageModelComponent } from './page-model.component';

const pageModel: PageModelDoc = {
  meta: { id: 'public.home', schema_version: 2 },
  content: {
    layout: {
      component: 'GridLayout',
      rows: [
        { id: 'hero', columns: [{ span: 12, widgets: ['home.hero'] }] },
        { id: 'news', columns: [{ span: 12, widgets: ['home.news'] }] },
      ],
    },
    widgets: {
      'home.hero': { type: 'HeroWidget' },
      'home.news': { type: 'NewsTickerWidget' },
    },
  },
};

const dynamic: PageDynamicPayload = {
  widgets: { 'home.news': { items: [{ id: 'n1' }] } },
  errors: [{ widgetId: 'home.hero', code: 'X' }],
};

describe('PageModelComponent', () => {
  function setup() {
    const fixture = TestBed.createComponent(PageModelComponent);
    fixture.componentRef.setInput('pageModel', pageModel);
    fixture.componentRef.setInput('dynamic', dynamic);
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
});
