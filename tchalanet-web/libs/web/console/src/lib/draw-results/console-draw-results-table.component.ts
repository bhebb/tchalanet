import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { AdminStatusPillComponent } from '@tch/ui/console';

import { ConsoleDrawSlotIdentityComponent } from '../draw-slots/console-draw-slot-identity.component';
import {
  ConsoleDrawResultActionEvent,
  ConsoleDrawResultRow,
  ConsoleRowAction,
} from './console-draw-results-table.models';

@Component({
  selector: 'tch-console-draw-results-table',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AdminStatusPillComponent, ConsoleDrawSlotIdentityComponent, MatButtonModule, MatTableModule],
  templateUrl: './console-draw-results-table.component.html',
  styleUrls: ['./console-draw-results-table.component.scss'],
})
export class ConsoleDrawResultsTableComponent {
  readonly rows = input.required<readonly ConsoleDrawResultRow[]>();
  readonly showAppliedAt = input(true);
  readonly showActions = input(true);

  readonly rowAction = output<ConsoleDrawResultActionEvent>();

  readonly columns = computed(() => {
    const columns = ['draw', 'numbers', 'status', 'quality', 'fetchedAt'];
    if (this.showAppliedAt()) columns.push('appliedAt');
    columns.push('source');
    if (this.showActions() && this.rows().some(row => (row.actions?.length ?? 0) > 0)) {
      columns.push('actions');
    }
    return columns;
  });

  emitAction(row: ConsoleDrawResultRow, action: ConsoleRowAction): void {
    this.rowAction.emit({ row, action });
  }
}
