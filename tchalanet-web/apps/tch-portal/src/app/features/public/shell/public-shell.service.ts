import { computed, inject, Injectable } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, of, shareReplay, tap } from 'rxjs';

import { PageModelApi, PageRuntimeResponse, PublicShellRuntime } from '@tch/page-model';
import { I18nFacade } from '../../../core/i18n';

@Injectable({ providedIn: 'root' })
export class PublicShellService {
  private readonly api = inject(PageModelApi);
  private readonly i18n = inject(I18nFacade);

  readonly page$ = this.api.getPublicPage().pipe(
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
    const { currentLang, supportedLangs } = response.meta;
    if (!supportedLangs?.length) return;
    this.i18n.setLanguages(
      supportedLangs.map(l => l.code),
      currentLang,
    );
  }
}
