import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '@ngx-translate/core';
import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { PageModelComponent, PageRuntimeResponse } from '@tch/page-model';
import { catchError, map, of, startWith } from 'rxjs';

import { PrivateShellService } from '../private-shell.service';

type DashboardState =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly response: PageRuntimeResponse };

@Component({
  selector: 'tch-private-dashboard-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [PageModelComponent, TranslatePipe, TchLoading, TchErrorPanel],
  templateUrl: './private-dashboard.page.html',
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
