import { TCH_GAME_ASSET_BASE_PATH } from '@tch/shared-assets';

const GAME_LABELS: Record<string, string> = {
  BOLET: 'Bòlèt',
  BORLETTE: 'Bòlèt',
  HT_BOLET: 'Bòlèt',
  HT_BORLETTE: 'Bòlèt',
  MARYAJ: 'Maryaj',
  HT_MARYAJ: 'Maryaj',
  MARYAJ_GRATIS: 'Maryaj gratis',
  MARYAJ_GRATUIT: 'Maryaj gratis',
  HT_MARYAJ_GRATIS: 'Maryaj gratis',
  HT_MARYAJ_GRATUIT: 'Maryaj gratis',
  LOTO3: 'Loto 3',
  LOTO_3: 'Loto 3',
  HT_LOTO3: 'Loto 3',
  HT_LOTO_3: 'Loto 3',
  LOTO4: 'Loto 4',
  LOTO_4: 'Loto 4',
  HT_LOTO4: 'Loto 4',
  HT_LOTO_4: 'Loto 4',
  LOTO5: 'Loto 5',
  LOTO_5: 'Loto 5',
  HT_LOTO5: 'Loto 5',
  HT_LOTO_5: 'Loto 5',
};

const GAME_LOGO_TEXT: Record<string, string> = {
  BOLET: 'Bo',
  BORLETTE: 'Bo',
  HT_BOLET: 'Bo',
  HT_BORLETTE: 'Bo',
  MARYAJ: 'Ma',
  HT_MARYAJ: 'Ma',
  MARYAJ_GRATIS: 'MG',
  MARYAJ_GRATUIT: 'MG',
  HT_MARYAJ_GRATIS: 'MG',
  HT_MARYAJ_GRATUIT: 'MG',
  LOTO3: 'L3',
  LOTO_3: 'L3',
  HT_LOTO3: 'L3',
  HT_LOTO_3: 'L3',
  LOTO4: 'L4',
  LOTO_4: 'L4',
  HT_LOTO4: 'L4',
  HT_LOTO_4: 'L4',
  LOTO5: 'L5',
  LOTO_5: 'L5',
  HT_LOTO5: 'L5',
  HT_LOTO_5: 'L5',
};

const GAME_LOGO_ASSETS: Record<string, string> = {
  BOLET: 'ht-bolet.svg',
  BORLETTE: 'ht-bolet.svg',
  HT_BOLET: 'ht-bolet.svg',
  HT_BORLETTE: 'ht-bolet.svg',
  MARYAJ: 'ht-maryaj.svg',
  HT_MARYAJ: 'ht-maryaj.svg',
  MARYAJ_GRATUIT: 'ht-maryaj-gratis.svg',
  MARYAJ_GRATIS: 'ht-maryaj-gratis.svg',
  HT_MARYAJ_GRATUIT: 'ht-maryaj-gratis.svg',
  HT_MARYAJ_GRATIS: 'ht-maryaj-gratis.svg',
  LOTO3: 'ht-loto-3.svg',
  LOTO_3: 'ht-loto-3.svg',
  HT_LOTO3: 'ht-loto-3.svg',
  HT_LOTO_3: 'ht-loto-3.svg',
  LOTO4: 'ht-loto-4.svg',
  LOTO_4: 'ht-loto-4.svg',
  HT_LOTO4: 'ht-loto-4.svg',
  HT_LOTO_4: 'ht-loto-4.svg',
  LOTO5: 'ht-loto-5.svg',
  LOTO_5: 'ht-loto-5.svg',
  HT_LOTO5: 'ht-loto-5.svg',
  HT_LOTO_5: 'ht-loto-5.svg',
};

