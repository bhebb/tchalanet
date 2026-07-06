import {
  consoleDrawDetailViewModel,
  consoleDrawRowViewModel,
} from './console-draw-view-models';

describe('console draw view models', () => {
  it('builds a draw row from identity input and preserves page-owned labels/actions', () => {
    const row = consoleDrawRowViewModel({
      id: 'draw-1',
      identityInput: {
        channelCode: 'HT_NY_MID',
        slotKey: 'NY_MID',
        channelName: 'Haïti · New York · Midday',
      },
      statusLabel: 'Ouvert',
      statusTone: 'success',
      resultLabel: 'Attendu',
      resultNumbers: ['123'],
      actions: [{ id: 'viewDetails', label: 'Voir détails' }],
    });

    expect(row.title).toBe('Midday');
    expect(row.identity.providerCode).toBe('NY');
    expect(row.identity.channelName).toBe('New York · Midday');
    expect(row.logoUrl).toContain('/assets/images/logo/ny_logo.png');
    expect(row.statusLabel).toBe('Ouvert');
    expect(row.resultNumbers).toEqual(['123']);
    expect(row.actions?.[0]?.id).toBe('viewDetails');
  });

  it('builds a draw detail view from the same identity contract', () => {
    const view = consoleDrawDetailViewModel({
      identityInput: {
        channelCode: 'HT_TX_1227',
        slotKey: 'TX_1227',
        fallbackTitle: 'TX_1227',
      },
      statusLabel: 'Fermé',
      overviewFacts: [{ label: 'Tenant', value: 'tenant-1', code: true }],
      result: {
        statusLabel: 'Confirmé',
        numbers: ['277', '53', '48'],
      },
      aside: {
        title: 'Texas · 12:27',
        items: [{ label: 'Statut', value: 'Fermé' }],
      },
    });

    expect(view.title).toBe('Texas · 1227');
    expect(view.identity.providerCode).toBe('TX');
    expect(view.statusLabel).toBe('Fermé');
    expect(view.overviewFacts?.[0]?.value).toBe('tenant-1');
    expect(view.result?.numbers).toEqual(['277', '53', '48']);
    expect(view.aside?.items[0]?.label).toBe('Statut');
  });
});
