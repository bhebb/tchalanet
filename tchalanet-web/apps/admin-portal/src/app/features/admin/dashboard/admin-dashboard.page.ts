import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '@ngx-translate/core';
import { PageModelApi, PageModelComponent, PageRuntimeResponse } from '@tch/page-model';
import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { catchError, map, of, startWith } from 'rxjs';

type DashboardState =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly response: PageRuntimeResponse };

@Component({
  selector: 'tch-admin-dashboard-page',
  imports: [PageModelComponent, TchErrorPanel, TchLoading, TranslatePipe],
  templateUrl: './admin-dashboard.page.html',
  styleUrl: './admin-dashboard.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminDashboardPage {
  private readonly pageModelApi = inject(PageModelApi);

  protected readonly state = toSignal(
    this.pageModelApi.getTenantPage().pipe(
      map(response => ({ status: 'ready', response }) as DashboardState),
      catchError(() => of({ status: 'error' } as DashboardState)),
      startWith({ status: 'loading' } as DashboardState),
    ),
    { initialValue: { status: 'loading' } as DashboardState },
  );
}
