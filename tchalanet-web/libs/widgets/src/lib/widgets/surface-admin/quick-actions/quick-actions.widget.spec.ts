import { describe, expect, it } from 'vitest';

import { WidgetAction } from '@tch/page-model';

import { QuickActionsWidget } from './quick-actions.widget';

describe('QuickActionsWidget route destinations', () => {
  const widget = Object.create(QuickActionsWidget.prototype) as QuickActionsWidget;

  it('separates a route query string for Angular RouterLink', () => {
    const action: WidgetAction = {
      id: 'todaysDraws',
      destination: { kind: 'route', value: '/app/admin/draws?date=TODAY' },
    };

    expect(widget.routerPath(action)).toBe('/app/admin/draws');
    expect(widget.routerQueryParams(action)).toEqual({ date: 'TODAY' });
  });

  it('keeps routes without query parameters unchanged', () => {
    const action: WidgetAction = {
      id: 'dailyReport',
      destination: { kind: 'route', value: '/app/admin/reports/daily' },
    };

    expect(widget.routerPath(action)).toBe('/app/admin/reports/daily');
    expect(widget.routerQueryParams(action)).toBeNull();
  });
});
