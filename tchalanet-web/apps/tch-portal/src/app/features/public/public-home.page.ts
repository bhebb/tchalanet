import { ChangeDetectionStrategy, Component, computed, inject, isDevMode } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '@ngx-translate/core';
import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { catchError, map, of, startWith } from 'rxjs';

import { PageModelComponent, PageRuntimeResponse } from '@tch/page-model';
import { PublicShellComponent } from './shell/public-shell.component';
import { PublicShellService } from './shell/public-shell.service';
// Dev-only theme sandbox: referenced exclusively inside the @defer block below, so Angular
// code-splits it (and its Material modules) into a lazy chunk that prod never loads.
import { ThemeSandboxComponent } from '../dev/theme-sandbox.component';

type PublicHomeState =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly response: PageRuntimeResponse };

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
    @defer (when devMode) {
      <tch-theme-sandbox />
    }
    <tch-page-shell>
      @switch (state().status) {
        @case ('loading') {
          <tch-loading [label]="'common.loading' | translate" />
        }
        @case ('error') {
          <tch-error-panel
            [title]="'common.error.title' | translate"
            [message]="'public.home.loadError' | translate"
          />
        }
        @case ('ready') {
          <tch-page-model [content]="response()!.content" [dynamic]="response()!.dynamic" />
        }
      }
    </tch-page-shell>
  `,
})
export class PublicHomePage {
  private readonly shellSvc = inject(PublicShellService);
  protected readonly devMode = isDevMode();

  readonly state = toSignal(
    this.shellSvc.page$.pipe(
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
}
