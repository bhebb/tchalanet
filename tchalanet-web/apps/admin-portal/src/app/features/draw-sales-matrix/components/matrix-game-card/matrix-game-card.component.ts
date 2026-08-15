import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { TchSectionError } from '@tch/ui/components';
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
    RouterLink,
    TranslatePipe,
    TchSectionError,
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
  readonly pendingEnabled = input<boolean | null>(null);
  readonly canManage = input(true);

  readonly gameAction = output<MatrixGameActionEvent>();

  protected readonly switchEnabled = computed(() => {
    const pending = this.pendingEnabled();
    if (pending !== null) return pending;
    const game = this.game();
    return game.offeredOnChannel && game.enabledOnChannel;
  });

  protected severityIcon(warning: SetupWarning): string {
    if (warning.severity === 'ERROR') return 'error';
    if (warning.severity === 'WARN') return 'warning';
    return 'info';
  }

  protected warningLabelKey(warning: SetupWarning): string {
    return `admin.drawSalesMatrix.warning.${warning.code.replace(/\./g, '_').toUpperCase()}`;
  }
}
