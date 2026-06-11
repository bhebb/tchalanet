import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '@ngx-translate/core';
import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { catchError, map, of, startWith } from 'rxjs';

import { PageModelApi, PageModelComponent, PageRuntimeResponse } from '@tch/page-model';

type PageState =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly response: PageRuntimeResponse };

@Component({
  selector: 'tch-public-managers-page',
  imports: [PageModelComponent, TranslatePipe, TchLoading, TchErrorPanel],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @switch (state().status) {
      @case ('loading') {
        <tch-loading [label]="'common.loading' | translate" />
      }
      @case ('error') {
        <tch-error-panel
          [title]="'common.error.title' | translate"
          [message]="'public.managers.loadError' | translate"
        />
      }
      @case ('ready') {
        <tch-page-model [content]="response()!.content" [dynamic]="response()!.dynamic" />
      }
    }
  `,
})
export class PublicManagersPage {
  private readonly api = inject(PageModelApi);

  readonly state = toSignal(
    this.api.getPublicManagersPage().pipe(
      map(response => ({ status: 'ready', response }) as PageState),
      catchError(() => of({ status: 'error' } as PageState)),
      startWith({ status: 'loading' } as PageState),
    ),
    { initialValue: { status: 'loading' } as PageState },
  );

  readonly response = computed(() => {
    const s = this.state();
    return s.status === 'ready' ? s.response : undefined;
  });
}
