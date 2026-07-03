import { ConsoleRowAction } from '../draw-results/console-draw-results-table.models';

export type ConsoleEntityDetailState = 'loading' | 'ready' | 'error';

export interface ConsoleEntityDetailError {
  readonly title: string;
  readonly message: string;
}

export interface ConsoleFact {
  readonly label: string;
  readonly value: string | number | null | undefined;
  readonly code?: boolean;
}

export interface ConsoleEntityDetailActionEvent {
  readonly action: ConsoleRowAction;
}
