import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
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
  imports: [PageShellComponent, PageModelComponent, TranslatePipe, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <tch-page-shell [shell]="pageModel()?.shell" [dynamic]="dynamic()">
      @switch (state().status) {
        @case ('loading') {
          <p class="public-home__status">{{ 'common.loading' | translate }}</p>
        }
        @case ('error') {
          <p class="public-home__status">{{ 'public.home.loadError' | translate }}</p>
        }
        @case ('ready') {
          <tch-page-model [pageModel]="pageModel()!" [dynamic]="dynamic()" />
          <section class="public-home__rules-link" aria-labelledby="public-home-rules-link-title">
            <div>
              <p>{{ 'public.home.rules_link.eyebrow' | translate }}</p>
              <h2 id="public-home-rules-link-title">{{ 'public.home.rules_link.title' | translate }}</h2>
              <span>{{ 'public.home.rules_link.description' | translate }}</span>
            </div>
            <a routerLink="/public/rules">
              <span class="material-symbols-outlined" aria-hidden="true">calculate</span>
              {{ 'public.home.rules_link.cta' | translate }}
            </a>
          </section>
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
