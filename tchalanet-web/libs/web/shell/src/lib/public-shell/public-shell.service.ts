import { computed, inject, Injectable } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import {
  catchError,
  distinctUntilChanged,
  filter,
  map,
  of,
  shareReplay,
  switchMap,
  tap,
} from 'rxjs';

import { I18nFacade } from '@tch/core/i18n';
import { PageModelApi, PageRuntimeResponse, PublicShellRuntime } from '@tch/page-model';

import { PublicFallbackBundleService } from './runtime/public-fallback-bundle.service';

@Injectable({ providedIn: 'root' })
export class PublicShellService {
  private readonly api = inject(PageModelApi);
  private readonly fallback = inject(PublicFallbackBundleService);
  private readonly i18n = inject(I18nFacade);
  private readonly language$ = toObservable(this.i18n.currentLanguage);

  readonly page$ = this.language$.pipe(
    filter(language => language.length > 0),
    distinctUntilChanged(),
    switchMap(language =>
      this.api.getPublicPage(language).pipe(
        catchError(() => this.fallback.load().pipe(map(b => b.pagePayload))),
      ),
    ),
    tap(response => this.hydrateI18n(response)),
    shareReplay(1),
  );

  readonly page = toSignal<PageRuntimeResponse | undefined>(
    this.page$.pipe(catchError(() => of(undefined))),
    { initialValue: undefined },
  );

  readonly shell = computed<PublicShellRuntime | undefined>(() => {
    const shell = this.page()?.shell;
    return shell?.type === 'public' ? shell : undefined;
  });

  private hydrateI18n(response: PageRuntimeResponse): void {
    const { supportedLangs } = response.meta;
    if (!supportedLangs?.length) return;
    this.i18n.setLanguages(
      supportedLangs.map(l => l.code),
      this.i18n.currentLanguage(),
    );
  }
}
