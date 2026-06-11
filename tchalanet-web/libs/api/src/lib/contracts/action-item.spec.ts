import { describe, expect, it } from 'vitest';

import { actionHref, actionRoute, actionText, isExternalAction, isRouteAction } from './action-item';

describe('ActionItem helpers', () => {
  const route = { id: 'home', labelKey: 'nav.home', destination: { kind: 'route' as const, value: '/home' } };
  const url = { id: 'docs', label: 'Docs', destination: { kind: 'url' as const, value: 'https://example.test' } };

  it('reads text and destinations without resolving bindings', () => {
    expect(actionText(route)).toBe('nav.home');
    expect(actionRoute(route)).toBe('/home');
    expect(actionHref(url)).toBe('https://example.test');
    expect(actionRoute(url)).toBe('');
  });

  it('distinguishes route and external actions', () => {
    expect(isRouteAction(route)).toBe(true);
    expect(isExternalAction(url)).toBe(true);
  });
});
