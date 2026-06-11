import { PUBLIC_RULE_GAMES } from './public-rules.data';
import type { PublicRuleGameId } from './public-rules.model';

export const SESSION_GAME_KEY = 'tch.rules.game';
export const SESSION_AMOUNT_KEY = 'tch.rules.amount';

export function sessionReadGame(): PublicRuleGameId {
  try {
    const v = sessionStorage.getItem(SESSION_GAME_KEY);
    return v && PUBLIC_RULE_GAMES.some(g => g.id === v) ? (v as PublicRuleGameId) : 'borlette';
  } catch {
    return 'borlette';
  }
}

export function sessionReadAmount(): number {
  try {
    const v = parseInt(sessionStorage.getItem(SESSION_AMOUNT_KEY) ?? '', 10);
    return Number.isFinite(v) && v > 0 ? v : 100;
  } catch {
    return 100;
  }
}

export function initialOption(): string {
  const game = PUBLIC_RULE_GAMES.find(g => g.id === sessionReadGame());
  return game?.betOptions[0]?.id ?? '';
}

export function sessionWrite(key: string, value: string): void {
  try {
    sessionStorage.setItem(key, value);
  } catch {
    /* private browsing */
  }
}
