import {
  actionFrom,
  actionsFrom,
  destinationHref,
  mapBackendDestination,
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
