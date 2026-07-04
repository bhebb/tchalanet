import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { webAppErrorFromProblemDetail } from '@tch/api';
import type { ProblemDetail } from '@tch/api';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { AdminPageShellComponent } from '@tch/ui/console';
import { resourceErrorVm, tchMutation, TchAsyncReadyDirective, TchAsyncViewComponent } from '@tch/web/async';
import { consoleGameName } from '@tch/web/console';
import type { TenantGameView } from '../../games-pricing/data-access/games-admin-api.service';
import { GameSettingsDialog } from '../../games-pricing/components/dialogs/game-settings.dialog';
import {
  AdminDrawSalesMatrixApi,
  SlotMatrixView,
  ChannelGameSetupView,
} from '../data-access/admin-draw-sales-matrix-api.service';
import {
  DrawSalesMatrixProviderListComponent,
  MatrixProviderGameActionEvent,
} from '../components/matrix-provider-list/matrix-provider-list.component';
import { DrawSalesMatrixSummaryComponent } from '../components/matrix-summary/matrix-summary.component';

interface MatrixGameMutationInput {
  readonly key: string;
  readonly drawChannelId: string;
  readonly tenantGameId: string;
  readonly game: ChannelGameSetupView;
}

interface MatrixToggleGameInput extends MatrixGameMutationInput {
  readonly enabled: boolean;
}

@Component({
  selector: 'tch-admin-draw-sales-matrix-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminPageShellComponent,
    TranslatePipe,
    MatButtonModule,
    TchAsyncReadyDirective,
    TchAsyncViewComponent,
    DrawSalesMatrixProviderListComponent,
    DrawSalesMatrixSummaryComponent,
  ],
  templateUrl: './admin-draw-sales-matrix.page.html',
  styleUrls: ['./admin-draw-sales-matrix.page.scss'],
})
export class AdminDrawSalesMatrixPage {
  private readonly api = inject(AdminDrawSalesMatrixApi);
  private readonly dialog = inject(MatDialog);
  private readonly translate = inject(TranslateService);

  readonly matrixResource = this.api.getMatrixResource({ suppressShellFeedback: true });
  readonly matrixError = resourceErrorVm(this.matrixResource, 'admin.setup.draw_sales_matrix');
  readonly matrix = computed(() => this.matrixResource.value() ?? null);
  readonly acting = signal<string | null>(null); // key = `${drawChannelId}:${tenantGameId}`
  readonly actionErrors = signal<Readonly<Record<string, ErrorViewModel>>>({});
  readonly actionNotices = signal<Readonly<Record<string, string>>>({});
  readonly offerGameMutation = tchMutation<MatrixGameMutationInput, unknown>({
    run: input => this.api.offerGame(input.drawChannelId, input.tenantGameId, { suppressShellFeedback: true }),
    source: 'admin.setup.draw_sales_matrix.offer',
    onSuccess: (_result, input) => {
      this.acting.set(null);
      this.setActionNotice(input.key, this.translate.instant('admin.drawSalesMatrix.feedback.added', {
        game: this.gameLabel(input.game),
      }));
      this.load(true);
    },
    onError: (err, input) => {
      this.acting.set(null);
      this.setActionError(input.key, err, input.drawChannelId, input.tenantGameId);
      return true;
    },
  });
  readonly toggleGameMutation = tchMutation<MatrixToggleGameInput, unknown>({
    run: input =>
      this.api.toggleGame(input.drawChannelId, input.tenantGameId, input.enabled, { suppressShellFeedback: true }),
    source: 'admin.setup.draw_sales_matrix.toggle',
    onSuccess: (_result, input) => {
      this.acting.set(null);
      this.setActionNotice(input.key, this.translate.instant(
        input.enabled ? 'admin.drawSalesMatrix.feedback.activated' : 'admin.drawSalesMatrix.feedback.disabled',
        { game: this.gameLabel(input.game) },
      ));
      this.load(true);
    },
    onError: (err, input) => {
      this.acting.set(null);
      this.setActionError(input.key, err, input.drawChannelId, input.tenantGameId);
      return true;
    },
  });
  readonly removeGameMutation = tchMutation<MatrixGameMutationInput, unknown>({
    run: input => this.api.removeGame(input.drawChannelId, input.tenantGameId, { suppressShellFeedback: true }),
    source: 'admin.setup.draw_sales_matrix.remove',
    onSuccess: (_result, input) => {
      this.acting.set(null);
      this.setActionNotice(input.key, this.translate.instant('admin.drawSalesMatrix.feedback.removed', {
        game: this.gameLabel(input.game),
      }));
      this.load(true);
    },
    onError: (err, input) => {
      this.acting.set(null);
      this.setActionError(input.key, err, input.drawChannelId, input.tenantGameId);
      return true;
    },
  });

  load(preserveActionFeedback = false): void {
    if (!preserveActionFeedback) {
      this.actionErrors.set({});
      this.actionNotices.set({});
    }
    this.matrixResource.reload();
  }

