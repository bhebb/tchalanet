import { AdminStatusTone } from '@tch/ui/console';

import {
  ConsoleDrawIdentityInput,
  consoleDrawIdentity,
} from '../draw-slots/console-draw-identities';
import { ConsoleDrawSlotIdentity } from '../draw-slots/console-draw-slot-identity.models';
import { ConsoleFact } from '../entity-detail/console-entity-detail.models';
import { ConsoleRowAction } from '../draw-results/console-draw-results-table.models';
import {
  ConsoleDrawDetailAside,
  ConsoleDrawDetailResult,
  ConsoleDrawDetailSection,
  ConsoleDrawDetailView,
} from './console-draw-detail.models';
import { ConsoleDrawRow } from './console-draws-table.models';

export interface ConsoleDrawRowViewModelInput {
  readonly id: string;
  readonly identity?: ConsoleDrawSlotIdentity;
  readonly identityInput?: ConsoleDrawIdentityInput;
  readonly groupLabel?: string;
  readonly title?: string | null;
  readonly subtitle?: string | null;
  readonly meta?: string | null;
  readonly scheduledDateLabel?: string | null;
  readonly scheduledTimeLabel?: string | null;
  readonly countdownLabel?: string | null;
  readonly countdownTone?: 'default' | 'warning';
  readonly statusLabel: string;
  readonly statusTone: AdminStatusTone;
  readonly resultLabel?: string | null;
  readonly resultTone?: AdminStatusTone;
  readonly resultNumbers?: readonly string[];
  readonly resultHint?: string | null;
  readonly modeLabel?: string | null;
  readonly publicationLabel?: string | null;
  readonly publicationTone?: AdminStatusTone;
  readonly actions?: readonly ConsoleRowAction[];
  readonly pending?: boolean;
}

export interface ConsoleDrawDetailViewModelInput {
  readonly identity?: ConsoleDrawSlotIdentity;
  readonly identityInput?: ConsoleDrawIdentityInput;
  readonly title?: string | null;
  readonly description?: string | null;
  readonly meta?: readonly string[];
  readonly statusLabel?: string | null;
  readonly statusTone?: AdminStatusTone;
  readonly overviewFacts?: readonly ConsoleFact[];
  readonly result?: ConsoleDrawDetailResult | null;
  readonly sections?: readonly ConsoleDrawDetailSection[];
  readonly aside?: ConsoleDrawDetailAside | null;
}

export function consoleDrawRowViewModel(input: ConsoleDrawRowViewModelInput): ConsoleDrawRow {
  const identity = resolveDrawIdentity(input.identity, input.identityInput);
  const title = firstText(input.title, identity.channelShortName, identity.channelName, identity.slotLabel, identity.slotKey, 'Tirage');
  const subtitle = firstText(input.subtitle, identity.slotLabel, identity.slotKey, identity.channelCode);
  const meta = firstText(input.meta, identity.providerName, identity.providerCode);

  return {
    id: input.id,
    groupLabel: input.groupLabel,
    title,
    subtitle,
    meta,
    logoUrl: identity.providerLogoUrl,
    logoAlt: firstText(identity.providerName, identity.providerCode, title),
    logoText: identity.logoText ?? undefined,
    identity,
    scheduledDateLabel: input.scheduledDateLabel ?? undefined,
    scheduledTimeLabel: input.scheduledTimeLabel ?? undefined,
    countdownLabel: input.countdownLabel,
    countdownTone: input.countdownTone,
    statusLabel: input.statusLabel,
    statusTone: input.statusTone,
    resultLabel: input.resultLabel ?? undefined,
    resultTone: input.resultTone,
    resultNumbers: input.resultNumbers ?? [],
    resultHint: input.resultHint ?? undefined,
    modeLabel: input.modeLabel ?? undefined,
    publicationLabel: input.publicationLabel ?? undefined,
    publicationTone: input.publicationTone,
    actions: input.actions,
    pending: input.pending,
  };
}

export function consoleDrawDetailViewModel(input: ConsoleDrawDetailViewModelInput): ConsoleDrawDetailView {
  const identity = resolveDrawIdentity(input.identity, input.identityInput);
  const title = firstText(input.title, identity.channelName, identity.channelShortName, identity.slotLabel, identity.slotKey, 'Détail du tirage');

  return {
    identity,
    title,
    description: input.description ?? undefined,
    meta: input.meta,
    statusLabel: input.statusLabel ?? undefined,
    statusTone: input.statusTone,
    overviewFacts: input.overviewFacts,
    result: input.result,
    sections: input.sections,
    aside: input.aside,
  };
}

function resolveDrawIdentity(
  identity: ConsoleDrawSlotIdentity | null | undefined,
  input: ConsoleDrawIdentityInput | null | undefined,
): ConsoleDrawSlotIdentity {
  return identity ?? consoleDrawIdentity(input ?? {});
}

function firstText(...values: readonly (string | null | undefined)[]): string {
  return values.find(value => typeof value === 'string' && value.trim().length > 0)?.trim() ?? '';
}
