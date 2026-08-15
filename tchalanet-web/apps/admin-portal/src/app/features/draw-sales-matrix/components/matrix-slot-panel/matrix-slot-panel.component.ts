import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { SlicePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatExpansionModule } from '@angular/material/expansion';
import { TranslatePipe } from '@ngx-translate/core';

import { TchErrorViewModel } from '@tch/web/errors';
import {
  ConsoleDrawSlotIdentity,
  ConsoleDrawSlotIdentityComponent,
  consoleDrawIdentity,
} from '@tch/web/console';
import {
  ChannelGameSetupView,
  SlotMatrixView,
  matrixEntityIdValue,
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
    SlicePipe,
    TranslatePipe,
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
  readonly pendingEnabled = input<Readonly<Record<string, boolean>>>({});
  readonly canManage = input(true);

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

  protected readonly availableGames = computed(() => this.slot().games.filter(game => game.enabledForTenant));
  protected readonly activeOfferedCount = computed(() =>
    this.availableGames().filter(game => game.offeredOnChannel && game.enabledOnChannel).length,
  );
  protected readonly availableCount = computed(() => this.availableGames().length);

  protected feedbackKey(slot: SlotMatrixView, game: ChannelGameSetupView): string | null {
    const drawChannelId = matrixEntityIdValue(slot.channel?.drawChannelId);
    const tenantGameId = matrixEntityIdValue(game.tenantGameId);
    return drawChannelId && tenantGameId ? `${drawChannelId}:${tenantGameId}` : null;
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

  protected pendingGameEnabled(slot: SlotMatrixView, game: ChannelGameSetupView): boolean | null {
    const key = this.feedbackKey(slot, game);
    return key && this.pendingEnabled()[key] !== undefined ? this.pendingEnabled()[key] : null;
  }

  protected onGameAction(event: MatrixGameActionEvent): void {
    this.gameAction.emit({
      action: event.action,
      slot: this.slot(),
      game: event.game,
    });
  }

  protected cutoffMinutes(slot: SlotMatrixView): number | null {
    const cutoffSec = slot.channel?.cutoffSec;
    return cutoffSec === undefined || cutoffSec === null ? null : Math.round(cutoffSec / 60);
  }
}
