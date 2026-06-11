import {
  actionFrom,
  actionsFrom,
  destinationHref,
  isBinding,
  mapBackendDestination,
  resolveBinding,
  resolvePath,
  toPublicPath,
} from './widget.contract';

describe('widget contract adapters', () => {
  it('maps backend route destinations to public paths', () => {
    expect(mapBackendDestination({ kind: 'route', value: '/verifier' })).toEqual({
      kind: 'route',
      value: '/public/check-ticket',
    });
    expect(toPublicPath('/results')).toBe('/public/results');
  });

  it('maps external destinations', () => {
    expect(mapBackendDestination({ kind: 'url', value: 'https://example.com' })).toEqual({
      kind: 'url',
      value: 'https://example.com',
    });
  });

  it('maps backend actions and hrefs', () => {
    const backendAction = {
      id: 'check_ticket',
      labelKey: 'nav.check_ticket',
      kind: 'link',
      destination: { kind: 'route', value: '/verifier' },
    };
    const action = actionFrom(backendAction);

    expect(action?.destination).toEqual({ kind: 'route', value: '/public/check-ticket' });
    expect(destinationHref(action?.destination)).toBe('/public/check-ticket');
    expect(actionsFrom([backendAction])).toHaveLength(1);
  });
});

describe('widget data binding', () => {
  it('detects binding descriptors', () => {
    expect(isBinding({ source: 'dynamic', path: 'kpis.total' })).toBe(true);
    expect(isBinding({ source: 'static', path: 'x' })).toBe(false);
    expect(isBinding({ path: 'x' })).toBe(false);
    expect(isBinding(42)).toBe(false);
  });

  it('resolves dot-paths and guards prototype pollution', () => {
    const payload = { kpis: { totalSellers: 1284, nested: { ok: 'yes' } } };
    expect(resolvePath(payload, 'kpis.totalSellers')).toBe(1284);
    expect(resolvePath(payload, 'kpis.nested.ok')).toBe('yes');
    expect(resolvePath(payload, 'kpis.missing')).toBeUndefined();
    expect(resolvePath(payload, '__proto__.polluted')).toBeUndefined();
    expect(resolvePath(undefined, 'a.b')).toBeUndefined();
    expect(resolvePath(payload, '')).toBeUndefined();
  });

  it('resolves bindings against the dynamic payload and passes literals through', () => {
    const dynamic = { kpis: { totalSellers: 1284 } };
    expect(resolveBinding({ source: 'dynamic', path: 'kpis.totalSellers' }, dynamic)).toBe(1284);
    expect(resolveBinding('literal', dynamic)).toBe('literal');
    expect(resolveBinding(7, dynamic)).toBe(7);
    expect(resolveBinding({ source: 'dynamic', path: 'kpis.missing' }, dynamic)).toBeUndefined();
  });
});
