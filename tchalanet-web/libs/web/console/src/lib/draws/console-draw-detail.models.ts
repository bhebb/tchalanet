import { AdminStatusTone } from '@tch/ui/console';

import { ConsoleDrawSlotIdentity } from '../draw-slots/console-draw-slot-identity.models';
import { ConsoleFact } from '../entity-detail/console-entity-detail.models';
import { ConsoleRowAction } from '../draw-results/console-draw-results-table.models';

export interface ConsoleDrawDetailMetric {
  readonly label: string;
  readonly value: string | number | null | undefined;
  readonly hint?: string | null;
  readonly tone?: AdminStatusTone;
}

export interface ConsoleDrawDetailResult {
  readonly statusLabel: string;
  readonly statusTone?: AdminStatusTone;
  readonly publicationLabel?: string | null;
  readonly publicationTone?: AdminStatusTone;
  readonly numbers?: readonly string[];
  readonly modeLabel?: string | null;
  readonly fetchedAtLabel?: string | null;
  readonly emptyLabel?: string | null;
  readonly sourceErrorLabel?: string | null;
}

export interface ConsoleDrawDetailSection {
  readonly id: string;
  readonly title: string;
  readonly icon?: string;
  readonly description?: string | null;
  readonly facts?: readonly ConsoleFact[];
  readonly metrics?: readonly ConsoleDrawDetailMetric[];
  readonly actions?: readonly ConsoleRowAction[];
}

export interface ConsoleDrawDetailAside {
  readonly eyebrow?: string | null;
  readonly title: string;
  readonly subtitle?: string | null;
  readonly code?: string | null;
  readonly items: readonly ConsoleDrawDetailMetric[];
  readonly advice?: string | null;
}

export interface ConsoleDrawDetailView {
  readonly identity: ConsoleDrawSlotIdentity;
  readonly title: string;
  readonly description?: string | null;
  readonly meta?: readonly string[];
  readonly statusLabel?: string | null;
  readonly statusTone?: AdminStatusTone;
  readonly overviewFacts?: readonly ConsoleFact[];
  readonly result?: ConsoleDrawDetailResult | null;
  readonly sections?: readonly ConsoleDrawDetailSection[];
  readonly aside?: ConsoleDrawDetailAside | null;
}
