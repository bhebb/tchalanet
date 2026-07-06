/**
 * Computes, for a drawn Pick 3 / Pick 4 number, the winning number combinations per **Tchalanet
 * supported** play option (Exact, Permuté with computed N-way, Deux premiers, Deux derniers).
 *
 * This formats result-derived data — it is NOT a static provider matrix, and it exposes only the
 * options Tchalanet actually sells/settles (see `SETTLEMENT_VARIANTS.md`). Unsupported provider
 * options (3-way straight/box, wheel, fireball, …) are intentionally absent.
 */

export interface DrawCombinationRow {
  /** Commercial play-type label (+ computed N-way for Permuté). */
  readonly playType: string;
  /** How the option wins: exact order or any order. */
  readonly winWith: 'exact' | 'any';
  /** Winning numbers/patterns for this option. */
  readonly winningNumbers: readonly string[];
}

export interface DrawCombinationFact {
  readonly label: string;
  readonly value: string | null;
  readonly tone?: 'default' | 'missing';
}

export interface DrawCombinationGameSection {
  readonly gameLabel: string;
  readonly resultNumbers: readonly string[];
  readonly rows: readonly DrawCombinationRow[];
}

export interface DrawCombinationResultInput {
  readonly numbers?: ReadonlyArray<number | string> | null;
  readonly sourceResult?: Record<string, unknown> | null;
  readonly haitiResult?: Record<string, unknown> | null;
  readonly rawPayload?: Record<string, unknown> | null;
}

/** Extracts the drawn digits from a raw numbers array (e.g. `[1, 8, 2]` → `"182"`). */
export function drawDigitsFromNumbers(numbers: ReadonlyArray<number | string> | null | undefined): string {
  if (!numbers) return '';
  return numbers.map(value => String(value).trim()).join('').replace(/\D/g, '');
}

/**
 * Winning combinations for a drawn Pick 3 (3 digits) or Pick 4 (4 digits). Returns an empty array
 * when the input is not a supported 3- or 4-digit number.
 */
export function drawCombinationRows(digits: string | null | undefined): DrawCombinationRow[] {
  const clean = (digits ?? '').replace(/\D/g, '');
  if (clean.length === 3) return pick3Rows(clean);
  if (clean.length === 4) return pick4Rows(clean);
  return [];
}

/**
 * Builds supported combination rows from an actual draw-result payload.
 *
 * Result APIs can expose multiple values in `numbers` (lot1, lot2, lot3, lot4, pick3, pick4), so
 * this must not concatenate the whole array. It follows the same fact preference used by
 * settlement: explicit pick values first, then compatible Haiti lots, then single result values.
 */
export function drawCombinationRowsFromResult(input: DrawCombinationResultInput | null | undefined): DrawCombinationRow[] {
  return drawCombinationGameSectionsFromResult(input).flatMap(section =>
    section.rows.map(row => ({ ...row, playType: `${section.gameLabel} · ${row.playType}` })),
  );
}

export function drawCombinationGameSectionsFromResult(
  input: DrawCombinationResultInput | null | undefined,
): DrawCombinationGameSection[] {
  if (!input) return [];

  const facts = drawCombinationFacts(input);
  const lot1_2d = last2(facts.lot1);
  const lot2_2d = last2(facts.lot2);
  const lot3_2d = last2(facts.lot3);

  return [
    section('Bòlèt', [lot1_2d, lot2_2d, lot3_2d], boletRows(lot1_2d, lot2_2d, lot3_2d)),
    section('Maryaj', [lot1_2d, lot2_2d], maryajRows(lot1_2d, lot2_2d)),
    section('Loto 3', [facts.pick3], pick3RowsOrEmpty(facts.pick3)),
    section('Loto 4', [facts.pick4], pick4RowsOrEmpty(facts.pick4)),
    section('Loto 5', [facts.lot1, facts.lot2, facts.lot3], loto5Rows(facts.lot1, facts.lot2, facts.lot3)),
  ].filter(item => item.rows.length > 0);
}

export function drawCombinationFactsFromResult(input: DrawCombinationResultInput | null | undefined): DrawCombinationFact[] {
  const facts = drawCombinationFacts(input);
  const lot1_2d = last2(facts.lot1);
  const lot2_2d = last2(facts.lot2);
  const lot3_2d = last2(facts.lot3);
  return [
    fact('1er lot', facts.lot1),
    fact('2e lot', facts.lot2),
    fact('3e lot', facts.lot3),
    fact('Maryaj', lot1_2d && lot2_2d ? `${lot1_2d} × ${lot2_2d}` : null),
    fact('Loto 3', facts.pick3),
    fact('Loto 4', facts.pick4),
    fact('Loto 5', facts.lot1 && facts.lot2 && facts.lot3 ? `${facts.lot1} / ${facts.lot2} / ${facts.lot3}` : null),
  ];
}

function pick3Rows(digits: string): DrawCombinationRow[] {
  const perms = uniquePermutations(digits);
  const rows: DrawCombinationRow[] = [
    { playType: 'Exact', winWith: 'exact', winningNumbers: [digits] },
  ];
  if (perms.length > 1) {
    rows.push({ playType: `Permuté · ${perms.length}-way`, winWith: 'any', winningNumbers: perms });
  }
  return rows;
}

function pick4Rows(digits: string): DrawCombinationRow[] {
  const perms = uniquePermutations(digits);
  const front = digits.slice(0, 2);
  const back = digits.slice(2, 4);
  const rows: DrawCombinationRow[] = [
    { playType: 'Exact', winWith: 'exact', winningNumbers: [digits] },
  ];
  if (perms.length > 1) {
    rows.push({ playType: `Permuté · ${perms.length}-way`, winWith: 'any', winningNumbers: perms });
  }
  rows.push(
    { playType: 'Deux premiers', winWith: 'exact', winningNumbers: [`${front} ••`] },
    { playType: 'Deux derniers', winWith: 'exact', winningNumbers: [`•• ${back}`] },
  );
  return rows;
}

