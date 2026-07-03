import { AdminStatusTone } from '@tch/ui/console';
import { ConsoleRowAction } from '../draw-results/console-draw-results-table.models';

export interface ConsoleDrawRow {
  readonly id: string;
  readonly groupLabel?: string;
  readonly title: string;
  readonly subtitle?: string;
  readonly meta?: string;
  readonly logoUrl?: string | null;
  readonly logoAlt?: string;
  readonly logoText?: string;
  readonly scheduledDateLabel?: string;
  readonly scheduledTimeLabel?: string;
  readonly countdownLabel?: string | null;
  readonly countdownTone?: 'default' | 'warning';
  readonly statusLabel: string;
  readonly statusTone: AdminStatusTone;
  readonly resultLabel?: string;
  readonly resultTone?: AdminStatusTone;
  readonly resultNumbers?: readonly string[];
  readonly resultHint?: string;
  readonly modeLabel?: string;
  readonly publicationLabel?: string;
  readonly publicationTone?: AdminStatusTone;
  readonly actions?: readonly ConsoleRowAction[];
  readonly pending?: boolean;
}

export interface ConsoleDrawActionEvent {
  readonly row: ConsoleDrawRow;
  readonly action: ConsoleRowAction;
}

export interface ConsoleDrawSelectionEvent {
  readonly row: ConsoleDrawRow;
  readonly selected: boolean;
}
