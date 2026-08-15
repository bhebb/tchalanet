import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { webAppErrorFromProblemDetail } from '@tch/api';
import type { ProblemDetail } from '@tch/api';
import { AccessService } from '@tch/core/auth';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { AdminPageShellComponent } from '@tch/ui/console';
import { resourceErrorVm, tchMutation, TchAsyncReadyDirective, TchAsyncViewComponent } from '@tch/web/async';
import {
  AdminDrawSalesMatrixApi,
  SlotMatrixView,
  ChannelGameSetupView,
  MatrixSummary,
  ProviderMatrixView,
  TenantDrawSalesMatrixView,
  matrixEntityIdValue,
} from '../data-access/admin-draw-sales-matrix-api.service';
import {
  DrawSalesMatrixProviderListComponent,
  MatrixProviderGameActionEvent,
} from '../components/matrix-provider-list/matrix-provider-list.component';

interface MatrixGameMutationInput {
  readonly key: string;
  readonly drawChannelId: string;
  readonly tenantGameId: string;
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
  ],
  templateUrl: './admin-draw-sales-matrix.page.html',
  styleUrls: ['./admin-draw-sales-matrix.page.scss'],
})
export class AdminDrawSalesMatrixPage {
  private readonly api = inject(AdminDrawSalesMatrixApi);
  private readonly access = inject(AccessService);
  private readonly translate = inject(TranslateService);

  readonly canManageMatrix = computed(() => this.access.can({ permission: 'game-pricing.update' }));
  readonly matrixResource = this.api.getMatrixResource({ suppressShellFeedback: true });
  readonly matrixError = resourceErrorVm(this.matrixResource, 'admin.setup.draw_sales_matrix');
  readonly matrix = computed(() => this.matrixResource.value() ?? null);
  readonly activeMatrix = computed(() => {
    const matrix = this.matrix();
    return matrix ? activeOnlyMatrix(matrix) : null;
  });
  readonly searchQuery = signal('');
  readonly filteredMatrix = computed(() => {
    const matrix = this.activeMatrix();
    if (!matrix) return null;
    return filterMatrix(matrix, this.searchQuery());
  });
  readonly acting = signal<string | null>(null); // key = `${drawChannelId}:${tenantGameId}`
  readonly actionErrors = signal<Readonly<Record<string, ErrorViewModel>>>({});
  readonly pendingEnabled = signal<Readonly<Record<string, boolean>>>({});
  readonly offerGameMutation = tchMutation<MatrixGameMutationInput, unknown>({
    run: input => this.api.offerGame(input.drawChannelId, input.tenantGameId, { suppressShellFeedback: true }),
    source: 'admin.setup.draw_sales_matrix.offer',
    onSuccess: () => {
      this.acting.set(null);
      this.load(true);
    },
    onError: (err, input) => {
      this.acting.set(null);
      this.clearPendingEnabled(input.key);
      this.setActionError(input.key, err, input.drawChannelId, input.tenantGameId);
      return true;
    },
  });
  readonly toggleGameMutation = tchMutation<MatrixToggleGameInput, unknown>({
    run: input =>
      this.api.toggleGame(input.drawChannelId, input.tenantGameId, input.enabled, { suppressShellFeedback: true }),
    source: 'admin.setup.draw_sales_matrix.toggle',
    onSuccess: () => {
      this.acting.set(null);
      this.load(true);
    },
    onError: (err, input) => {
      this.acting.set(null);
      this.clearPendingEnabled(input.key);
      this.setActionError(input.key, err, input.drawChannelId, input.tenantGameId);
      return true;
    },
  });

