import {
  actionFrom,
  actionsFrom,
  destinationHref,
  mapBackendDestination,
  toPublicPath,
} from './widget.contract';

describe('widget contract adapters', () => {
  it('maps backend internal destinations to public paths', () => {
    expect(mapBackendDestination({ kind: 'internal', path: '/verifier' })).toEqual({
      type: 'path',
      path: '/public/check-ticket',
    });
    expect(toPublicPath('/results')).toBe('/public/results');
  });

  it('maps external and anchor destinations', () => {
    expect(mapBackendDestination({ kind: 'external', path: 'https://example.com' })).toEqual({
      type: 'external',
      url: 'https://example.com',
    });
    expect(mapBackendDestination({ kind: 'anchor', anchor_id: 'help' })).toEqual({
      type: 'anchor',
      id: 'help',
    });
  });

  it('maps backend actions and hrefs', () => {
    const backendAction = {
      id: 'check_ticket',
      label_key: 'nav.check_ticket',
      kind: 'internal',
      path: '/verifier',
    };
    const action = actionFrom(backendAction);

    expect(action?.destination).toEqual({ type: 'path', path: '/public/check-ticket' });
    expect(destinationHref(action?.destination)).toBe('/public/check-ticket');
    expect(actionsFrom([backendAction])).toHaveLength(1);
  });
});
