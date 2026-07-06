import { AdminStatusTone } from '@tch/ui/console';
import { ConsoleDrawSlotIdentity } from '../draw-slots/console-draw-slot-identity.models';

export type ConsoleRowActionTone = 'default' | 'primary' | 'danger';

export interface ConsoleRowAction {
  readonly id: string;
  readonly label: string;
  readonly icon?: string;
  readonly tone?: ConsoleRowActionTone;
  readonly variant?: 'button' | 'icon';
  readonly disabled?: boolean;
}

export interface ConsoleDrawResultRow {
  readonly id: string;
  readonly title: string;
  readonly labelKey?: string | null;
  readonly subtitle?: string;
  readonly meta?: string;
  readonly logoUrl?: string | null;
  readonly logoAlt?: string;
  readonly logoText?: string;
  readonly slotKey?: string;
  readonly identity: ConsoleDrawSlotIdentity;
  readonly numbers: readonly string[];
  readonly statusLabel: string;
  readonly statusTone: AdminStatusTone;
  readonly qualityLabel: string;
  readonly qualityTone: AdminStatusTone;
  readonly sourceLabel: string;
  readonly sourceFlags?: readonly string[];
  readonly hasSourceDetails?: boolean;
  readonly occurredDateLabel?: string;
  readonly providerDateLabel?: string;
  readonly providerTimeLabel?: string;
  readonly localDateLabel?: string;
  readonly localTimeLabel?: string;
  readonly fetchedAtLabel?: string;
  readonly appliedAtLabel?: string;
  readonly actions?: readonly ConsoleRowAction[];
}

export interface ConsoleDrawResultActionEvent {
  readonly row: ConsoleDrawResultRow;
  readonly action: ConsoleRowAction;
}

export interface ConsoleDrawResultSourceEvent {
  readonly row: ConsoleDrawResultRow;
}
