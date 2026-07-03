import { describe, expect, it } from 'vitest';

import { dateParam, enumParam, numberParam, textParam } from './url-params';

describe('numberParam', () => {
  it('parse un entier ≥ 0', () => {
    expect(numberParam('3', 0)).toBe(3);
    expect(numberParam('0', 5)).toBe(0);
  });

  it('retombe sur le fallback pour absent/vide/invalide/négatif', () => {
    expect(numberParam(null, 20)).toBe(20);
    expect(numberParam(undefined, 20)).toBe(20);
    expect(numberParam('', 20)).toBe(20);
    expect(numberParam('abc', 20)).toBe(20);
    expect(numberParam('-1', 20)).toBe(20);
    expect(numberParam('1.5', 20)).toBe(20);
  });
});

describe('dateParam', () => {
  it('accepte YYYY-MM-DD', () => {
    expect(dateParam('2026-07-03')).toBe('2026-07-03');
  });

  it('rejette tout autre format', () => {
    expect(dateParam(null)).toBe('');
    expect(dateParam('2026-7-3')).toBe('');
    expect(dateParam('03/07/2026')).toBe('');
    expect(dateParam('garbage')).toBe('');
  });
});

describe('textParam', () => {
  it('trime et gère l’absence', () => {
    expect(textParam('  abc  ')).toBe('abc');
    expect(textParam(null)).toBe('');
    expect(textParam(undefined)).toBe('');
  });
});

describe('enumParam', () => {
  const STATUSES = ['all', 'OPEN', 'CLOSED'] as const;

  it('conserve une valeur autorisée', () => {
    expect(enumParam('OPEN', STATUSES, 'all')).toBe('OPEN');
  });

  it('retombe sur le fallback hors-ensemble/absent', () => {
    expect(enumParam('BOGUS', STATUSES, 'all')).toBe('all');
    expect(enumParam(null, STATUSES, 'all')).toBe('all');
    expect(enumParam(undefined, STATUSES, 'all')).toBe('all');
  });
});
