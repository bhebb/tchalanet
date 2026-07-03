import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatMenuModule } from '@angular/material/menu';
import { MatTableModule } from '@angular/material/table';
import { AdminStatusPillComponent } from '@tch/ui/console';

import {
  ConsoleDrawActionEvent,
  ConsoleDrawRow,
  ConsoleDrawSelectionEvent,
} from './console-draws-table.models';
import { ConsoleRowAction } from '../draw-results/console-draw-results-table.models';

@Component({
  selector: 'tch-console-draws-table',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminStatusPillComponent,
    MatButtonModule,
    MatCheckboxModule,
    MatMenuModule,
    MatTableModule,
  ],
  templateUrl: './console-draws-table.component.html',
  styleUrls: ['./console-draws-table.component.scss'],
})
export class ConsoleDrawsTableComponent {
  readonly rows = input.required<readonly ConsoleDrawRow[]>();
  readonly selectable = input(false);
  readonly selectedIds = input<ReadonlySet<string>>(new Set());
  readonly totalElements = input<number | null>(null);
  readonly page = input<number | null>(null);
  readonly pageSize = input<number | null>(null);
  readonly hasPrevious = input(false);
  readonly hasNext = input(false);

  readonly rowAction = output<ConsoleDrawActionEvent>();
  readonly rowSelection = output<ConsoleDrawSelectionEvent>();
  readonly previousPage = output<void>();
  readonly nextPage = output<void>();

  readonly columns = computed(() => {
    const columns = this.selectable()
      ? ['select', 'draw', 'scheduledAt', 'status', 'result', 'mode', 'publication', 'actions']
      : ['draw', 'scheduledAt', 'status', 'result', 'mode', 'publication', 'actions'];
    return columns.filter(column =>
      column !== 'actions' || this.rows().some(row => row.pending || (row.actions?.length ?? 0) > 0),
    );
  });

  readonly groupedRows = computed(() => {
    const groups: Array<{ label: string | null; rows: readonly ConsoleDrawRow[] }> = [];
    for (const row of this.rows()) {
      const label = row.groupLabel ?? null;
      const last = groups[groups.length - 1];
      if (last && last.label === label) {
        groups[groups.length - 1] = { label, rows: [...last.rows, row] };
      } else {
        groups.push({ label, rows: [row] });
      }
    }
    return groups;
  });

  readonly showFooter = computed(() =>
    this.totalElements() != null && this.page() != null && this.pageSize() != null,
  );

  readonly rangeStart = computed(() => {
    const total = this.totalElements() ?? 0;
    if (total === 0) return 0;
    return ((this.page() ?? 0) * (this.pageSize() ?? 0)) + 1;
  });

  readonly rangeEnd = computed(() => {
    const total = this.totalElements() ?? 0;
    const page = this.page() ?? 0;
    const pageSize = this.pageSize() ?? total;
    return Math.min((page + 1) * pageSize, total);
  });

  isSelected(row: ConsoleDrawRow): boolean {
    return this.selectedIds().has(row.id);
  }

  emitSelection(row: ConsoleDrawRow, selected: boolean): void {
    this.rowSelection.emit({ row, selected });
  }

  emitAction(row: ConsoleDrawRow, action: ConsoleRowAction): void {
    this.rowAction.emit({ row, action });
  }
}
