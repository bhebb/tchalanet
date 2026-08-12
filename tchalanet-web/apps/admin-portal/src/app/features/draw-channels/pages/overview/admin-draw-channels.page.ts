import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { mapHttpErrorToProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { TchErrorPanel, TchSectionError } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { AdminPageShellComponent } from '@tch/ui/console';
import { AdminRefreshButtonComponent } from '@tch/ui/console';
import { AdminEmptyStateComponent } from '@tch/ui/console';

import { AdminDrawChannelsApiService } from '../../data-access/admin-draw-channels-api.service';
import {
  DrawChannelProviderView,
} from '../../data-access/admin-draw-channels.models';
import { DrawChannelsSummaryComponent } from '../../components/draw-channels-summary/draw-channels-summary.component';
import { DrawChannelProviderCardComponent } from '../../components/draw-channel-provider-card/draw-channel-provider-card.component';

export function adminDrawChannelsErrorView(
  err: unknown,
  translate: (key: string) => string,
): ErrorViewModel {
  const problem = mapHttpErrorToProblemDetail(err);
  const normalized = webAppErrorFromProblemDetail(problem, 'admin.setup.draw_channels', 'page');
  const copy = resolveErrorFeedbackCopy(normalized, translate);
  return toErrorViewModel(normalized, copy);
}

type ActiveFilter = 'all' | 'active' | 'todo' | 'inactive' | 'error';
type PageState = 'loading' | 'ready' | 'error';

@Component({
  selector: 'tch-admin-draw-channels-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    MatButtonModule,
    TranslatePipe,
    AdminPageShellComponent,
    AdminRefreshButtonComponent,
    AdminEmptyStateComponent,
    TchErrorPanel,
    TchSectionError,
    DrawChannelsSummaryComponent,
    DrawChannelProviderCardComponent,
  ],
  templateUrl: './admin-draw-channels.page.html',
  styleUrls: ['./admin-draw-channels.page.scss'],
})
export class AdminDrawChannelsPage implements OnInit {
  private readonly api = inject(AdminDrawChannelsApiService);
  private readonly translate = inject(TranslateService);

  readonly pageState = signal<PageState>('loading');
  readonly pageError = signal<ErrorViewModel | null>(null);
  readonly actionNotice = signal<string | null>(null);
  readonly allProviders = signal<DrawChannelProviderView[]>([]);
  readonly activeFilter = signal<ActiveFilter>('all');
  readonly searchQuery = signal<string>('');

  readonly filteredProviders = computed(() => {
    const filter = this.activeFilter();
    const query = this.searchQuery().trim().toLowerCase();
    return this.allProviders()
      .filter(p => {
        if (filter === 'active') return p.tenantStatus === 'ACTIVE';
        if (filter === 'todo')
          return p.tenantStatus === 'INACTIVE' || p.tenantStatus === 'NEEDS_CONFIG';
        if (filter === 'inactive') return p.tenantStatus === 'INACTIVE';
        if (filter === 'error') return p.resultAcquisition.sourceStatus === 'ERROR';
        return true;
      })
      .filter(
        p =>
          !query ||
          p.providerLabel.toLowerCase().includes(query) ||
          p.providerCode.toLowerCase().includes(query),
      );
  });

  readonly filters: { key: ActiveFilter; labelKey: string }[] = [
    { key: 'all', labelKey: 'admin.drawChannels.filters.all' },
    { key: 'active', labelKey: 'admin.drawChannels.filters.active' },
    { key: 'todo', labelKey: 'admin.drawChannels.filters.todo' },
    { key: 'inactive', labelKey: 'admin.drawChannels.filters.inactive' },
    { key: 'error', labelKey: 'admin.drawChannels.filters.sourceError' },
  ];

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.pageState.set('loading');
    this.pageError.set(null);
    this.actionNotice.set(null);
    this.api.getDrawChannelProviders({ suppressShellFeedback: true }).subscribe({
      next: data => {
        this.allProviders.set(data);
        this.pageState.set('ready');
      },
      error: (err: unknown) => {
        this.pageError.set(this.errorViewModel(err));
        this.pageState.set('error');
      },
    });
  }

  private errorViewModel(err: unknown): ErrorViewModel {
    return adminDrawChannelsErrorView(err, key => this.translate.instant(key));
  }
}
