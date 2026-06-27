export interface ResultSlotLabelSource {
  readonly slotKey: string;
  readonly provider?: string | null;
  readonly drawTime?: string | null;
  readonly timezone?: string | null;
  readonly label?: string | null;
  readonly labelKey?: string | null;
}

export function resultSlotLabel(slot: ResultSlotLabelSource): string {
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

function clean(value: string | null | undefined): string | null {
  const trimmed = value?.trim();
  return trimmed ? trimmed : null;
}
