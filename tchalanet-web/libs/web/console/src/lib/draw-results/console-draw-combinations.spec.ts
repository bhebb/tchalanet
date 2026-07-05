import {
  drawCombinationRows,
  drawCombinationRowsFromResult,
  drawDigitsFromNumbers,
  uniquePermutations,
} from './console-draw-combinations';

describe('drawDigitsFromNumbers', () => {
  it('joins a numbers array into a digit string', () => {
    expect(drawDigitsFromNumbers([1, 8, 2])).toBe('182');
  });

  it('strips non-digits and handles null', () => {
    expect(drawDigitsFromNumbers(['1', '2', 'x'])).toBe('12');
    expect(drawDigitsFromNumbers(null)).toBe('');
  });
});

describe('uniquePermutations', () => {
  it('returns 6 for three distinct digits', () => {
    expect(uniquePermutations('182')).toEqual(['128', '182', '218', '281', '812', '821']);
  });

  it('returns 3 for two identical digits', () => {
    expect(uniquePermutations('112')).toEqual(['112', '121', '211']);
  });

  it('returns 24 for four distinct digits', () => {
    expect(uniquePermutations('1234')).toHaveLength(24);
  });

  it('returns 12 for one pair among four digits', () => {
    expect(uniquePermutations('1123')).toHaveLength(12);
  });
});

describe('drawCombinationRows', () => {
  it('Pick 3 exposes Exact + Permuté 6-way for distinct digits', () => {
    const rows = drawCombinationRows('182');

    expect(rows.map(r => r.playType)).toEqual(['Exact', 'Permuté · 6-way']);
    expect(rows[0].winningNumbers).toEqual(['182']);
    expect(rows[1].winningNumbers).toContain('821');
  });

  it('Pick 3 Permuté collapses to 3-way with a repeated digit', () => {
    const rows = drawCombinationRows('112');
    expect(rows[1].playType).toBe('Permuté · 3-way');
  });

  it('Pick 3 does not expose unsupported all-identical box', () => {
    const rows = drawCombinationRows('111');
    expect(rows.map(r => r.playType)).toEqual(['Exact']);
  });

  it('Pick 4 exposes Exact, Permuté, and both pairs', () => {
    const rows = drawCombinationRows('1234');

    expect(rows.map(r => r.playType)).toEqual([
      'Exact',
      'Permuté · 24-way',
      'Deux premiers',
      'Deux derniers',
    ]);
    expect(rows[2].winningNumbers).toEqual(['12 ••']);
    expect(rows[3].winningNumbers).toEqual(['•• 34']);
  });

  it('Pick 4 does not expose unsupported all-identical box', () => {
    const rows = drawCombinationRows('1111');
    expect(rows.map(r => r.playType)).toEqual(['Exact', 'Deux premiers', 'Deux derniers']);
  });

  it('returns no rows for an unsupported length', () => {
    expect(drawCombinationRows('12')).toEqual([]);
    expect(drawCombinationRows(null)).toEqual([]);
  });
});

describe('drawCombinationRowsFromResult', () => {
  it('derives combinations from Haiti result facts without concatenating all lots', () => {
    const rows = drawCombinationRowsFromResult({
      numbers: ['536', '76', '06'],
      haitiResult: { lot1: '536', lot2: '76', lot3: '06' },
    });

    expect(rows.map(r => r.playType)).toEqual(['Loto 3 · Exact', 'Loto 3 · Permuté · 6-way']);
    expect(rows[0].winningNumbers).toEqual(['536']);
  });

  it('uses explicit pick4 facts before numbers fallbacks', () => {
    const rows = drawCombinationRowsFromResult({
      numbers: ['111', '222'],
      sourceResult: { pick4: '1234' },
    });

    expect(rows.map(r => r.playType)).toContain('Loto 4 · Permuté · 24-way');
    expect(rows.find(r => r.playType === 'Loto 4 · Exact')?.winningNumbers).toEqual(['1234']);
  });
});
