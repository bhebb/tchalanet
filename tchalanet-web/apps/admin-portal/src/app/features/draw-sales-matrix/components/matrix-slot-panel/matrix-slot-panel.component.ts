import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatExpansionModule } from '@angular/material/expansion';
import { TranslatePipe } from '@ngx-translate/core';

import { BadgeStatus, TchStatusBadge } from '@tch/ui/components';
import { TchErrorViewModel } from '@tch/web/errors';
import {
  ConsoleDrawSlotIdentity,
  ConsoleDrawSlotIdentityComponent,
  consoleDrawIdentity,
} from '@tch/web/console';
import {
  ChannelGameSetupView,
  SlotMatrixView,
} from '../../data-access/admin-draw-sales-matrix-api.service';
import {
  DrawSalesMatrixGameCardComponent,
  MatrixGameAction,
  MatrixGameActionEvent,
} from '../matrix-game-card/matrix-game-card.component';

export interface MatrixSlotGameActionEvent {
  readonly action: MatrixGameAction;
  readonly slot: SlotMatrixView;
  readonly game: ChannelGameSetupView;
}

@Component({
  selector: 'tch-draw-sales-matrix-slot-panel',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    MatButtonModule,
    MatExpansionModule,
    TranslatePipe,
    TchStatusBadge,
    ConsoleDrawSlotIdentityComponent,
    DrawSalesMatrixGameCardComponent,
  ],
  templateUrl: './matrix-slot-panel.component.html',
  styleUrls: ['./matrix-slot-panel.component.scss'],
})
export class DrawSalesMatrixSlotPanelComponent {
  readonly providerCode = input.required<string>();
  readonly slot = input.required<SlotMatrixView>();
  readonly actingKey = input<string | null>(null);
  readonly actionErrors = input<Readonly<Record<string, TchErrorViewModel>>>({});
  readonly actionNotices = input<Readonly<Record<string, string>>>({});

  readonly gameAction = output<MatrixSlotGameActionEvent>();

  protected readonly identity = computed<ConsoleDrawSlotIdentity>(() => {
    const providerCode = this.providerCode();
    const slot = this.slot();

    return consoleDrawIdentity({
      providerCode,
      providerName: providerCode,
      slotKey: slot.slotKey,
      channelCode: slot.channel?.channelCode ?? null,
      localTimeLabel: slot.channel?.drawTime ?? null,
      officialTimeLabel: slot.resultSlot.drawTime,
    });
  });

  protected readonly offeredGames = computed(() => this.slot().games.filter(game => game.offeredOnChannel));
  protected readonly availableGames = computed(() =>
    this.slot().games.filter(game => !game.offeredOnChannel && game.enabledForTenant),
  );
  protected readonly activeOfferedCount = computed(() =>
    this.slot().games.filter(game => game.offeredOnChannel && game.enabledOnChannel).length,
  );
  protected readonly offeredCount = computed(() =>
    this.slot().games.filter(game => game.offeredOnChannel).length,
  );

  protected slotStatus(slot: SlotMatrixView): BadgeStatus {
    if (slot.slotReady) return 'ready';
    const hasError = slot.warnings.some(w => w.severity === 'ERROR');
    return hasError ? 'blocked' : 'warning';
  }

  protected slotStatusLabelKey(slot: SlotMatrixView): string {
    if (slot.slotReady) return 'admin.drawSalesMatrix.slot.status.ready';
    if (slot.channel) return 'admin.drawSalesMatrix.slot.status.review';
    return 'admin.drawSalesMatrix.slot.status.notConfigured';
  }

  protected cutoffMinutes(cutoffSec: number | null | undefined): number {
    if (cutoffSec == null) return 0;
    return Math.max(0, Math.round(cutoffSec / 60));
  }

  protected feedbackKey(slot: SlotMatrixView, game: ChannelGameSetupView): string | null {
    const drawChannelId = slot.channel?.drawChannelId.value;
    return drawChannelId ? `${drawChannelId}:${game.tenantGameId.value}` : null;
  }

  protected isActing(slot: SlotMatrixView, game: ChannelGameSetupView): boolean {
    const key = this.feedbackKey(slot, game);
    return key !== null && this.actingKey() === key;
  }

  protected actionError(slot: SlotMatrixView, game: ChannelGameSetupView): TchErrorViewModel | null {
    const key = this.feedbackKey(slot, game);
    return key ? this.actionErrors()[key] ?? null : null;
  }

  protected actionNotice(slot: SlotMatrixView, game: ChannelGameSetupView): string | null {
    const key = this.feedbackKey(slot, game);
    return key ? this.actionNotices()[key] ?? null : null;
  }

  protected onGameAction(event: MatrixGameActionEvent): void {
    this.gameAction.emit({
      action: event.action,
      slot: this.slot(),
      game: event.game,
    });
  }
}
