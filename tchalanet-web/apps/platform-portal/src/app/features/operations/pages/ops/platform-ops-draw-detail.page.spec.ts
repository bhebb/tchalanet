import { describe, expect, it } from 'vitest';

import { DrawView } from '../../data-access/platform-ops-api.service';
import { actionsForDraw } from './platform-ops-draw-action-matrix';

function draw(status: DrawView['status'], resultStatus?: NonNullable<DrawView['lastResult']>['status']): DrawView {
  return {
    id: 'draw-1',
    tenantId: 'tenant-1',
    channel: { id: 'channel-1', code: 'NY', name: 'New York' },
    slot: { id: 'slot-1', key: 'ny-eve', label: 'Soir', timezone: 'America/New_York', drawTime: '19:00' },
    drawDate: '2026-08-06',
    scheduledAt: '2026-08-06T23:00:00Z',
    cutoffAt: '2026-08-06T22:30:00Z',
    status,
    active: true,
    lastResult: resultStatus
      ? {
          id: 'result-1',
          occurredAt: '2026-08-07T00:00:00Z',
          status: resultStatus,
          lot1: '123',
          lot2: '45',
          lot3: '67',
          lot4: null,
        }
      : null,
  };
}

describe('platform draw detail action matrix', () => {
  it('keeps lifecycle actions aligned with backend transitions', () => {
    expect(actionsForDraw(draw('SCHEDULED')).map(action => action.kind)).toEqual([
      'open', 'lock', 'reschedule', 'cancel',
    ]);
    expect(actionsForDraw(draw('OPEN')).map(action => action.kind)).toEqual([
      'close', 'lock', 'cancel',
    ]);
    expect(actionsForDraw(draw('RESULTED')).map(action => action.kind)).toEqual([
      'override', 'settle',
    ]);
    expect(actionsForDraw(draw('SETTLED')).map(action => action.kind)).toEqual(['archive']);
    expect(actionsForDraw(draw('CANCELED')).map(action => action.kind)).toEqual(['archive']);
  });

  it('requires a result before settlement and exposes platform result decisions', () => {
    expect(actionsForDraw(draw('CLOSED')).map(action => action.kind)).toEqual(['manual', 'cancel']);
    expect(actionsForDraw(draw('CLOSED', 'PROVISIONAL')).map(action => action.kind)).toEqual([
      'confirm', 'override', 'cancel',
    ]);
    expect(actionsForDraw(draw('CLOSED', 'CONFIRMED')).map(action => action.kind)).toEqual([
      'override', 'cancel',
    ]);
  });
});
