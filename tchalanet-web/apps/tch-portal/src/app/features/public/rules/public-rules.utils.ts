import type { PublicTchalaEntry } from './public-rules.model';

export function filterTchalaEntries(
  entries: readonly PublicTchalaEntry[],
  query: string,
): readonly PublicTchalaEntry[] {
  const normalized = normalizeQuery(query);
  if (!normalized) return entries;
  return entries.filter(entry =>
    entry.keywords.some(keyword => normalizeQuery(keyword).includes(normalized)),
  );
}

function normalizeQuery(value: string): string {
  return value
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
    .trim()
    .toLowerCase();
}
