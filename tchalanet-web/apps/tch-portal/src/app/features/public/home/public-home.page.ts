import { ChangeDetectionStrategy, Component, inject, isDevMode } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '@ngx-translate/core';
import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { catchError, map, of, startWith } from 'rxjs';

import { PageModelComponent, PageRuntimeResponse } from '@tch/page-model';
import { ThemeSandboxComponent } from '../../dev/theme-sandbox.component';
import { PublicShellService } from '../shell/public-shell.service';

type PublicHomeState =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly response: PageRuntimeResponse };

@Component({
  selector: 'tch-public-home-page',
  imports: [PageModelComponent, TranslatePipe, TchLoading, TchErrorPanel, ThemeSandboxComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @defer (when devMode) {
      <tch-theme-sandbox />
    }

    @let vm = state();

    @switch (vm.status) {
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
        <tch-page-model [content]="vm.response.content" [dynamic]="vm.response.dynamic" />
      }
    }
  `,
})
export class PublicHomePage {
  private readonly shellSvc = inject(PublicShellService);

  protected readonly devMode = isDevMode();

  protected readonly state = toSignal(
    this.shellSvc.page$.pipe(
      map(response => ({ status: 'ready', response }) as PublicHomeState),
      catchError(() => of({ status: 'error' } as PublicHomeState)),
      startWith({ status: 'loading' } as PublicHomeState),
    ),
    { initialValue: { status: 'loading' } as PublicHomeState },
  );
}
