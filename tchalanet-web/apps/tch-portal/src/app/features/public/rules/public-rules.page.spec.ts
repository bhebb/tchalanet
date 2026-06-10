import { PUBLIC_TCHALA_ENTRIES } from './public-rules.data';
import { filterTchalaEntries } from './public-rules.utils';

describe('PublicRulesPage helpers', () => {
  it('returns all Tchala entries when the query is empty', () => {
    expect(filterTchalaEntries(PUBLIC_TCHALA_ENTRIES, '')).toEqual(PUBLIC_TCHALA_ENTRIES);
  });

  it('filters entries by localized keywords', () => {
    const results = filterTchalaEntries(PUBLIC_TCHALA_ENTRIES, 'dlo');

    expect(results.map(entry => entry.id)).toEqual(['water']);
  });

  it('normalizes accents and casing in search terms', () => {
    const results = filterTchalaEntries(PUBLIC_TCHALA_ENTRIES, 'VOYÁGE');

    expect(results.map(entry => entry.id)).toEqual(['travel']);
  });

  it('returns no entries when no keyword matches', () => {
    expect(filterTchalaEntries(PUBLIC_TCHALA_ENTRIES, 'unknown-term')).toEqual([]);
  });
});
