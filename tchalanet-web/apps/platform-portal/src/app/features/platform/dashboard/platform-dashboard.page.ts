import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { PageModelApi, PageModelComponent, PageRuntimeResponse } from '@tch/page-model';
import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { catchError, map, of, startWith } from 'rxjs';

type DashboardState =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly response: PageRuntimeResponse };

@Component({
  selector: 'tch-platform-dashboard-page',
  imports: [PageModelComponent, TchErrorPanel, TchLoading, TranslatePipe],
  templateUrl: './platform-dashboard.page.html',
  styleUrl: './platform-dashboard.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PlatformDashboardPage {
  private readonly pageModelApi = inject(PageModelApi);
  private readonly route = inject(ActivatedRoute);
  private readonly logicalId =
    typeof this.route.snapshot.data['pageModelLogicalId'] === 'string'
      ? this.route.snapshot.data['pageModelLogicalId']
      : 'private.dashboard.superadmin.ops';

  protected readonly state = toSignal(
    this.pageModelApi.getPlatformPage(this.logicalId).pipe(
      map(response => ({ status: 'ready', response }) as DashboardState),
      catchError(() =>
        this.pageModelApi.getPrivateFallbackPage().pipe(
          map(response => ({ status: 'ready', response }) as DashboardState),
          catchError(() => of({ status: 'error' } as DashboardState)),
        ),
      ),
      startWith({ status: 'loading' } as DashboardState),
    ),
    { initialValue: { status: 'loading' } as DashboardState },
  );
}
