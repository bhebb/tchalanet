import { AdminStatusTone } from '@tch/ui/console';
import { ConsoleRowAction } from '../draw-results/console-draw-results-table.models';

export interface ConsolePricingRow {
  readonly id: string;
  readonly gameCode: string;
  readonly betType: string;
  readonly betOption?: string | number | null;
  readonly odds?: string | number | null;
  readonly payoutRuleLabel?: string | null;
  readonly payoutValue?: string | number | null;
  readonly tenantLabel?: string | null;
  readonly statusLabel: string;
  readonly statusTone: AdminStatusTone;
  readonly actions?: readonly ConsoleRowAction[];
}

export interface ConsolePricingActionEvent {
  readonly row: ConsolePricingRow;
  readonly action: ConsoleRowAction;
}
