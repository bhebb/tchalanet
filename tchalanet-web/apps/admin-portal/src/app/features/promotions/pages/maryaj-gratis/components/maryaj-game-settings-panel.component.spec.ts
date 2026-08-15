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
                'admin.maryajGratis.game.notDefined': 'Pa defini',
                'admin.maryajGratis.game.readiness.READY': 'Pare',
                'common.not_available': 'Pa disponib',
              })[key] ?? key,
          },
        },
      ],
    });
    component = TestBed.runInInjectionContext(() => new MaryajGameSettingsPanelComponent());
  });

  it('formats stake amounts with currency and readable grouping', () => {
    expect(component.formatAmount(10000000, 'HTG')).toBe('10 000 000 HTG');
    expect(component.formatAmount(null, 'HTG')).toBe('Pa defini');
  });

  it('uses business labels for known Maryaj payout variants', () => {
    expect(component.gainOptionLabel('MARRIAGE_EXACT_ORDER', 'Exact order')).toBe('Egzak');
    expect(component.gainOptionLabel('MARRIAGE_REVERSE_ALLOWED', 'Reverse allowed')).toBe(
      'Reverse',
    );
    expect(component.gainOptionLabel('OTHER', 'Custom')).toBe('Custom');
  });

  it('does not leak raw readiness translation keys for unknown statuses', () => {
    expect(component.readinessLabel('READY')).toBe('Pare');
    expect(component.readinessLabel('UNKNOWN')).toBe('UNKNOWN');
  });
});
