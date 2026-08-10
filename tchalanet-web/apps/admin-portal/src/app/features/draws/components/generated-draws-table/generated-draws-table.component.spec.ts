import { TestBed } from '@angular/core/testing';
import { TranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it } from 'vitest';

import { GeneratedDrawsTableComponent } from './generated-draws-table.component';
import { GeneratedDrawGroup, GeneratedDrawView } from '../../data-access/admin-generated-draws.models';

function draw(overrides: Partial<GeneratedDrawView>): GeneratedDrawView {
  return {
    drawId: 'draw-1',
    drawChannelId: 'channel-1',
    drawChannelCode: 'NY',
    providerCode: 'NY',
    providerLabel: 'New York',
    slotKey: 'ny-eve',
    slotLabel: 'Soir',
    label: 'New York · Soir',
    businessDate: '2026-07-29',
    scheduledAt: '2026-07-29T19:00:00Z',
    timezone: 'America/New_York',
    salesStatus: 'CLOSED',
    resultStatus: 'MISSING',
    resultMode: 'MANUAL',
    ...overrides,
  };
}

function createComponent(canEnterManualResults: boolean) {
  const fixture = TestBed.createComponent(GeneratedDrawsTableComponent);
  fixture.componentRef.setInput('todayDate', '2026-07-29');
  fixture.componentRef.setInput('canEnterManualResults', canEnterManualResults);
  fixture.componentRef.setInput('canManageLifecycle', true);
  return fixture;
}

function withGroup(fixture: ReturnType<typeof createComponent>, drawView: GeneratedDrawView): void {
  const group: GeneratedDrawGroup = { date: '2026-07-29', draws: [drawView] };
  fixture.componentRef.setInput('groups', [group]);
}

describe('GeneratedDrawsTableComponent — resultActionHint', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        {
          provide: TranslateService,
          useValue: {
            currentLang: 'fr',
            instant: (key: string) =>
              ({
                'admin.generatedDraws.hint.resultToEnter': 'Résultat à saisir',
                'admin.generatedDraws.hint.resultMissing': 'Résultat manquant',
              })[key] ?? key,
          },
        },
      ],
    });
  });

  it('reads "Résultat à saisir" when manual entry is currently allowed', () => {
    const fixture = createComponent(true);
    // Scheduled well over 30 minutes ago and no result yet ⇒ canEnterManualResult() is true.
    withGroup(fixture, draw({
      resultStatus: 'MISSING',
      scheduledAt: new Date(Date.now() - 60 * 60 * 1000).toISOString(),
    }));

    const [row] = fixture.componentInstance.drawRows();
    expect(row.resultActionHint).toBe('Résultat à saisir');
  });

  it('reads "Résultat manquant" when manual entry is not yet allowed', () => {
    const fixture = createComponent(true);
    // Scheduled in the future ⇒ the 30-minute delay has not elapsed, canEnterManualResult() is false.
    withGroup(fixture, draw({
      resultStatus: 'MISSING',
      scheduledAt: new Date(Date.now() + 60 * 60 * 1000).toISOString(),
    }));

    const [row] = fixture.componentInstance.drawRows();
    expect(row.resultActionHint).toBe('Résultat manquant');
  });

  it('reads "Résultat manquant" when the user has no permission to enter results, even if due', () => {
    const fixture = createComponent(false);
    withGroup(fixture, draw({
      resultStatus: 'EXPECTED',
      scheduledAt: new Date(Date.now() - 60 * 60 * 1000).toISOString(),
    }));

    const [row] = fixture.componentInstance.drawRows();
    expect(row.resultActionHint).toBe('Résultat manquant');
  });

  it('leaves resultActionHint unset once a result exists', () => {
    const fixture = createComponent(true);
    withGroup(fixture, draw({
      resultStatus: 'CONFIRMED',
      numbers: ['1', '2', '3'],
    }));

    const [row] = fixture.componentInstance.drawRows();
    expect(row.resultActionHint).toBeUndefined();
    expect(row.resultLabel).toBe('domain.draw.resultStatus.CONFIRMED');
  });
});
