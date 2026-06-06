import { filterResults, PublicResultListItem } from './public-results.page';

const items: readonly PublicResultListItem[] = [
  {
    id: 'ny',
    gameName: 'New York Afternoon',
    sourceKey: 'new-york',
    drawDateKey: 'date',
    drawTime: '14:30',
    status: 'CONFIRMED',
    numbers: ['12'],
  },
  {
    id: 'fl',
    gameName: 'Florida Evening',
    sourceKey: 'florida',
    drawDateKey: 'date',
    drawTime: '18:45',
    status: 'PENDING',
    numbers: [],
  },
];

describe('PublicResultsPage helpers', () => {
  it('returns all results when the all filter is selected', () => {
    expect(filterResults(items, 'all')).toEqual(items);
  });

  it('filters results by source key', () => {
    expect(filterResults(items, 'new-york')).toEqual([items[0]]);
    expect(filterResults(items, 'florida')).toEqual([items[1]]);
    expect(filterResults(items, 'georgia')).toEqual([]);
  });
});
