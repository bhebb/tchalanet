import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { TchSectionError } from '@tch/ui/components';
import { BadgeStatus, TchStatusBadge } from '@tch/ui/components';
import { TchErrorViewModel } from '@tch/web/errors';
import {
  ConsoleGameLogoUrlPipe,
  ConsoleGameLogoTextPipe,
  ConsoleGameNamePipe,
} from '@tch/web/console';
import {
  ChannelGameSetupView,
  DrawChannelSetupView,
  SetupWarning,
} from '../../data-access/admin-draw-sales-matrix-api.service';

export type MatrixGameCardMode = 'offered' | 'available';
export type MatrixGameAction = 'offer' | 'toggle';

export interface MatrixGameActionEvent {
  readonly action: MatrixGameAction;
  readonly game: ChannelGameSetupView;
}

@Component({
  selector: 'tch-draw-sales-matrix-game-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatButtonModule,
    MatIconModule,
    RouterLink,
    TranslatePipe,
    TchSectionError,
    TchStatusBadge,
    ConsoleGameLogoUrlPipe,
    ConsoleGameLogoTextPipe,
    ConsoleGameNamePipe,
  ],
  templateUrl: './matrix-game-card.component.html',
  styleUrls: ['./matrix-game-card.component.scss'],
})
export class DrawSalesMatrixGameCardComponent {
  readonly mode = input.required<MatrixGameCardMode>();
  readonly game = input.required<ChannelGameSetupView>();
  readonly channel = input<DrawChannelSetupView | null>(null);
  readonly acting = input(false);
  readonly actionError = input<TchErrorViewModel | null>(null);
  readonly actionNotice = input<string | null>(null);

  readonly gameAction = output<MatrixGameActionEvent>();

  protected gameStatus(game: ChannelGameSetupView): BadgeStatus {
    if (game.saleReady) return 'ready';
    if (!game.offeredOnChannel) return 'missing';
    const hasError = game.warnings.some(w => w.severity === 'ERROR');
    return hasError ? 'blocked' : 'warning';
  }

  protected gameStatusLabelKey(game: ChannelGameSetupView): string {
    if (game.saleReady) return 'admin.drawSalesMatrix.game.status.ready';
    if (!game.offeredOnChannel) return 'admin.drawSalesMatrix.game.status.notOffered';
    if (!game.enabledOnChannel) return 'admin.drawSalesMatrix.game.status.disabled';
    return 'admin.drawSalesMatrix.game.status.incomplete';
  }

  protected severityIcon(warning: SetupWarning): string {
    if (warning.severity === 'ERROR') return 'error';
    if (warning.severity === 'WARN') return 'warning';
    return 'info';
  }

  protected warningLabelKey(warning: SetupWarning): string {
    return `admin.drawSalesMatrix.warning.${warning.code}`;
  }
}