  load(preserveActionFeedback = false): void {
    if (!preserveActionFeedback) {
      this.actionErrors.set({});
      this.pendingEnabled.set({});
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
    if (!this.canManageMatrix()) return;
    const drawChannelId = matrixEntityIdValue(slot.channel?.drawChannelId);
    if (!drawChannelId) return;
    const tenantGameId = matrixEntityIdValue(game.tenantGameId);
    if (!tenantGameId) return;
    const key = this.actingKey(drawChannelId, tenantGameId);
    this.acting.set(key);
    this.setPendingEnabled(key, true);
    this.clearActionError(key);
    this.offerGameMutation.execute({
      key,
      drawChannelId,
      tenantGameId,
    }, {
      key,
    });
  }

  toggleGame(slot: SlotMatrixView, game: ChannelGameSetupView): void {
    if (!this.canManageMatrix()) return;
    const drawChannelId = matrixEntityIdValue(slot.channel?.drawChannelId);
    if (!drawChannelId) return;
    const tenantGameId = matrixEntityIdValue(game.tenantGameId);
    if (!tenantGameId) return;
    const key = this.actingKey(drawChannelId, tenantGameId);
    this.acting.set(key);
    this.clearActionError(key);
    const newEnabled = !game.enabledOnChannel;
    this.setPendingEnabled(key, newEnabled);
    this.toggleGameMutation.execute({
      key,
      drawChannelId,
      tenantGameId,
      enabled: newEnabled,
    }, {
      key,
    });
  }

  onMatrixGameAction(event: MatrixProviderGameActionEvent): void {
    switch (event.action) {
      case 'offer':
        this.offerGame(event.slot, event.game);
        break;
      case 'toggle':
        this.toggleGame(event.slot, event.game);
        break;
    }
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

  private clearActionError(key: string): void {
    this.actionErrors.update(current => {
      if (!current[key]) return current;
      const next = { ...current };
      delete next[key];
      return next;
    });
  }

  private setPendingEnabled(key: string, enabled: boolean): void {
    this.pendingEnabled.update(current => ({ ...current, [key]: enabled }));
  }

  private clearPendingEnabled(key: string): void {
    this.pendingEnabled.update(current => {
      if (current[key] === undefined) return current;
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

function activeOnlyMatrix(matrix: TenantDrawSalesMatrixView): TenantDrawSalesMatrixView {
  const providers = matrix.providers
    .map(provider => ({
      ...provider,
      slots: provider.slots.filter(slot => slot.channel?.active === true),
    }))
    .filter(provider => provider.slots.length > 0);

  return {
    providers,
    summary: summarizeMatrix(providers),
  };
}

function filterMatrix(matrix: TenantDrawSalesMatrixView, searchQuery: string): TenantDrawSalesMatrixView {
  const query = searchQuery.trim().toLocaleLowerCase();
  if (!query) return matrix;

  const providers = matrix.providers
    .map(provider => {
      const providerMatches = includesQuery(provider.providerCode, query);
      const slots = provider.slots
        .map(slot => {
          const slotMatches =
            providerMatches ||
            includesQuery(slot.slotKey, query) ||
            includesQuery(slot.channel?.channelCode, query) ||
            includesQuery(slot.channel?.drawTime, query) ||
            includesQuery(slot.resultSlot.drawTime, query);
          const games = slotMatches
            ? slot.games
            : slot.games.filter(game =>
                includesQuery(game.gameCode, query) ||
                includesQuery(game.displayName, query),
              );
          return games.length > 0 ? { ...slot, games } : null;
        })
        .filter((slot): slot is SlotMatrixView => slot !== null);

      return slots.length > 0 ? { ...provider, slots } : null;
    })
    .filter((provider): provider is ProviderMatrixView => provider !== null);

  return {
    providers,
    summary: summarizeMatrix(providers),
  };
}

function includesQuery(value: string | null | undefined, query: string): boolean {
  return value?.toLocaleLowerCase().includes(query) === true;
}

function summarizeMatrix(providers: readonly ProviderMatrixView[]): MatrixSummary {
  const slots = providers.flatMap(provider => provider.slots);
  const games = slots.flatMap(slot => slot.games);

  return {
    providerCount: providers.length,
    slotCount: slots.length,
    configuredChannelCount: slots.filter(slot => slot.channel !== null).length,
    activeChannelCount: slots.filter(slot => slot.channel?.active === true).length,
    supportedTenantGameCount: new Set(games.map(game => matrixEntityIdValue(game.tenantGameId)).filter(Boolean)).size,
    offeredChannelGameCount: games.filter(game => game.offeredOnChannel).length,
    saleReadyChannelGameCount: games.filter(game => game.saleReady).length,
    missingStakeConfigCount: games.filter(game => game.offeredOnChannel && (game.minStake === null || game.maxStake === null)).length,
    missingLimitCount: games.filter(game => game.offeredOnChannel && !game.limits.configured).length,
  };
}
