import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { TranslateService, provideTranslateService } from '@ngx-translate/core';
import { describe, expect, it, vi } from 'vitest';

import {
  ChannelGameSetupView,
  DrawChannelSetupView,
} from '../../data-access/admin-draw-sales-matrix-api.service';
import { DrawSalesMatrixGameCardComponent } from './matrix-game-card.component';

describe(DrawSalesMatrixGameCardComponent.name, () => {
  it('renders a compact matrix game row with only name, logo and switch for ready games', () => {
    TestBed.configureTestingModule({
      imports: [DrawSalesMatrixGameCardComponent],
      providers: [provideNoopAnimations(), provideRouter([]), provideTranslateService()],
    });
    const translate = TestBed.inject(TranslateService);
    translate.setTranslation('ht', translations);
    translate.use('ht');

    const fixture = TestBed.createComponent(DrawSalesMatrixGameCardComponent);
    fixture.componentRef.setInput('mode', 'offered');
    fixture.componentRef.setInput('game', gameFixture);
    fixture.componentRef.setInput('channel', channelFixture);

    fixture.detectChanges();

    const text = normalizedText(fixture.nativeElement.textContent as string);
    expect(text).toContain('Bolèt');
    expect(text).not.toContain('ON');
    expect(text).not.toContain('Pare');
    expect(text).not.toContain('Miz');
    expect(text).not.toContain('Barèm');
    expect(fixture.nativeElement.querySelector('.matrix-game__logo img')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.matrix-game__switch')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('tch-section-error')).toBeNull();
  });

  it('disables the matrix switch when the admin lacks game-pricing entitlement', () => {
    TestBed.configureTestingModule({
      imports: [DrawSalesMatrixGameCardComponent],
      providers: [provideNoopAnimations(), provideRouter([]), provideTranslateService()],
    });

    const fixture = TestBed.createComponent(DrawSalesMatrixGameCardComponent);
    fixture.componentRef.setInput('mode', 'offered');
    fixture.componentRef.setInput('game', gameFixture);
    fixture.componentRef.setInput('channel', channelFixture);
    fixture.componentRef.setInput('canManage', false);

    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.matrix-game__switch').disabled).toBe(true);
  });

  it('emits a toggle action for games already offered on the draw channel', () => {
    TestBed.configureTestingModule({
      imports: [DrawSalesMatrixGameCardComponent],
      providers: [provideNoopAnimations(), provideRouter([]), provideTranslateService()],
    });
    const fixture = TestBed.createComponent(DrawSalesMatrixGameCardComponent);
    const gameAction = vi.fn();
    fixture.componentInstance.gameAction.subscribe(gameAction);
    fixture.componentRef.setInput('mode', 'offered');
    fixture.componentRef.setInput('game', gameFixture);
    fixture.componentRef.setInput('channel', channelFixture);

    fixture.detectChanges();
    fixture.nativeElement.querySelector('.matrix-game__switch').click();

    expect(gameAction).toHaveBeenCalledWith({
      action: 'toggle',
      game: gameFixture,
    });
  });
});

function normalizedText(value: string): string {
  return value.replace(/\s+/g, ' ').trim();
}

const channelFixture: DrawChannelSetupView = {
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
};

const gameFixture: ChannelGameSetupView = {
  gameCode: 'HT_BOLET',
  tenantGameId: 'tenant-game-bolet',
  displayName: 'Bolèt',
  enabledForTenant: true,
  visibleInPos: true,
  offeredOnChannel: true,
  enabledOnChannel: true,
  minStake: 1,
  maxStake: 1_000_000,
  limits: {
    configured: true,
    assignments: [],
  },
  saleReady: true,
  warnings: [],
};

const translations = {
  admin: {
    drawSalesMatrix: {
      game: {
        action: {
          off: 'Dezaktive jwèt la sou tiraj sa a',
          on: 'Aktive jwèt la sou tiraj sa a',
        },
      },
    },
  },
};
