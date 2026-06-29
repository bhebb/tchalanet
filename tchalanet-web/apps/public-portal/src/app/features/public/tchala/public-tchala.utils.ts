import type { TchalaEntry } from './public-tchala.service';
import type { TchalaDisplayEntry } from './public-tchala.model';

export const PAGE_SIZE = 24;

export function apiEntryToDisplay(e: TchalaEntry): TchalaDisplayEntry {
  return {
    id: e.id,
    icon: 'auto_stories',
    term: e.dream,
    description: e.source === 'IMPORT' ? '' : (e.note ?? ''),
    numbers: e.numbers.map(n => String(n).padStart(2, '0')),
  };
}
