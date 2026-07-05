import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { TranslatePipe } from '@ngx-translate/core';
import { AdminStatusPillComponent } from '@tch/ui/console';

import { ConsoleRowAction } from '../draw-results/console-draw-results-table.models';
import { ConsoleGameLogoTextPipe, ConsoleGameLogoUrlPipe, ConsoleGameNamePipe } from './console-game-labels.pipe';
import { ConsoleGameActionEvent, ConsoleGameRow } from './console-games-table.models';

@Component({
  selector: 'tch-console-games-table',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminStatusPillComponent,
    ConsoleGameLogoTextPipe,
    ConsoleGameLogoUrlPipe,
    ConsoleGameNamePipe,
    MatButtonModule,
    MatTableModule,
    TranslatePipe,
  ],
  templateUrl: './console-games-table.component.html',
  styleUrls: ['./console-games-table.component.scss'],
})
export class ConsoleGamesTableComponent {
  readonly rows = input.required<readonly ConsoleGameRow[]>();
  readonly showSortOrder = input(false);
  readonly showActions = input(true);

  readonly rowAction = output<ConsoleGameActionEvent>();

  readonly columns = computed(() => {
    const columns = ['code', 'name', 'category'];
    if (this.showSortOrder()) columns.push('sortOrder');
    columns.push('status');
    if (this.showActions() && this.rows().some(row => (row.actions?.length ?? 0) > 0)) {
      columns.push('actions');
    }
    return columns;
  });

  emitAction(row: ConsoleGameRow, action: ConsoleRowAction): void {
    this.rowAction.emit({ row, action });
  }
}
