import { LowerCasePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { BadgeStatus, TchCard, TchSectionError, TchSectionErrorSeverity, TchStatusBadge } from '@tch/ui/components';
import { AdminStatusPillComponent } from '@tch/ui/console';

import { ConsoleRowAction } from '../draw-results/console-draw-results-table.models';
import { ConsoleGameCardActionEvent, ConsoleGameCardBadgeTone, ConsoleGameCardView } from './console-games-table.models';

export interface ConsoleGameCardError {
  readonly title: string;
  readonly message: string;
  readonly severity?: TchSectionErrorSeverity;
}

@Component({
  selector: 'tch-console-game-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    LowerCasePipe,
    AdminStatusPillComponent,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    TchCard,
    TchSectionError,
    TchStatusBadge,
  ],
  templateUrl: './console-game-card.component.html',
  styleUrls: ['./console-game-card.component.scss'],
})
export class ConsoleGameCardComponent {
  readonly row = input.required<ConsoleGameCardView>();
  readonly actionError = input<ConsoleGameCardError | null>(null);

  readonly rowAction = output<ConsoleGameCardActionEvent>();

  badgeStatus(tone: ConsoleGameCardBadgeTone): BadgeStatus {
    if (tone === 'info') return 'pending';
    if (tone === 'neutral') return 'missing';
    return tone;
  }

  emitAction(row: ConsoleGameCardView, action: ConsoleRowAction): void {
    this.rowAction.emit({ row, action });
  }
}
