import {
  MatrixSummary,
  ProviderMatrixView,
  SlotMatrixView,
  TenantDrawSalesMatrixView,
  matrixEntityIdValue,
} from './admin-draw-sales-matrix-api.service';

export function activeOnlyMatrix(matrix: TenantDrawSalesMatrixView): TenantDrawSalesMatrixView {
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

export function filterMatrix(matrix: TenantDrawSalesMatrixView, searchQuery: string): TenantDrawSalesMatrixView {
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

export function summarizeMatrix(providers: readonly ProviderMatrixView[]): MatrixSummary {
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

function includesQuery(value: string | null | undefined, query: string): boolean {
  return value?.toLocaleLowerCase().includes(query) === true;
}
