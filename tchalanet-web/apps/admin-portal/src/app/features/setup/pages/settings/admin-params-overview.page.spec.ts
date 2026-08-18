import { describe, expect, it } from 'vitest';

import {
  makeBadgeFact,
  makeTextFact,
  paperSizeLabel,
} from './params-overview.utils';

describe('paperSizeLabel', () => {
  it('maps RECEIPT_58MM to "58 mm"', () => {
    expect(paperSizeLabel('RECEIPT_58MM')).toBe('58 mm');
  });

  it('maps RECEIPT_80MM to "80 mm"', () => {
    expect(paperSizeLabel('RECEIPT_80MM')).toBe('80 mm');
  });

  it('maps A4 to "A4"', () => {
    expect(paperSizeLabel('A4')).toBe('A4');
  });

  it('defaults to "80 mm" when size is null', () => {
    expect(paperSizeLabel(null)).toBe('80 mm');
  });

  it('passes through an unknown code unchanged', () => {
    expect(paperSizeLabel('RECEIPT_UNKNOWN_XL')).toBe('RECEIPT_UNKNOWN_XL');
  });
});

describe('makeBadgeFact', () => {
  it('returns active text and active=true when flag is true', () => {
    const fact = makeBadgeFact('Enpresyon', true, 'Aktif', 'Inaktif');
    expect(fact.value).toBe('Aktif');
    expect(fact.active).toBe(true);
    expect(fact.badge).toBe(true);
  });

  it('returns inactive text and active=false when flag is false', () => {
    const fact = makeBadgeFact('SMS', false, 'Aktif', 'Inaktif');
    expect(fact.value).toBe('Inaktif');
    expect(fact.active).toBe(false);
    expect(fact.badge).toBe(true);
  });
});

describe('makeTextFact', () => {
  it('returns a non-badge fact with the given label and value', () => {
    const fact = makeTextFact('Papye', '80 mm');
    expect(fact.label).toBe('Papye');
    expect(fact.value).toBe('80 mm');
    expect(fact.badge).toBe(false);
    expect(fact.active).toBe(false);
  });
});