const BET_TYPE_LABELS: Record<string, string> = {
  STRAIGHT: 'Direct',
  BOX: 'Permuté',
  FRONT_PAIR: 'Deux premiers',
  BACK_PAIR: 'Deux derniers',
  LOTTO: 'Loto',
  COMBO: 'Combiné',
  MARIAGE: 'Maryaj',
  MARYAJ: 'Maryaj',
  MATCH_1_2D: '1er lot',
  MATCH_2_2D: '2e lot',
  MATCH_3_2D: '3e lot',
  PATTERN2X2: 'Maryaj 2x2',
  PATTERN2_2: 'Maryaj 2x2',
  PATTERN2STAR2: 'Maryaj 2x2',
  PATTERN_2X2: 'Maryaj 2x2',
  MARRIAGE_2D2D: 'Maryaj 2x2',
  LOTTO3_3D: 'Loto 3',
  LOTTO4_PATTERN: 'Loto 4',
  LOTTO5_PATTERN: 'Loto 5',
};

const BET_OPTION_LABELS: Record<string, Record<number, string>> = {
  MARRIAGE_2D2D: {
    1: 'Ordre exact',
    2: 'Revers / double',
  },
  PATTERN2X2: {
    1: 'Ordre exact',
    2: 'Revers / double',
  },
  LOTTO3_3D: {
    1: 'Exact',
    2: 'Permuté',
  },
  LOTTO4_PATTERN: {
    1: 'Exact',
    2: 'Permuté',
    3: 'Deux premiers',
    4: 'Deux derniers',
  },
  LOTTO5_PATTERN: {
    1: '1er + 2e lot',
    2: '1er + 3e lot',
    3: 'Mixte 1er/2e/3e lot',
  },
};

/**
 * Admin/support labels for computed settlement variants. Mirrors the backend
 * `SettlementVariant.adminLabel()` and is used only as a fallback when the backend does not
 * provide `adminLabel`. Technical variants are shown in console/admin/support contexts only —
 * never on the seller terminal or the customer receipt.
 */
const SETTLEMENT_VARIANT_LABELS: Record<string, string> = {
  MATCH_1_2D: '1er lot',
  MATCH_2_2D: '2e lot',
  MATCH_3_2D: '3e lot',
  MARRIAGE_EXACT_ORDER: 'Maryaj · ordre exact',
  MARRIAGE_REVERSE_ALLOWED: 'Maryaj · revers/double',
  LOTTO3_STRAIGHT: 'Exact',
  LOTTO3_BOX_3_WAY: 'Permuté · 3-way',
  LOTTO3_BOX_6_WAY: 'Permuté · 6-way',
  LOTTO4_STRAIGHT: 'Exact',
  LOTTO4_BOX_4_WAY: 'Permuté · 4-way',
  LOTTO4_BOX_6_WAY: 'Permuté · 6-way',
  LOTTO4_BOX_12_WAY: 'Permuté · 12-way',
  LOTTO4_BOX_24_WAY: 'Permuté · 24-way',
  LOTTO4_FRONT_PAIR: 'Deux premiers',
  LOTTO4_BACK_PAIR: 'Deux derniers',
  LOTTO5_LOT1_LOT2: '1er + 2e lot',
  LOTTO5_LOT1_LOT3: '1er + 3e lot',
  LOTTO5_MIXED_1_2_3: 'Mixte 1er/2e/3e lot',
};

/** One settlement line as returned by the backend admin endpoint. */
export interface ConsoleSettlementLine {
  readonly lineNo: number;
  readonly gameCode: string;
  readonly betType: string;
  readonly betOption: number | null;
  readonly selection: string | null;
  readonly commercialLabel: string | null;
  readonly variant: string | null;
  readonly adminLabel: string | null;
  readonly resolved: boolean;
  readonly error: string | null;
}

/** A display row for the console "Combinaisons & règles" tab. */
export interface ConsoleBetVariationRow {
  readonly lineNo: number;
  readonly gameLabel: string;
  readonly selection: string | null;
  /** Commercial label shown by default (customer-safe). */
  readonly commercialLabel: string;
  /** Technical/admin label (may expose 24-way etc.). Null when unresolved. */
  readonly variantLabel: string | null;
  readonly resolved: boolean;
  readonly error: string | null;
}

export interface ConsoleGameIdentity {
  readonly code: string;
  readonly name: string;
  readonly logoText: string;
  readonly logoUrl: string | null;
}

/**
 * Admin/support label for a computed settlement variant. Prefers the backend-provided
 * `adminLabel`, falls back to the local map, then to a readable code.
 */
