import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import {
  AdminDetailLayoutComponent,
  AdminPageShellComponent,
} from '@tch/ui/console';

import {
  ConsoleEntityDetailActionEvent,
  ConsoleEntityDetailError,
  ConsoleEntityDetailState,
} from './console-entity-detail.models';
import { ConsoleRowAction } from '../draw-results/console-draw-results-table.models';

@Component({
  selector: 'tch-console-entity-detail',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminPageShellComponent,
    AdminDetailLayoutComponent,
    TchLoading,
    TchErrorPanel,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './console-entity-detail.component.html',
  styleUrls: ['./console-entity-detail.component.scss'],
})
export class ConsoleEntityDetailComponent {
  readonly title = input.required<string>();
  readonly description = input<string>('');
  readonly state = input.required<ConsoleEntityDetailState>();
  readonly loadingLabel = input('Chargement...');
  readonly error = input<ConsoleEntityDetailError | null>(null);
  readonly meta = input<readonly string[]>([]);
  readonly actions = input<readonly ConsoleRowAction[]>([]);

  readonly retry = output<void>();
  readonly action = output<ConsoleEntityDetailActionEvent>();

  emitAction(action: ConsoleRowAction): void {
    this.action.emit({ action });
  }
}
