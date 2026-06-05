import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { catchError, map, of, startWith } from 'rxjs';

import { PageModelApi } from '../../core/pagemodel';
import { PageModelComponent } from '../pagemodel/page-model.component';
import { PageShellComponent } from '../pagemodel/shell/page-shell.component';
import { TchErrorPanel, TchLoading } from '@tch/shared/ui';

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
  imports: [PageShellComponent, PageModelComponent, TranslatePipe, RouterLink, TchLoading, TchErrorPanel],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
      <tch-page-shell [shell]="pageModel()?.shell">
          @switch (state().status) {
              @case ('loading') {
                  <tch-loading [label]="'common.loading' | translate"/>
              }
              @case ('error') {
                  <tch-error-panel
                          [title]="'common.error.title' | translate"
                          [message]="'public.home.loadError' | translate"
                  />
              }
              @case ('ready') {
                  <tch-page-model [pageModel]="pageModel()!" [dynamic]="dynamic()"/>
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

      .public-home__rules-link {
        display: grid;
        gap: 1rem;
        width: min(100% - 2 * var(--tch-page-margin-mobile, 16px), 1180px);
        margin: clamp(1.5rem, 5vw, 3rem) auto;
        padding: clamp(1rem, 4vw, 1.5rem);
        border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        border-radius: var(--tch-radius-xl, 24px);
        background: var(--tch-color-surface-container-low, var(--mat-sys-surface-container-low));
      }

      .public-home__rules-link p,
      .public-home__rules-link h2,
      .public-home__rules-link span {
        margin: 0;
      }

      .public-home__rules-link p {
        color: var(--tch-color-primary, var(--mat-sys-primary));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 800;
        text-transform: uppercase;
      }

      .public-home__rules-link h2 {
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
        font-size: var(--tch-font-size-headline-mobile, 1.5rem);
        line-height: var(--tch-line-height-headline-mobile, 2rem);
      }

      .public-home__rules-link div > span {
        display: block;
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
      }

      .public-home__rules-link a {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        gap: 0.5rem;
        min-height: var(--tch-touch-target, 48px);
        border-radius: var(--tch-radius-control, 8px);
        padding: 0 1rem;
        background: var(--tch-color-secondary-container, var(--mat-sys-secondary-container));
        color: var(--tch-color-on-secondary-container, var(--mat-sys-on-secondary-container));
        font-weight: 800;
        text-decoration: none;
      }

      @media (min-width: 760px) {
        .public-home__rules-link {
          grid-template-columns: minmax(0, 1fr) auto;
          align-items: center;
        }
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
