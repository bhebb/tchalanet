import { TestBed } from '@angular/core/testing';
import { TchBackendClient } from '@tch/api';
import { firstValueFrom, of } from 'rxjs';

import { AdminDrawChannelsApiService } from './admin-draw-channels-api.service';
import type { TenantDrawSalesMatrixView } from '../../draw-sales-matrix/data-access/admin-draw-sales-matrix-api.service';

describe('AdminDrawChannelsApiService', () => {
  it('maps the backend draw sales matrix into business draw-channel cards', async () => {
    const backend = { get: vi.fn(() => of(haitiLotteryMatrix())) };
    TestBed.configureTestingModule({
      providers: [AdminDrawChannelsApiService, { provide: TchBackendClient, useValue: backend }],
    });

    const providers = await firstValueFrom(
      TestBed.inject(AdminDrawChannelsApiService).getDrawChannelProviders({
        suppressShellFeedback: true,
      }),
    );

    expect(backend.get).toHaveBeenCalledWith('/admin/setup/draw-sales-matrix', {
      suppressShellFeedback: true,
    });
    expect(providers).toHaveLength(1);
    expect(providers[0]).toMatchObject({
      providerCode: 'HT',
      providerLabel: 'HT',
      profileLabelKey: 'admin.drawChannels.profile.haitiLottery',
      tenantStatus: 'ACTIVE',
      configuredChannelCount: 2,
      activeChannelCount: 1,
      offeredGameCount: 2,
      saleReadyGameCount: 1,
      resultAcquisition: {
        mode: 'AUTO',
        source: 'API',
        sourceStatus: 'OK',
      },
      readiness: {
        status: 'READY',
        label: 'admin.drawChannels.readiness.ready',
        reason: null,
      },
    });
    expect(providers[0].slots[0]).toMatchObject({
      channelId: 'channel-1',
      slotKey: 'EVE',
      label: 'HT · Evening',
      enabled: true,
      offeredGameCount: 2,
      saleReadyGameCount: 1,
    });
  });
});

function haitiLotteryMatrix(): TenantDrawSalesMatrixView {
  return {
    summary: {
      providerCount: 1,
      slotCount: 2,
      configuredChannelCount: 2,
      activeChannelCount: 1,
      supportedTenantGameCount: 2,
      offeredChannelGameCount: 2,
      saleReadyChannelGameCount: 1,
      missingStakeConfigCount: 1,
      missingLimitCount: 0,
    },
    providers: [
      {
        providerCode: 'HT',
        slots: [
          {
            slotKey: 'EVE',
            labelKey: null,
            resultSlot: {
              resultSlotId: { value: 'slot-1' },
              drawTime: '20:00',
              daysOfWeek: 'MON,TUE,WED,THU,FRI,SAT,SUN',
              active: true,
            },
            channel: {
              drawChannelId: { value: 'channel-1' },
              channelCode: 'HT_EVE',
              active: true,
              configured: true,
              drawTime: '20:00',
              salesOpenTime: null,
              cutoffSec: 600,
              defaultSource: 'api',
              sortOrder: 1,
              dependsOnChannelId: null,
            },
            games: [
              game('BOLET', 'Bolèt', true, true, true, true),
              game('LOTO3', 'Loto 3 chif', true, true, true, false),
            ],
            slotReady: true,
            warnings: [],
          },
          {
            slotKey: 'MID',
            labelKey: null,
            resultSlot: {
              resultSlotId: { value: 'slot-2' },
              drawTime: '12:00',
              daysOfWeek: 'MON,TUE,WED,THU,FRI,SAT,SUN',
              active: true,
            },
            channel: {
              drawChannelId: { value: 'channel-2' },
              channelCode: 'HT_MID',
              active: false,
              configured: true,
              drawTime: '12:00',
              salesOpenTime: null,
              cutoffSec: 600,
              defaultSource: null,
              sortOrder: 2,
              dependsOnChannelId: null,
            },
            games: [game('BOLET', 'Bolèt', true, true, false, false)],
            slotReady: false,
            warnings: [],
          },
        ],
      },
    ],
  };
}

function game(
  gameCode: string,
  displayName: string,
  enabledForTenant: boolean,
  visibleInPos: boolean,
  offeredOnChannel: boolean,
  saleReady: boolean,
) {
  return {
    gameCode,
    tenantGameId: { value: `tenant-game-${gameCode}` },
    displayName,
    enabledForTenant,
    visibleInPos,
    offeredOnChannel,
    enabledOnChannel: offeredOnChannel,
    minStake: saleReady ? 1 : null,
    maxStake: saleReady ? 1000 : null,
    limits: { configured: saleReady, assignments: [] },
    saleReady,
    warnings: [],
  };
}
