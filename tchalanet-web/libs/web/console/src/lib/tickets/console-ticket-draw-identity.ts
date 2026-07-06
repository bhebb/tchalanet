import {
  ConsoleDrawIdentityInput,
  consoleDrawIdentity,
} from '../draw-slots/console-draw-identities';
import { ConsoleDrawSlotIdentity } from '../draw-slots/console-draw-slot-identity.models';

export interface ConsoleTicketDrawIdentityInput {
  readonly channelCode?: string | null;
  readonly channelLabel?: string | null;
  readonly resultSlotKey?: string | null;
  readonly drawDateLabel?: string | null;
  readonly scheduledAt?: string | null;
  readonly fallbackLabel?: string | null;
}

export interface ConsoleTicketDrawIdentityView {
  readonly identity: ConsoleDrawSlotIdentity;
  readonly receiptLabel: string;
  readonly receiptDateTimeLabel: string;
}

export function consoleTicketDrawIdentity(
  input: ConsoleTicketDrawIdentityInput,
): ConsoleTicketDrawIdentityView {
  const scheduledTimeLabel = timeLabel(input.scheduledAt);
  const identityInput: ConsoleDrawIdentityInput = {
    channelCode: input.channelCode,
    channelName: input.channelLabel,
    slotKey: input.resultSlotKey ?? input.channelCode,
    officialDateLabel: input.drawDateLabel,
    officialTimeLabel: scheduledTimeLabel,
    fallbackTitle: input.fallbackLabel ?? input.channelLabel ?? input.channelCode,
  };
  const identity = consoleDrawIdentity(identityInput);
  const receiptLabel =
    identity.channelName ??
    identity.channelShortName ??
    input.fallbackLabel ??
    input.channelLabel ??
    input.channelCode ??
    'Tirage';

  return {
    identity,
    receiptLabel,
    receiptDateTimeLabel: [input.drawDateLabel, scheduledTimeLabel].filter(Boolean).join(' · '),
  };
}

function timeLabel(value: string | null | undefined): string | null {
  if (!value) return null;
  const parsed = Date.parse(value);
  if (!Number.isFinite(parsed)) {
    return value.length >= 5 ? value.substring(0, 5) : value;
  }
  return new Intl.DateTimeFormat('fr-CA', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(new Date(parsed));
}
