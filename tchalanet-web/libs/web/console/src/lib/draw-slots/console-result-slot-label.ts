export interface ConsoleResultSlotLabelSource {
  readonly slotKey: string;
  readonly provider?: unknown;
  readonly drawTime?: unknown;
  readonly timezone?: unknown;
  readonly label?: unknown;
  readonly labelKey?: unknown;
}

export function consoleResultSlotLabel(slot: ConsoleResultSlotLabelSource): string {
  const label = clean(slot.label);
  const provider = clean(slot.provider)?.toUpperCase();
  const time = clean(slot.drawTime)?.slice(0, 5);

  const parts = [
    label,
    provider && provider !== label?.toUpperCase() ? provider : null,
    time,
  ].filter((part): part is string => !!part);

  return parts.length ? `${parts.join(' · ')} (${slot.slotKey})` : slot.slotKey;
}

function clean(value: unknown): string | null {
  const trimmed = typeof value === 'string' ? value.trim() : '';
  return trimmed || null;
}
