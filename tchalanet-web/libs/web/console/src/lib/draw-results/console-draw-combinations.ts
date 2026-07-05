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
  if (!input) return [];

  const pick3 = firstDigitsOfLength(3, [
    valueFromPayloads(input, 'pick3'),
    valueFromPayloads(input, 'lot1'),
    valueFromPayloads(input, 'lot4'),
    ...digitsFromNumbers(input.numbers),
  ]);
  const pick4 = firstDigitsOfLength(4, [
    valueFromPayloads(input, 'pick4'),
    valueFromPayloads(input, 'lot4'),
    valueFromPayloads(input, 'lot1'),
    ...digitsFromNumbers(input.numbers),
  ]);

  return [
    ...prefixRows('Loto 3', pick3RowsOrEmpty(pick3)),
    ...prefixRows('Loto 4', pick4RowsOrEmpty(pick4)),
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

function prefixRows(prefix: string, rows: DrawCombinationRow[]): DrawCombinationRow[] {
  return rows.map(row => ({ ...row, playType: `${prefix} · ${row.playType}` }));
}

function firstDigitsOfLength(length: 3 | 4, values: Array<unknown>): string | null {
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
