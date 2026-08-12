import { TestBed } from '@angular/core/testing';
import { TchBackendClient } from '@tch/api';
import { firstValueFrom, of } from 'rxjs';

import { AdminDrawChannelsApiService } from './admin-draw-channels-api.service';

describe('AdminDrawChannelsApiService', () => {
  it('loads all tenant draw channels and puts active channels first', async () => {
    const backend = { get: vi.fn(() => of(tenantDrawChannels())) };
    TestBed.configureTestingModule({
      providers: [AdminDrawChannelsApiService, { provide: TchBackendClient, useValue: backend }],
    });

    const providers = await firstValueFrom(
      TestBed.inject(AdminDrawChannelsApiService).getDrawChannelProviders({
        suppressShellFeedback: true,
      }),
    );

    expect(backend.get).toHaveBeenCalledWith('/tenant/draw-channels', {
      suppressShellFeedback: true,
      params: { activeOnly: 'false' },
    });
    expect(providers).toHaveLength(1);
    expect(providers[0]).toMatchObject({
      providerCode: 'HT',
      providerLabel: 'HT',
      timezone: 'America/Port-au-Prince',
      tenantStatus: 'ACTIVE',
      configuredChannelCount: 2,
      activeChannelCount: 1,
      readiness: {
        status: 'READY',
        label: 'admin.drawChannels.readiness.ready',
        reason: null,
      },
    });
    expect(providers[0].slots.map(slot => slot.channelId)).toEqual(['channel-active', 'channel-inactive']);
    expect(providers[0].slots[0]).toMatchObject({
      channelId: 'channel-active',
      label: 'HT · Evening',
      enabled: true,
      resultSlotActive: true,
      drawTime: '20:00',
      cutoffTime: '19:50',
    });
  });

  it('loads a tenant draw channel detail', async () => {
    const detail = {
      id: 'channel-active',
      code: 'HT_EVE',
      name: 'Haiti · Evening',
      timezone: 'America/Port-au-Prince',
      drawTime: '20:00',
      cutoffSec: 300,
      daysOfWeek: ['MONDAY'],
      active: true,
      sortOrder: 11,
      period: 'EVENING',
      notes: null,
      resultSlotId: 'slot-evening',
      defaultSource: null,
    };
    const backend = { get: vi.fn(() => of(detail)) };
    TestBed.configureTestingModule({
      providers: [AdminDrawChannelsApiService, { provide: TchBackendClient, useValue: backend }],
    });

    const result = await firstValueFrom(
      TestBed.inject(AdminDrawChannelsApiService).getChannelDetail('channel-active', {
        suppressShellFeedback: true,
      }),
    );

    expect(backend.get).toHaveBeenCalledWith('/tenant/draw-channels/channel-active', {
      suppressShellFeedback: true,
    });
    expect(result).toBe(detail);
  });

  it('saves a tenant draw channel configuration', async () => {
    const backend = { put: vi.fn(() => of({ id: 'channel-active' })) };
    TestBed.configureTestingModule({
      providers: [AdminDrawChannelsApiService, { provide: TchBackendClient, useValue: backend }],
    });

    await firstValueFrom(
      TestBed.inject(AdminDrawChannelsApiService).updateChannel(
        'channel-active',
        {
          name: 'Haiti · Evening',
          timezone: 'America/Port-au-Prince',
          drawTime: '20:00',
          cutoffSec: 300,
          daysOfWeek: ['MONDAY', 'TUESDAY'],
          active: true,
          sortOrder: 11,
          period: 'EVENING',
          notes: null,
          defaultSource: null,
        },
        { suppressShellFeedback: true },
      ),
    );

    expect(backend.put).toHaveBeenCalledWith(
      '/tenant/draw-channels/channel-active',
      {
        name: 'Haiti · Evening',
        timezone: 'America/Port-au-Prince',
        drawTime: '20:00',
        cutoffSec: 300,
        daysOfWeek: ['MONDAY', 'TUESDAY'],
        active: true,
        sortOrder: 11,
        period: 'EVENING',
        notes: null,
        defaultSource: null,
      },
      { suppressShellFeedback: true },
    );
  });
});

function tenantDrawChannels() {
  return [
    {
      id: 'channel-inactive',
      channelCode: 'HT_MID',
      channelName: 'HT · Midday',
      drawTime: '12:00',
      cutoffTime: '11:50',
      timezone: 'America/Port-au-Prince',
      active: false,
      resultSlotActive: true,
    },
    {
      id: 'channel-active',
      channelCode: 'HT_EVE',
      channelName: 'HT · Evening',
      drawTime: '20:00',
      cutoffTime: '19:50',
      timezone: 'America/Port-au-Prince',
      active: true,
      resultSlotActive: true,
    },
  ];
}
