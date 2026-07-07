import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { TchSectionError } from '@tch/ui/components';
import { BadgeStatus, TchStatusBadge } from '@tch/ui/components';
import { TchErrorViewModel } from '@tch/web/errors';
import {
  ConsoleFact,
  ConsoleFactsComponent,
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
    TranslatePipe,
    TchSectionError,
    TchStatusBadge,
    ConsoleFactsComponent,
    ConsoleGameLogoUrlPipe,
    ConsoleGameLogoTextPipe,
    ConsoleGameNamePipe,
  ],
  templateUrl: './matrix-game-card.component.html',
  styleUrls: ['./matrix-game-card.component.scss'],
})
export class DrawSalesMatrixGameCardComponent {
  private readonly translate = inject(TranslateService);

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

  protected fallbackWarningLabel(warning: SetupWarning): string {
    return warning.code;
  }

  protected gameFacts(game: ChannelGameSetupView): readonly ConsoleFact[] {
    return [
      {
        label: this.t('admin.drawSalesMatrix.game.fact.minStake'),
        value: game.minStake ?? this.t('admin.drawSalesMatrix.game.fact.notConfigured'),
      },
      {
        label: this.t('admin.drawSalesMatrix.game.fact.maxStake'),
        value: game.maxStake ?? this.t('admin.drawSalesMatrix.game.fact.notConfigured'),
      },
      {
        label: this.t('admin.drawSalesMatrix.game.fact.limits'),
        value: game.limits.configured
          ? this.t('admin.drawSalesMatrix.game.fact.configured')
          : this.t('admin.drawSalesMatrix.game.fact.notConfigured'),
      },
      {
        label: this.t('admin.drawSalesMatrix.game.fact.visibility'),
        value: game.visibleInPos
          ? this.t('admin.drawSalesMatrix.game.fact.visibleInPos')
          : this.t('admin.drawSalesMatrix.game.fact.hiddenInPos'),
      },
      {
        label: this.t('admin.drawSalesMatrix.game.fact.source'),
        value: this.t('admin.drawSalesMatrix.game.fact.tenantGameSettings'),
      },
    ];
  }

  private t(key: string): string {
    return this.translate.instant(key);
  }
}
