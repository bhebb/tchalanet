import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { TranslatePipe } from '@ngx-translate/core';
import { AdminStatusPillComponent } from '@tch/ui/console';

import { ConsoleDrawSlotIdentityComponent } from '../draw-slots/console-draw-slot-identity.component';
import {
  ConsoleDrawResultActionEvent,
  ConsoleDrawResultRow,
  ConsoleDrawResultSourceEvent,
  ConsoleRowAction,
} from './console-draw-results-table.models';

@Component({
  selector: 'tch-console-draw-results-table',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminStatusPillComponent,
    ConsoleDrawSlotIdentityComponent,
    MatButtonModule,
    MatTableModule,
    TranslatePipe,
  ],
  templateUrl: './console-draw-results-table.component.html',
  styleUrls: ['./console-draw-results-table.component.scss'],
})
export class ConsoleDrawResultsTableComponent {
  readonly rows = input.required<readonly ConsoleDrawResultRow[]>();
  readonly showAppliedAt = input(true);
  readonly showActions = input(true);
  readonly showSource = input(true);
  readonly showSourceFlags = input(false);
  readonly sourceColumnLabel = input('Source');
  private readonly expandedRows = signal<ReadonlySet<string>>(new Set());

  readonly rowAction = output<ConsoleDrawResultActionEvent>();
  readonly sourceAction = output<ConsoleDrawResultSourceEvent>();

  readonly columns = computed(() => {
    const columns = ['draw', 'occurredAt', 'numbers', 'status', 'quality', 'fetchedAt'];
    if (this.showAppliedAt()) columns.push('appliedAt');
    if (this.showSource()) columns.push('source');
    if (this.showActions() && this.rows().some(row => (row.actions?.length ?? 0) > 0)) {
      columns.push('actions');
    }
    return columns;
  });

  emitAction(row: ConsoleDrawResultRow, action: ConsoleRowAction): void {
    this.rowAction.emit({ row, action });
  }

  isExpanded(row: ConsoleDrawResultRow): boolean {
    return this.expandedRows().has(row.id);
  }

  toggleDetails(row: ConsoleDrawResultRow): void {
    this.expandedRows.update(current => {
      const next = new Set(current);
      if (next.has(row.id)) {
        next.delete(row.id);
      } else {
        next.add(row.id);
      }
      return next;
    });
  }

  canShowSource(row: ConsoleDrawResultRow): boolean {
    return row.hasSourceDetails === true || row.sourceLabel.trim().toUpperCase() === 'EXTERNAL';
  }

  emitSource(row: ConsoleDrawResultRow): void {
    this.sourceAction.emit({ row });
  }
}