  actingKey(drawChannelId: string, tenantGameId: string): string {
    return `${drawChannelId}:${tenantGameId}`;
  }

  isActing(drawChannelId: string, tenantGameId: string): boolean {
    return this.acting() === this.actingKey(drawChannelId, tenantGameId);
  }

  offerGame(slot: SlotMatrixView, game: ChannelGameSetupView): void {
    const drawChannelId = slot.channel?.drawChannelId.value;
    if (!drawChannelId) return;
    const tenantGameId = game.tenantGameId.value;
    const key = this.actingKey(drawChannelId, tenantGameId);
    this.acting.set(key);
    this.clearActionError(key);
    this.clearActionNotice(key);
    this.offerGameMutation.execute({
      key,
      drawChannelId,
      tenantGameId,
      game,
    }, {
      key,
    });
  }

  toggleGame(slot: SlotMatrixView, game: ChannelGameSetupView): void {
    const drawChannelId = slot.channel?.drawChannelId.value;
    if (!drawChannelId) return;
    const tenantGameId = game.tenantGameId.value;
    const key = this.actingKey(drawChannelId, tenantGameId);
    this.acting.set(key);
    this.clearActionError(key);
    this.clearActionNotice(key);
    const newEnabled = !game.enabledOnChannel;
    this.toggleGameMutation.execute({
      key,
      drawChannelId,
      tenantGameId,
      game,
      enabled: newEnabled,
    }, {
      key,
    });
  }

  removeGame(slot: SlotMatrixView, game: ChannelGameSetupView): void {
    const drawChannelId = slot.channel?.drawChannelId.value;
    if (!drawChannelId) return;
    const tenantGameId = game.tenantGameId.value;
    const key = this.actingKey(drawChannelId, tenantGameId);
    this.acting.set(key);
    this.clearActionError(key);
    this.clearActionNotice(key);
    this.removeGameMutation.execute({
      key,
      drawChannelId,
      tenantGameId,
      game,
    }, {
      key,
    });
  }

  configureGame(game: ChannelGameSetupView): void {
    const dialogGame = this.toDialogGame(game);
    const ref = this.dialog.open(GameSettingsDialog, { data: { game: dialogGame }, width: '520px' });
    ref.afterClosed().subscribe(ok => { if (ok) this.load(); });
  }

  isMaryajGratis(game: ChannelGameSetupView): boolean {
    return (
      game.gameCode === 'HT_MARYAJ_GRATUIT' ||
      game.displayName?.toLowerCase().includes('maryaj gratuit') === true
    );
  }

  gameLabel(game: ChannelGameSetupView): string {
    if (this.isMaryajGratis(game)) return 'Maryaj gratis';
    return consoleGameName(game.gameCode, game.displayName);
  }

  onMatrixGameAction(event: MatrixProviderGameActionEvent): void {
    switch (event.action) {
      case 'configure':
        this.configureGame(event.game);
        break;
      case 'offer':
        this.offerGame(event.slot, event.game);
        break;
      case 'toggle':
        this.toggleGame(event.slot, event.game);
        break;
      case 'remove':
        this.removeGame(event.slot, event.game);
        break;
    }
  }

  private toDialogGame(game: ChannelGameSetupView): TenantGameView {
    return {
      gameCode: game.gameCode,
      catalogName: this.gameLabel(game),
      displayName: game.displayName,
      category: null,
      enabled: game.enabledForTenant,
      visibleInPos: game.visibleInPos,
      displayOrder: 0,
      minStake: game.minStake,
      maxStake: game.maxStake,
      availabilityEnabled: false,
      availabilityDays: null,
      startLocalTime: null,
      endLocalTime: null,
      readyForSale: game.saleReady,
    };
  }

  private setActionError(
    key: string,
    err: unknown,
    drawChannelId: string,
    tenantGameId: string,
  ): void {
    const error = this.errorViewModel(err, `admin.setup.draw_sales_matrix.${drawChannelId}.${tenantGameId}`, 'section');
    this.actionErrors.update(current => ({ ...current, [key]: error }));
  }

  private setActionNotice(key: string, message: string): void {
    this.actionNotices.update(current => ({ ...current, [key]: message }));
  }

  private clearActionError(key: string): void {
    this.actionErrors.update(current => {
      if (!current[key]) return current;
      const next = { ...current };
      delete next[key];
      return next;
    });
  }

  private clearActionNotice(key: string): void {
    this.actionNotices.update(current => {
      if (!current[key]) return current;
      const next = { ...current };
      delete next[key];
      return next;
    });
  }

  private errorViewModel(
    err: unknown,
    source: string,
    surface: 'page' | 'section',
  ): ErrorViewModel {
    const problem = (err as { error?: ProblemDetail })?.error;
    if (!problem) {
      return {
        severity: 'error',
        title: this.translate.instant('common.errors.fallback.title'),
        message: this.translate.instant('common.errors.fallback.message'),
      };
    }

    const normalized = webAppErrorFromProblemDetail(problem, source, surface);
    const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
    return toErrorViewModel(normalized, copy);
  }
}
