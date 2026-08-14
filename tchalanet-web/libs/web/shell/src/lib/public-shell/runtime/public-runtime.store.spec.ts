import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { I18nFacade } from '@tch/core/i18n';
import { ThemeStore } from '@tch/ui/theme';
import { of } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';

import { PUBLIC_RUNTIME_I18N_CONFIG, PublicRuntimeStore } from './public-runtime.store';
import { PublicRuntimeInitializer } from './public-runtime-initializer';

describe('PublicRuntimeStore', () => {
  it('reloads public bootstrap when the active language changes', async () => {
    const currentLanguage = signal('fr');
    const i18n = {
      currentLanguage,
      init: vi.fn(),
    };
    const initializer = {
      initialize: vi.fn(() => of({})),
    };

    TestBed.configureTestingModule({
      providers: [
        PublicRuntimeStore,
        { provide: I18nFacade, useValue: i18n },
        { provide: ThemeStore, useValue: { init: vi.fn() } },
        { provide: PublicRuntimeInitializer, useValue: initializer },
        {
          provide: PUBLIC_RUNTIME_I18N_CONFIG,
          useValue: { languages: ['fr', 'en', 'ht'], defaultLanguage: 'fr' },
        },
      ],
    });

    const store = TestBed.inject(PublicRuntimeStore);
    store.init();
    await nextTask();

    currentLanguage.set('en');
    await nextTask();

    expect(initializer.initialize.mock.calls).toEqual([['fr'], ['en']]);
    expect(store.state()).toBe('ready');
  });
});

function nextTask(): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, 0));
}
