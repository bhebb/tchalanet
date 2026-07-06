import { ConsoleDrawSlotIdentity } from '../draw-slots/console-draw-slot-identity.models';
import { ConsoleFact } from '../entity-detail/console-entity-detail.models';

export interface ConsoleDrawResultSummaryView {
  readonly identity: ConsoleDrawSlotIdentity;
  readonly numbers: readonly (string | number)[];
}

export interface ConsoleDrawResultSummaryFacts {
  readonly resultFacts: readonly ConsoleFact[];
  readonly linkedDrawFacts: readonly ConsoleFact[];
}
