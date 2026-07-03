import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { TranslatePipe } from '@ngx-translate/core';
import { AdminStatusPillComponent } from '@tch/ui/console';

import { ConsoleRowAction } from '../draw-results/console-draw-results-table.models';
import { ConsolePricingActionEvent, ConsolePricingRow } from './console-pricing-table.models';

@Component({
  selector: 'tch-console-pricing-table',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AdminStatusPillComponent, MatButtonModule, MatTableModule, TranslatePipe],
  templateUrl: './console-pricing-table.component.html',
  styleUrls: ['./console-pricing-table.component.scss'],
})
export class ConsolePricingTableComponent {
  readonly rows = input.required<readonly ConsolePricingRow[]>();
  readonly showTenant = input(false);
  readonly showActions = input(true);

  readonly rowAction = output<ConsolePricingActionEvent>();

  readonly columns = computed(() => {
    const columns = ['gameCode', 'betType', 'betOption', 'odds'];
    if (this.showTenant()) columns.push('tenant');
    columns.push('status');
    if (this.showActions() && this.rows().some(row => (row.actions?.length ?? 0) > 0)) {
      columns.push('actions');
    }
    return columns;
  });

  emitAction(row: ConsolePricingRow, action: ConsoleRowAction): void {
    this.rowAction.emit({ row, action });
  }
}
