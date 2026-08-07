import type { ConsoleDrawLifecycleDisplayAction } from '@tch/web/console';

import type { DrawResultOpsResponse, DrawView } from '../../data-access/platform-ops-api.service';

export type DrawActionItem =
  | { kind: ConsoleDrawLifecycleDisplayAction }
  | { kind: 'manual' }
  | { kind: 'confirm' }
  | { kind: 'override' };

export function actionsForDraw(draw: DrawView): DrawActionItem[] {
  switch (draw.status) {
    case 'SCHEDULED': return [{ kind: 'open' }, { kind: 'lock' }, { kind: 'reschedule' }, { kind: 'cancel' }];
    case 'OPEN': return [{ kind: 'close' }, { kind: 'lock' }, { kind: 'cancel' }];
    case 'LOCKED': return [{ kind: 'unlock' }, { kind: 'cancel' }];
    case 'CLOSED':
      if (!draw.lastResult) return [{ kind: 'manual' }, { kind: 'cancel' }];
      return draw.lastResult.status === 'PROVISIONAL'
        ? [{ kind: 'confirm' }, { kind: 'override' }, { kind: 'cancel' }]
        : [{ kind: 'override' }, { kind: 'cancel' }];
    case 'RESULTED': return [{ kind: 'override' }, { kind: 'settle' }];
    case 'SETTLED': return [{ kind: 'archive' }];
    case 'CANCELED':
    case 'CANCELLED': return [{ kind: 'archive' }];
    default: return [];
  }
}

export function drawResultOpsRow(draw: DrawView): DrawResultOpsResponse | null {
  const result = draw.lastResult;
  if (!result) return null;
  return {
    id: result.id,
    slotKey: draw.slot.key,
    occurredAt: `${draw.drawDate}T00:00:00Z`,
    status: result.status,
    source: 'DRAW_DETAIL',
    quality: 'COMPLETE',
    haitiResult: {
      lot1: result.lot1,
      lot2: result.lot2,
      lot3: result.lot3,
      lot4: result.lot4,
    },
  };
}
