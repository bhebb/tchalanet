import { describe, expect, it } from 'vitest';

import { TenantDrawSalesMatrixView } from './admin-draw-sales-matrix-api.service';
import { activeOnlyMatrix, filterMatrix, summarizeMatrix } from './admin-draw-sales-matrix-view';

describe('draw sales matrix view helpers', () => {
  it('keeps only active draw channels and recomputes the compact summary', () => {
    const active = activeOnlyMatrix(matrixFixture);

    expect(active.providers).toHaveLength(1);
    expect(active.providers[0].slots.map(slot => slot.slotKey)).toEqual(['NY_MID']);
    expect(active.summary).toMatchObject({
      providerCount: 1,
      slotCount: 1,
      configuredChannelCount: 1,
      activeChannelCount: 1,
      supportedTenantGameCount: 2,
      offeredChannelGameCount: 2,
      saleReadyChannelGameCount: 1,
      missingStakeConfigCount: 1,
      missingLimitCount: 1,
    });
  });

  it('filters by game name without turning every game into an individual card', () => {
    const filtered = filterMatrix(activeOnlyMatrix(matrixFixture), 'maryaj gratis');

    expect(filtered.providers).toHaveLength(1);
    expect(filtered.providers[0].slots).toHaveLength(1);
    expect(filtered.providers[0].slots[0].games.map(game => game.displayName)).toEqual(['Maryaj gratis']);
    expect(filtered.summary).toMatchObject({
      slotCount: 1,
      supportedTenantGameCount: 1,
      offeredChannelGameCount: 1,
    });
  });

  it('filters by draw metadata and preserves every game in matching draw rows', () => {
    const filtered = filterMatrix(activeOnlyMatrix(matrixFixture), '14:30');

    expect(filtered.providers[0].slots[0].slotKey).toBe('NY_MID');
    expect(filtered.providers[0].slots[0].games.map(game => game.gameCode)).toEqual([
      'HT_BOLET',
      'HT_MARYAJ_GRATIS',
    ]);
  });

  it('summarizes unique tenant games from backend scalar ids and legacy value objects', () => {
    const summary = summarizeMatrix(matrixFixture.providers);

    expect(summary.supportedTenantGameCount).toBe(3);
    expect(summary.slotCount).toBe(2);
    expect(summary.activeChannelCount).toBe(1);
  });
});

const matrixFixture: TenantDrawSalesMatrixView = {
  summary: {
    providerCount: 0,
    slotCount: 0,
    configuredChannelCount: 0,
    activeChannelCount: 0,
    supportedTenantGameCount: 0,
    offeredChannelGameCount: 0,
    saleReadyChannelGameCount: 0,
    missingStakeConfigCount: 0,
    missingLimitCount: 0,
  },
  providers: [
    {
      providerCode: 'NY',
      slots: [
        {
          slotKey: 'NY_MID',
          labelKey: null,
          resultSlot: {
            resultSlotId: { value: 'slot-ny-mid' },
            drawTime: '14:30',
            daysOfWeek: 'MON-SUN',
            active: true,
          },
          channel: {
            drawChannelId: 'channel-ny-mid',
            channelCode: 'HT_NY_MID',
            active: true,
            configured: true,
            drawTime: '14:30',
            salesOpenTime: '05:30',
            cutoffSec: 300,
            defaultSource: 'EXTERNAL',
            sortOrder: 1,
            dependsOnChannelId: null,
          },
          games: [
            game({
              gameCode: 'HT_BOLET',
              tenantGameId: 'tenant-game-bolet',
              displayName: 'Bolèt',
              offeredOnChannel: true,
              enabledOnChannel: true,
              saleReady: true,
            }),
            game({
              gameCode: 'HT_MARYAJ_GRATIS',
              tenantGameId: { value: 'tenant-game-maryaj-gratis' },
              displayName: 'Maryaj gratis',
              offeredOnChannel: true,
              enabledOnChannel: true,
              minStake: null,
              saleReady: false,
              limitsConfigured: false,
            }),
          ],
          slotReady: true,
          warnings: [],
        },
        {
          slotKey: 'GA_LATE',
          labelKey: null,
          resultSlot: {
            resultSlotId: 'slot-ga-late',
            drawTime: '23:00',
            daysOfWeek: 'MON-SUN',
            active: true,
          },
          channel: {
            drawChannelId: 'channel-ga-late',
            channelCode: 'HT_GA_LATE',
            active: false,
            configured: true,
            drawTime: '23:00',
            salesOpenTime: '05:30',
            cutoffSec: 300,
            defaultSource: 'EXTERNAL',
            sortOrder: 2,
            dependsOnChannelId: null,
          },
          games: [
            game({
              gameCode: 'HT_LOTO5',
              tenantGameId: 'tenant-game-loto5',
              displayName: 'Loto 5',
              offeredOnChannel: true,
              enabledOnChannel: false,
              saleReady: false,
            }),
          ],
          slotReady: true,
          warnings: [],
        },
      ],
    },
  ],
};

function game(overrides: {
  gameCode: string;
  tenantGameId: string | { value: string };
  displayName: string;
  offeredOnChannel: boolean;
  enabledOnChannel: boolean;
  saleReady: boolean;
  minStake?: number | null;
  limitsConfigured?: boolean;
}) {
  return {
    gameCode: overrides.gameCode,
    tenantGameId: overrides.tenantGameId,
    displayName: overrides.displayName,
    enabledForTenant: true,
    visibleInPos: true,
    offeredOnChannel: overrides.offeredOnChannel,
    enabledOnChannel: overrides.enabledOnChannel,
    minStake: 'minStake' in overrides ? overrides.minStake ?? null : 1,
    maxStake: 1_000_000,
    limits: {
      configured: overrides.limitsConfigured ?? true,
      assignments: [],
    },
    saleReady: overrides.saleReady,
    warnings: [],
  };
}
