import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '@ngx-translate/core';
import { catchError, map, of, startWith } from 'rxjs';

import { PageModelApi } from '../../core/pagemodel';
import { PageModelComponent } from '../pagemodel/page-model.component';
import { PageShellComponent } from '../pagemodel/shell/page-shell.component';

type PublicHomeState =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | {
      readonly status: 'ready';
      readonly response: import('../../shared/types').PublicPageModelResponse;
    };

/**
 * Public home page. Loads the `public.home` PageModel from the backend and renders it through the
 * widget engine inside the public shell. Works without an authenticated session.
 */
@Component({
  selector: 'tch-public-home-page',
  imports: [PageShellComponent, PageModelComponent, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <tch-page-shell [shell]="pageModel()?.shell">
      @switch (state().status) {
        @case ('loading') {
          <p class="public-home__status">{{ 'common.loading' | translate }}</p>
        }
        @case ('error') {
          <p class="public-home__status">{{ 'public.home.loadError' | translate }}</p>
        }
        @case ('ready') {
          <tch-page-model [pageModel]="pageModel()!" [dynamic]="dynamic()" />
        }
      }
    </tch-page-shell>
  `,
  styles: [
    `
      .public-home__status {
        padding: 2rem;
        text-align: center;
        color: var(--tch-color-foreground, var(--mat-sys-on-surface));
      }
    `,
  ],
})
export class PublicHomePage {
  private readonly api = inject(PageModelApi);

  readonly state = toSignal(
    this.api.getPublicPage('public.home').pipe(
      map(response => ({ status: 'ready', response }) as PublicHomeState),
      catchError(() => of({ status: 'error' } as PublicHomeState)),
      startWith({ status: 'loading' } as PublicHomeState),
    ),
    { initialValue: { status: 'loading' } as PublicHomeState },
  );

  readonly pageModel = computed(() => {
    const s = this.state();
    return s.status === 'ready' ? s.response.pageModel : undefined;
  });

  readonly dynamic = computed(() => {
    const s = this.state();
    return s.status === 'ready' ? s.response.dynamic : undefined;
  });
}
