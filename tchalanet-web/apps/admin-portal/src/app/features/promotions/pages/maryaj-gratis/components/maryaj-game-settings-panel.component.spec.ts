import { TestBed } from '@angular/core/testing';
import { TranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it } from 'vitest';

import { MaryajGameSettingsPanelComponent } from './maryaj-game-settings-panel.component';

describe(MaryajGameSettingsPanelComponent.name, () => {
  let component: MaryajGameSettingsPanelComponent;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        {
          provide: TranslateService,
          useValue: {
            instant: (key: string) =>
              ({
                'admin.maryajGratis.game.exact': 'Egzak',
                'admin.maryajGratis.game.reverse': 'Reverse',
                'admin.maryajGratis.game.readiness.READY': 'Pare',
                'admin.maryajGratis.game.readiness.TODO': 'Pou konfigire',
                'admin.maryajGratis.game.readiness.BLOCKED': 'Pa disponib',
                'admin.gamesPricing.readiness.reason.missingStakeOrPricing':
                  'Miz oswa barèm manke.',
              })[key] ?? key,
          },
        },
      ],
    });
    component = TestBed.runInInjectionContext(() => new MaryajGameSettingsPanelComponent());
  });

  it('does not leak raw readiness translation keys for unknown statuses', () => {
    expect(component.readinessLabel('READY')).toBe('Pare');
    expect(component.readinessLabel('TODO')).toBe('Pou konfigire');
    expect(component.readinessLabel('BLOCKED')).toBe('Pa disponib');
    expect(component.readinessLabel('UNKNOWN')).toBe('UNKNOWN');
  });

  it('translates readiness reasons and keeps plain backend text as fallback', () => {
    expect(
      component.readinessReason('admin.gamesPricing.readiness.reason.missingStakeOrPricing'),
    ).toBe('Miz oswa barèm manke.');
    expect(component.readinessReason('Configure stake first')).toBe('Configure stake first');
    expect(component.readinessReason(null)).toBeNull();
  });

  it('maps readiness states to shared console status tones', () => {
    expect(component.readinessTone('READY')).toBe('success');
    expect(component.readinessTone('TODO')).toBe('warning');
    expect(component.readinessTone('BLOCKED')).toBe('danger');
    expect(component.readinessTone('UNKNOWN')).toBe('neutral');
  });

  it('keeps draw availability navigation on the dedicated matrix route', () => {
    expect(component.availabilityRoute).toBe('/app/admin/games/channel-matrix');
  });
});
