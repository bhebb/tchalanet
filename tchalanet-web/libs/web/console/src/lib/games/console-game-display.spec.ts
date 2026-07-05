import {
  ConsoleSettlementLine,
  consoleBetVariationRows,
  consoleGameName,
  consoleSettlementVariantLabel,
} from './console-game-display';

describe('consoleGameName', () => {
  it('renders Bòlèt as the canonical Haiti game label', () => {
    expect(consoleGameName('HT_BOLET')).toBe('Bòlèt');
    expect(consoleGameName('BORLETTE')).toBe('Bòlèt');
  });
});

describe('consoleSettlementVariantLabel', () => {
  it('prefers the backend-provided admin label', () => {
    expect(consoleSettlementVariantLabel('LOTTO4_BOX_24_WAY', 'Permuté · 24-way')).toBe(
      'Permuté · 24-way',
    );
  });

  it('falls back to the local map when no admin label is provided', () => {
    expect(consoleSettlementVariantLabel('LOTTO4_BOX_12_WAY')).toBe('Permuté · 12-way');
  });

  it('returns null when the variant is missing', () => {
    expect(consoleSettlementVariantLabel(null)).toBeNull();
  });

  it('falls back to a readable code for an unknown variant', () => {
    expect(consoleSettlementVariantLabel('SOME_NEW_VARIANT')).toBe('SOME NEW VARIANT');
  });
});

describe('consoleBetVariationRows', () => {
  const line = (over: Partial<ConsoleSettlementLine>): ConsoleSettlementLine => ({
    lineNo: 1,
    gameCode: 'HT_LOTO4',
    betType: 'LOTTO4_PATTERN',
    betOption: 2,
    selection: '1234',
    commercialLabel: 'Désordre / Box',
    variant: 'LOTTO4_BOX_24_WAY',
    adminLabel: 'Permuté · 24-way',
    resolved: true,
    error: null,
    ...over,
  });

  it('maps a resolved line with commercial + technical labels', () => {
    const [row] = consoleBetVariationRows([line({})]);

    expect(row.gameLabel).toBe('Loto 4');
    expect(row.commercialLabel).toBe('Désordre / Box');
    expect(row.variantLabel).toBe('Permuté · 24-way');
    expect(row.resolved).toBe(true);
  });

  it('hides the technical label and keeps the error for unresolved lines', () => {
    const [row] = consoleBetVariationRows([
      line({ resolved: false, variant: null, adminLabel: null, commercialLabel: null, error: 'bad option' }),
    ]);

    expect(row.variantLabel).toBeNull();
    expect(row.error).toBe('bad option');
    // still shows a commercial label derived from betType/betOption
    expect(row.commercialLabel).toBe('Permuté');
  });

  it('returns an empty array for null input', () => {
    expect(consoleBetVariationRows(null)).toEqual([]);
  });
});
