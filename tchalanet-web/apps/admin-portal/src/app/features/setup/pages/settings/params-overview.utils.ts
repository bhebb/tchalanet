import type { SettingsSummaryFact } from './settings-summary-card.component';

export function paperSizeLabel(size: string | null): string {
  const map: Record<string, string> = { RECEIPT_58MM: '58 mm', RECEIPT_80MM: '80 mm', A4: 'A4' };
  return size ? (map[size] ?? size) : '80 mm';
}

export function makeBadgeFact(
  label: string,
  active: boolean,
  activeText: string,
  inactiveText: string,
): SettingsSummaryFact {
  return { label, value: active ? activeText : inactiveText, badge: true, active };
}

export function makeTextFact(label: string, value: string): SettingsSummaryFact {
  return { label, value, badge: false, active: false };
}
