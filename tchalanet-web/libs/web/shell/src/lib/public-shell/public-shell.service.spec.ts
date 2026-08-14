import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { I18nFacade } from '@tch/core/i18n';
import { PageModelApi, PageRuntimeResponse } from '@tch/page-model';
import { of } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';

import { PublicShellService } from './public-shell.service';
import { PublicFallbackBundleService } from './runtime/public-fallback-bundle.service';

describe('PublicShellService', () => {
  it('reloads the public PageModel when the active language changes', async () => {
    const currentLanguage = signal('fr');
    const api = {
      getPublicPage: vi.fn((lang?: string) => of(pageResponse(lang ?? 'missing'))),
    };
    const i18n = {
      currentLanguage,
      setLanguages: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        PublicShellService,
        { provide: PageModelApi, useValue: api },
        { provide: I18nFacade, useValue: i18n },
        { provide: PublicFallbackBundleService, useValue: { load: vi.fn() } },
      ],
    });

    const service = TestBed.inject(PublicShellService);
    const seen: string[] = [];
    const subscription = service.page$.subscribe(response => seen.push(response.meta.slug));

    await nextTask();
    currentLanguage.set('en');
    await nextTask();

    expect(api.getPublicPage.mock.calls).toEqual([['fr'], ['en']]);
    expect(seen).toEqual(['fr', 'en']);

    subscription.unsubscribe();
  });
});

function pageResponse(lang: string): PageRuntimeResponse {
  return {
    meta: {
      logicalId: `public.home.${lang}`,
      scope: 'public',
      slug: lang,
      schemaVersion: 2,
      supportedLangs: [
        { code: 'fr', label: 'Français', shortLabel: 'FR' },
        { code: 'en', label: 'English', shortLabel: 'EN' },
      ],
    },
    shell: {
      type: 'public',
      header: {},
      footer: {},
    },
    content: {
      layout: { rows: [] },
      widgets: {},
    },
    dynamic: {
      widgets: {},
      errors: [],
    },
  };
}

function nextTask(): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, 0));
}