export function consoleSettlementVariantLabel(
  variant: string | null | undefined,
  adminLabel?: string | null,
): string | null {
  const provided = adminLabel?.trim();
  if (provided) return provided;
  if (!variant) return null;
  return SETTLEMENT_VARIANT_LABELS[variant] ?? readableCode(variant);
}

/**
 * Maps backend settlement lines to console display rows. Formats backend/result-derived data — it
 * is NOT a static provider-doc matrix. Unresolved lines keep their commercial label and carry the
 * error, so support can spot legacy/corrupted lines without the row breaking.
 */
export function consoleBetVariationRows(
  lines: readonly ConsoleSettlementLine[] | null | undefined,
): ConsoleBetVariationRow[] {
  if (!lines) return [];
  return lines.map(line => ({
    lineNo: line.lineNo,
    gameLabel: consoleGameName(line.gameCode),
    selection: line.selection,
    commercialLabel: line.commercialLabel?.trim() || consoleBetLabel(line.betType, line.betOption),
    variantLabel: line.resolved
      ? consoleSettlementVariantLabel(line.variant, line.adminLabel)
      : null,
    resolved: line.resolved,
    error: line.error,
  }));
}

export function consoleGameName(gameCode: string, displayName?: string | null): string {
  const explicit = displayName?.trim();
  if (explicit && explicit !== gameCode && !isTechnicalCode(explicit)) return explicit;
  return GAME_LABELS[normalizeGameCode(gameCode)] ?? readableCode(gameCode);
}

export function consoleGameLogoText(gameCode: string, displayName?: string | null): string {
  return GAME_LOGO_TEXT[normalizeGameCode(gameCode)] ?? initials(consoleGameName(gameCode, displayName));
}

export function consoleGameLogoUrl(gameCode: string): string | null {
  const asset = GAME_LOGO_ASSETS[normalizeGameCode(gameCode)];
  return asset ? `${TCH_GAME_ASSET_BASE_PATH}/${asset}` : null;
}

export function consoleGameIdentity(gameCode: string, displayName?: string | null): ConsoleGameIdentity {
  return {
    code: gameCode,
    name: consoleGameName(gameCode, displayName),
    logoText: consoleGameLogoText(gameCode, displayName),
    logoUrl: consoleGameLogoUrl(gameCode),
  };
}

export function consoleBetTypeLabel(betType: string): string {
  return BET_TYPE_LABELS[normalizeBetType(betType)] ?? readableCode(betType);
}

export function consoleBetOptionLabel(betType: string, betOption: number | string | null): string | null {
  if (betOption == null) return null;
  const option = typeof betOption === 'number' ? betOption : Number(betOption);
  if (!Number.isFinite(option)) return String(betOption);
  return BET_OPTION_LABELS[normalizeBetType(betType)]?.[option] ?? `Option ${option}`;
}

export function consoleBetLabel(betType: string, betOption: number | string | null): string {
  return consoleBetOptionLabel(betType, betOption) ?? consoleBetTypeLabel(betType);
}

function normalizeBetType(value: string): string {
  return value
    .trim()
    .toUpperCase()
    .replace(/\s+/g, '_')
    .replace(/\*/g, 'STAR')
    .replace(/-/g, '_');
}

function normalizeGameCode(value: string): string {
  return value
    .trim()
    .toUpperCase()
    .replace(/\s+/g, '_')
    .replace(/-/g, '_');
}

function readableCode(code: string): string {
  return code
    .replace(/^HT_/, '')
    .replace(/_/g, ' ')
    .replace(/\bLOTO(\d)\b/g, 'Loto $1')
    .replace(/\b\w/g, char => char.toUpperCase());
}

function isTechnicalCode(value: string): boolean {
  return /^[A-Z0-9_*:-]+$/.test(value);
}

function initials(value: string): string {
  const parts = value.trim().split(/\s+/).filter(Boolean);
  const text = parts.length >= 2
    ? `${parts[0][0] ?? ''}${parts[1][0] ?? ''}`
    : value.slice(0, 2);
  return text.toUpperCase();
}
