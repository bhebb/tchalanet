import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '@ngx-translate/core';
import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { PageModelComponent, PageRuntimeResponse } from '@tch/page-model';
import { catchError, map, of, startWith } from 'rxjs';

import { PrivateShellService } from '../private/shell/private-shell.service';

type DashboardState =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly response: PageRuntimeResponse };

@Component({
  selector: 'tch-private-dashboard-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [PageModelComponent, TranslatePipe, TchLoading, TchErrorPanel],
  template: `
    @let vm = state();

    @switch (vm.status) {
      @case ('loading') {
        <tch-loading [label]="'common.loading' | translate" />
      }
      @case ('error') {
        <tch-error-panel
          [title]="'common.error.title' | translate"
          [message]="'dashboard.loadError' | translate"
        />
      }
      @case ('ready') {
        <tch-page-model [content]="vm.response.content" [dynamic]="vm.response.dynamic" />
      }
    }
  `,
})
export class PrivateDashboardPage {
  private readonly shellSvc = inject(PrivateShellService);

  protected readonly state = toSignal(
    this.shellSvc.page$.pipe(
      map(response => ({ status: 'ready', response }) as DashboardState),
      catchError(() => of({ status: 'error' } as DashboardState)),
      startWith({ status: 'loading' } as DashboardState),
    ),
    { initialValue: { status: 'loading' } as DashboardState },
  );
}
