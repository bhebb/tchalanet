import {
  consoleDrawResultRowViewModel,
  consoleDrawResultSummaryFacts,
  consoleDrawResultSummaryViewModel,
} from './console-draw-result-view-models';

describe('console draw result view models', () => {
  it('builds a result row from identity input and page-owned labels', () => {
    const row = consoleDrawResultRowViewModel({
      id: 'result-1',
      identityInput: {
        channelCode: 'HT_NY_MID',
        slotKey: 'NY_MID',
        channelName: 'Haïti · New York · Midday',
      },
      numbers: ['418', '32', '06'],
      statusLabel: 'Confirmé',
      statusTone: 'success',
      qualityLabel: 'Complet',
      qualityTone: 'success',
      sourceLabel: 'Provider',
      providerDateLabel: '4 juill. 2026',
      providerTimeLabel: '14 h 30',
      actions: [{ id: 'detail', label: 'Voir détail' }],
    });

    expect(row.title).toBe('New York · Midday');
    expect(row.identity.providerCode).toBe('NY');
    expect(row.logoUrl).toContain('/assets/images/logo/ny_logo.png');
    expect(row.numbers).toEqual(['418', '32', '06']);
    expect(row.sourceLabel).toBe('Provider');
    expect(row.actions?.[0]?.id).toBe('detail');
  });

  it('builds summary view and facts without requiring page-specific components', () => {
    const summary = consoleDrawResultSummaryViewModel({
      identityInput: {
        providerCode: 'TX',
        slotKey: 'TX_1800',
      },
      numbers: ['277', '53', '48'],
    });
    const facts = consoleDrawResultSummaryFacts({
      resultFacts: [{ label: 'Statut', value: 'Confirmé' }],
      linkedDrawFacts: [{ label: 'Slot', value: 'TX_1800', code: true }],
    });

    expect(summary.identity.providerCode).toBe('TX');
    expect(summary.numbers).toEqual(['277', '53', '48']);
    expect(facts.resultFacts[0]?.value).toBe('Confirmé');
    expect(facts.linkedDrawFacts[0]?.code).toBe(true);
  });
});
