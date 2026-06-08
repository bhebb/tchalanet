import { computed, inject, Injectable } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, of, shareReplay } from 'rxjs';

import { PageModelApi, PageRuntimeResponse, PublicShellRuntime } from '@tch/page-model';

@Injectable({ providedIn: 'root' })
export class PublicShellService {
  private readonly api = inject(PageModelApi);

  readonly page$ = this.api.getPublicPage().pipe(shareReplay(1));

  readonly page = toSignal<PageRuntimeResponse | undefined>(
    this.page$.pipe(catchError(() => of(undefined))),
    { initialValue: undefined },
  );

  readonly shell = computed<PublicShellRuntime | undefined>(() => {
    const shell = this.page()?.shell;
    return shell?.type === 'public' ? shell : undefined;
  });
}
