import { AdminStatusTone } from '@tch/ui/console';
import { ConsoleRowAction } from '../draw-results/console-draw-results-table.models';

export interface ConsoleGameRow {
  readonly id: string;
  readonly code: string;
  readonly name: string;
  readonly category?: string | null;
  readonly sortOrder?: number | null;
  readonly statusLabel: string;
  readonly statusTone: AdminStatusTone;
  readonly supporting?: string | null;
  readonly actions?: readonly ConsoleRowAction[];
}

export interface ConsoleGameActionEvent {
  readonly row: ConsoleGameRow;
  readonly action: ConsoleRowAction;
}
