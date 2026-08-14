import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { TestBed } from '@angular/core/testing';
import { TranslateService, provideTranslateService } from '@ngx-translate/core';
import { describe, expect, it } from 'vitest';

import {
  DrawChannelListItemComponent,
  type DrawChannelListItemView,
} from '../../components/draw-channel-list-item/draw-channel-list-item.component';

describe(DrawChannelListItemComponent.name, () => {
  it('renders a draw-channel as a focused sales configuration row', () => {
    TestBed.configureTestingModule({
      imports: [DrawChannelListItemComponent],
      providers: [provideNoopAnimations(), provideTranslateService()],
    });
    const translate = TestBed.inject(TranslateService);
    translate.setTranslation('ht', translations);
    translate.use('ht');

    const fixture = TestBed.createComponent(DrawChannelListItemComponent);
    fixture.componentRef.setInput('row', {
      providerCode: 'TX',
      providerLabel: 'Texas',
      resultMode: 'AUTO',
      status: 'active',
      slot: {
        channelId: 'channel-ht-tx-1000',
        slotKey: 'HT_TX_1000',
        label: 'Texas · 1000',
        enabled: true,
        resultSlotActive: true,
        drawTime: '10:00:00',
        cutoffTime: '09:55:00',
        daysOfWeek: 'MON-SUN',
        defaultSource: 'EXTERNAL',
      },
    } satisfies DrawChannelListItemView);

    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Texas · 1000');
    expect(text).toContain('HT_TX_1000');
    expect(text).toContain('Rezilta: otomatik');
    expect(text).toContain('Lè tiraj:');
    expect(text).toContain('10:00');
    expect(text).toContain('Vant fèmen 09:55');
    expect(text).toContain('Jou ouvè: Chak jou');
    expect(text).toContain('Plafon ak blokaj');
    expect(text).not.toContain('Jwèt');
    expect(fixture.nativeElement.querySelector('.dc-channel-card__logo img')).not.toBeNull();
  });
});

const translations = {
  admin: {
    drawChannels: {
      actions: {
        configure: 'Konfigire',
        configureLimits: 'Plafon ak blokaj',
        viewDetails: 'Gade detay',
      },
      list: {
        days: {
          everyDay: 'Chak jou',
          label: 'Jou ouvè: {{value}}',
        },
        message: {
          active: 'Kanal sa a disponib pou vann.',
        },
        resultMode: {
          AUTO: 'Rezilta: otomatik',
        },
        status: {
          active: 'Aktif',
        },
      },
      slots: {
        cutoffTime: 'Vant fèmen {{value}}',
        drawTimeLabel: 'Lè tiraj',
        saleEnabled: 'Aktif',
        saleDisabled: 'Inaktif',
        toggle: {
          disable: 'Dezaktive',
          enable: 'Aktive',
        },
      },
    },
  },
  common: {
    edit: 'Modifye',
  },
};
