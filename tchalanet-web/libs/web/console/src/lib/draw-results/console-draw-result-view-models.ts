import { AdminStatusTone } from '@tch/ui/console';

import {
  ConsoleDrawIdentityInput,
  consoleDrawIdentity,
} from '../draw-slots/console-draw-identities';
import { ConsoleDrawSlotIdentity } from '../draw-slots/console-draw-slot-identity.models';
import { ConsoleFact } from '../entity-detail/console-entity-detail.models';
import {
  ConsoleDrawResultRow,
  ConsoleRowAction,
} from './console-draw-results-table.models';
import {
  ConsoleDrawResultSummaryFacts,
  ConsoleDrawResultSummaryView,
} from './console-draw-result-detail.models';

export interface ConsoleDrawResultRowViewModelInput {
  readonly id: string;
  readonly identity?: ConsoleDrawSlotIdentity;
  readonly identityInput?: ConsoleDrawIdentityInput;
  readonly title?: string | null;
  readonly subtitle?: string | null;
  readonly meta?: string | null;
  readonly labelKey?: string | null;
  readonly slotKey?: string | null;
  readonly numbers: readonly (string | null)[];
  readonly statusLabel: string;
  readonly statusTone: AdminStatusTone;
  readonly qualityLabel: string;
  readonly qualityTone: AdminStatusTone;
  readonly sourceLabel: string;
  readonly sourceFlags?: readonly string[];
  readonly hasSourceDetails?: boolean;
  readonly occurredDateLabel?: string | null;
  readonly providerDateLabel?: string | null;
  readonly providerTimeLabel?: string | null;
  readonly localDateLabel?: string | null;
  readonly localTimeLabel?: string | null;
  readonly fetchedAtLabel?: string | null;
  readonly appliedAtLabel?: string | null;
  readonly actions?: readonly ConsoleRowAction[];
}

export interface ConsoleDrawResultSummaryViewModelInput {
  readonly identity?: ConsoleDrawSlotIdentity;
  readonly identityInput?: ConsoleDrawIdentityInput;
  readonly numbers: readonly (string | number)[];
}

export interface ConsoleDrawResultSummaryFactsInput {
  readonly resultFacts?: readonly ConsoleFact[];
  readonly linkedDrawFacts?: readonly ConsoleFact[];
}

export function consoleDrawResultRowViewModel(
  input: ConsoleDrawResultRowViewModelInput,
): ConsoleDrawResultRow {
  const identity = resolveDrawIdentity(input.identity, input.identityInput);
  const title = firstText(input.title, identity.channelName, identity.channelShortName, identity.slotLabel, identity.slotKey, 'Résultat');
  const subtitle = firstText(input.subtitle, input.slotKey, identity.slotKey, identity.channelCode);
  const meta = firstText(input.meta, input.providerTimeLabel, input.providerDateLabel, identity.providerName);

  return {
    id: input.id,
    title,
    labelKey: input.labelKey,
    subtitle,
    meta,
    logoUrl: identity.providerLogoUrl,
    logoAlt: firstText(identity.providerName, identity.providerCode, title),
    logoText: identity.logoText ?? undefined,
    slotKey: input.slotKey ?? identity.slotKey ?? undefined,
    identity,
    numbers: input.numbers,
    statusLabel: input.statusLabel,
    statusTone: input.statusTone,
    qualityLabel: input.qualityLabel,
    qualityTone: input.qualityTone,
    sourceLabel: input.sourceLabel,
    sourceFlags: input.sourceFlags,
    hasSourceDetails: input.hasSourceDetails,
    occurredDateLabel: input.occurredDateLabel ?? undefined,
    providerDateLabel: input.providerDateLabel ?? undefined,
    providerTimeLabel: input.providerTimeLabel ?? undefined,
    localDateLabel: input.localDateLabel ?? undefined,
    localTimeLabel: input.localTimeLabel ?? undefined,
    fetchedAtLabel: input.fetchedAtLabel ?? undefined,
    appliedAtLabel: input.appliedAtLabel ?? undefined,
    actions: input.actions,
  };
}

export function consoleDrawResultSummaryViewModel(
  input: ConsoleDrawResultSummaryViewModelInput,
): ConsoleDrawResultSummaryView {
  return {
    identity: resolveDrawIdentity(input.identity, input.identityInput),
    numbers: input.numbers,
  };
}

export function consoleDrawResultSummaryFacts(
  input: ConsoleDrawResultSummaryFactsInput,
): ConsoleDrawResultSummaryFacts {
  return {
    resultFacts: input.resultFacts ?? [],
    linkedDrawFacts: input.linkedDrawFacts ?? [],
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
