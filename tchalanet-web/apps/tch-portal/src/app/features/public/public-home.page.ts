import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '@ngx-translate/core';
import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { catchError, map, of, startWith } from 'rxjs';

import { PageModelApi, PageModelComponent, PageRuntimeResponse, PublicShellRuntime } from '@tch/page-model';
import { PublicShellComponent } from './shell/public-shell.component';
import { ThemeSandboxComponent } from './theme-sandbox.component'; // dev-only theme sandbox (floating, isDevMode)

type PublicHomeState =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | {
      readonly status: 'ready';
      readonly response: PageRuntimeResponse;
    };

/**
 * Public home page. Loads the `public.home` PageModel from the backend and renders it through the
 * widget engine inside the public shell. Works without an authenticated session.
 */
@Component({
  selector: 'tch-public-home-page',
  imports: [
    PublicShellComponent,
    PageModelComponent,
    TranslatePipe,
    TchLoading,
    TchErrorPanel,
    ThemeSandboxComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
      <!-- Dev-only floating theme sandbox (no-op in prod via isDevMode) -->
      <tch-theme-sandbox />
      <tch-page-shell [shell]="shell()">
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
                  <tch-page-model [content]="response()!.content" [dynamic]="response()!.dynamic"/>
              }
          }
      </tch-page-shell>
  `,
})
export class PublicHomePage {
  private readonly api = inject(PageModelApi);

  readonly state = toSignal(
    this.api.getPublicPage().pipe(
      map(response => ({ status: 'ready', response }) as PublicHomeState),
      catchError(() => of({ status: 'error' } as PublicHomeState)),
      startWith({ status: 'loading' } as PublicHomeState),
    ),
    { initialValue: { status: 'loading' } as PublicHomeState },
  );

  readonly response = computed(() => {
    const s = this.state();
    return s.status === 'ready' ? s.response : undefined;
  });

  readonly shell = computed<PublicShellRuntime | undefined>(() => {
    const shell = this.response()?.shell;
    return shell?.type === 'public' ? shell : undefined;
  });
}