function boletRows(lot1: string | null, lot2: string | null, lot3: string | null): DrawCombinationRow[] {
  const rows: Array<DrawCombinationRow | null> = [
    lot1 ? { playType: 'Bòlèt · 1er lot', winWith: 'exact' as const, winningNumbers: [lot1] } : null,
    lot2 ? { playType: 'Bòlèt · 2e lot', winWith: 'exact' as const, winningNumbers: [lot2] } : null,
    lot3 ? { playType: 'Bòlèt · 3e lot', winWith: 'exact' as const, winningNumbers: [lot3] } : null,
  ];
  return rows.filter((row): row is DrawCombinationRow => row !== null);
}

function maryajRows(lot1: string | null, lot2: string | null): DrawCombinationRow[] {
  if (!lot1 || !lot2) return [];
  const reverse = lot1 === lot2 ? [lot1] : [`${lot1}-${lot2}`, `${lot2}-${lot1}`];
  return [
    { playType: 'Maryaj · ordre exact', winWith: 'exact', winningNumbers: [`${lot1}-${lot2}`] },
    { playType: 'Maryaj · revers/double', winWith: 'any', winningNumbers: reverse },
  ];
}

function loto5Rows(lot1: string | null, lot2: string | null, lot3: string | null): DrawCombinationRow[] {
  const rows: Array<DrawCombinationRow | null> = [
    lot1 && lot2 ? { playType: 'Loto 5 · 1er + 2e lot', winWith: 'exact' as const, winningNumbers: [`${lot1} / ${lot2}`] } : null,
    lot1 && lot3 ? { playType: 'Loto 5 · 1er + 3e lot', winWith: 'exact' as const, winningNumbers: [`${lot1} / ${lot3}`] } : null,
    lot1 && lot2 && lot3
      ? { playType: 'Loto 5 · mixte 1er/2e/3e lot', winWith: 'any' as const, winningNumbers: [lot1, lot2, lot3] }
      : null,
  ];
  return rows.filter((row): row is DrawCombinationRow => row !== null);
}

/** Distinct permutations of a digit string, sorted for stable display. */
export function uniquePermutations(digits: string): string[] {
  const out = new Set<string>();
  permute(digits.split(''), 0, out);
  return [...out].sort();
}

function permute(chars: string[], start: number, out: Set<string>): void {
  if (start === chars.length - 1) {
    out.add(chars.join(''));
    return;
  }
  for (let i = start; i < chars.length; i++) {
    swap(chars, start, i);
    permute(chars, start + 1, out);
    swap(chars, start, i);
  }
}

function swap(chars: string[], a: number, b: number): void {
  const tmp = chars[a];
  chars[a] = chars[b];
  chars[b] = tmp;
}

function pick3RowsOrEmpty(digits: string | null): DrawCombinationRow[] {
  return digits ? pick3Rows(digits) : [];
}

function pick4RowsOrEmpty(digits: string | null): DrawCombinationRow[] {
  return digits ? pick4Rows(digits) : [];
}

function drawCombinationFacts(input: DrawCombinationResultInput | null | undefined): {
  lot1: string | null;
  lot2: string | null;
  lot3: string | null;
  pick3: string | null;
  pick4: string | null;
} {
  if (!input) return { lot1: null, lot2: null, lot3: null, pick3: null, pick4: null };
  const numberDigits = digitsFromNumbers(input.numbers);
  const lot1 = firstDigitsOfLength(3, [valueFromPayloads(input, 'lot1')]);
  const lot2 = firstDigitsOfLength(2, [valueFromPayloads(input, 'lot2')]);
  const lot3 = firstDigitsOfLength(2, [valueFromPayloads(input, 'lot3')]);
  const lot4 = firstDigitsOfLength(4, [valueFromPayloads(input, 'lot4'), valueFromPayloads(input, 'pick4'), ...numberDigits]);
  return {
    lot1,
    lot2,
    lot3,
    pick3: firstDigitsOfLength(3, [valueFromPayloads(input, 'pick3'), lot1, valueFromPayloads(input, 'lot4'), ...numberDigits]),
    pick4: firstDigitsOfLength(4, [valueFromPayloads(input, 'pick4'), lot4, lot1, ...numberDigits]),
  };
}

function firstDigitsOfLength(length: 2 | 3 | 4, values: Array<unknown>): string | null {
  for (const value of values) {
    const digits = String(value ?? '').replace(/\D/g, '');
    if (digits.length === length) return digits;
  }
  return null;
}

function digitsFromNumbers(numbers: ReadonlyArray<number | string> | null | undefined): string[] {
  if (!numbers) return [];
  return numbers.map(value => String(value).replace(/\D/g, '')).filter(Boolean);
}

function valueFromPayloads(input: DrawCombinationResultInput, key: string): unknown {
  return input.haitiResult?.[key] ?? input.sourceResult?.[key] ?? input.rawPayload?.[key] ?? null;
}

function last2(value: string | null): string | null {
  return value && value.length >= 2 ? value.slice(-2) : null;
}

function fact(label: string, value: string | null): DrawCombinationFact {
  return { label, value, tone: value ? 'default' : 'missing' };
}

function section(
  gameLabel: string,
  numbers: readonly (string | null)[],
  rows: readonly DrawCombinationRow[],
): DrawCombinationGameSection {
  return {
    gameLabel,
    resultNumbers: numbers.filter((number): number is string => Boolean(number)),
    rows,
  };
}
