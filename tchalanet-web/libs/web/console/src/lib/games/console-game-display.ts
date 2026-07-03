const GAME_LABELS: Record<string, string> = {
  BORLETTE: 'Borlette',
  HT_BOLET: 'Borlette',
  HT_BORLETTE: 'Borlette',
  MARYAJ: 'Maryaj',
  HT_MARYAJ: 'Maryaj',
  HT_MARYAJ_GRATUIT: 'Maryaj gratis',
  LOTO3: 'Loto 3',
  HT_LOTO3: 'Loto 3',
  LOTO4: 'Loto 4',
  HT_LOTO4: 'Loto 4',
  LOTO5: 'Loto 5',
  HT_LOTO5: 'Loto 5',
};

const GAME_LOGO_TEXT: Record<string, string> = {
  BORLETTE: 'Bo',
  HT_BOLET: 'Bo',
  HT_BORLETTE: 'Bo',
  MARYAJ: 'Ma',
  HT_MARYAJ: 'Ma',
  HT_MARYAJ_GRATUIT: 'MG',
  LOTO3: 'L3',
  HT_LOTO3: 'L3',
  LOTO4: 'L4',
  HT_LOTO4: 'L4',
  LOTO5: 'L5',
  HT_LOTO5: 'L5',
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

export function consoleGameName(gameCode: string, displayName?: string | null): string {
  const explicit = displayName?.trim();
  if (explicit && explicit !== gameCode && !isTechnicalCode(explicit)) return explicit;
  return GAME_LABELS[gameCode] ?? readableCode(gameCode);
}

export function consoleGameLogoText(gameCode: string, displayName?: string | null): string {
  return GAME_LOGO_TEXT[gameCode] ?? initials(consoleGameName(gameCode, displayName));
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
