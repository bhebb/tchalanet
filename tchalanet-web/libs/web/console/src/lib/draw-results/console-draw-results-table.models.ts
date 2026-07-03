import { AdminStatusTone } from '@tch/ui/console';

export type ConsoleRowActionTone = 'default' | 'primary' | 'danger';

export interface ConsoleRowAction {
  readonly id: string;
  readonly label: string;
  readonly icon?: string;
  readonly tone?: ConsoleRowActionTone;
  readonly variant?: 'button' | 'icon';
}

export interface ConsoleDrawResultRow {
  readonly id: string;
  readonly title: string;
  readonly subtitle?: string;
  readonly meta?: string;
  readonly logoUrl?: string | null;
  readonly logoAlt?: string;
  readonly slotKey?: string;
  readonly numbers: readonly string[];
  readonly statusLabel: string;
  readonly statusTone: AdminStatusTone;
  readonly qualityLabel: string;
  readonly qualityTone: AdminStatusTone;
  readonly sourceLabel: string;
  readonly fetchedAtLabel?: string;
  readonly appliedAtLabel?: string;
  readonly actions?: readonly ConsoleRowAction[];
}

export interface ConsoleDrawResultActionEvent {
  readonly row: ConsoleDrawResultRow;
  readonly action: ConsoleRowAction;
}
