import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { webAppErrorFromProblemDetail } from '@tch/api';
import type { ProblemDetail } from '@tch/api';
import { TchErrorPanel, TchSectionError } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { AdminPageShellComponent } from '@tch/ui/console';
import { AdminEmptyStateComponent } from '@tch/ui/console';

import { AdminDrawChannelsApiService } from '../../data-access/admin-draw-channels-api.service';
import { DrawChannelProviderView, DrawChannelSlotConfigView } from '../../data-access/admin-draw-channels.models';
import { DrawChannelsSummaryComponent } from '../../components/draw-channels-summary/draw-channels-summary.component';
import { DrawChannelProviderCardComponent } from '../../components/draw-channel-provider-card/draw-channel-provider-card.component';
import { DrawChannelConfigDialog } from '../../components/dialogs/draw-channel-config.dialog';

type ActiveFilter = 'all' | 'active' | 'todo' | 'inactive' | 'error';
type PageState = 'loading' | 'ready' | 'error';

@Component({
  selector: 'tch-admin-draw-channels-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    MatButtonModule,
    MatDialogModule,
    TranslatePipe,
    AdminPageShellComponent,
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
  private readonly api      = inject(AdminDrawChannelsApiService);
  private readonly route    = inject(ActivatedRoute);
  private readonly dialog   = inject(MatDialog);
  private readonly translate = inject(TranslateService);
  private pendingConfigure: { providerCode: string; slotKey: string | null } | null = null;

  readonly pageState    = signal<PageState>('loading');
  readonly pageError    = signal<ErrorViewModel | null>(null);
  readonly actionNotice = signal<string | null>(null);
  readonly allProviders = signal<DrawChannelProviderView[]>([]);
  readonly activeFilter = signal<ActiveFilter>('all');
  readonly searchQuery  = signal<string>('');

  readonly filteredProviders = computed(() => {
    const filter = this.activeFilter();
    const query  = this.searchQuery().trim().toLowerCase();
    return this.allProviders()
      .filter(p => {
        if (filter === 'active')   return p.tenantStatus === 'ACTIVE';
        if (filter === 'todo')     return p.tenantStatus === 'INACTIVE' || p.tenantStatus === 'NEEDS_CONFIG';
        if (filter === 'inactive') return p.tenantStatus === 'INACTIVE';
        if (filter === 'error')    return p.resultAcquisition.sourceStatus === 'ERROR';
        return true;
      })
      .filter(p =>
        !query ||
        p.providerLabel.toLowerCase().includes(query) ||
        p.providerCode.toLowerCase().includes(query),
      );
  });

  readonly filters: { key: ActiveFilter; labelKey: string }[] = [
    { key: 'all',      labelKey: 'admin.drawChannels.filters.all' },
    { key: 'active',   labelKey: 'admin.drawChannels.filters.active' },
    { key: 'todo',     labelKey: 'admin.drawChannels.filters.todo' },
    { key: 'inactive', labelKey: 'admin.drawChannels.filters.inactive' },
    { key: 'error',    labelKey: 'admin.drawChannels.filters.sourceError' },
  ];

  ngOnInit(): void {
    const providerCode = this.route.snapshot.queryParamMap.get('provider');
    if (providerCode) {
      this.pendingConfigure = {
        providerCode,
        slotKey: this.route.snapshot.queryParamMap.get('slot'),
      };
      this.searchQuery.set(providerCode);
    }
    this.load();
  }

  load(): void {
    this.pageState.set('loading');
    this.pageError.set(null);
    this.actionNotice.set(null);
    this.api.getDrawChannelProviders().subscribe({
      next: data => {
        this.allProviders.set(data);
        this.pageState.set('ready');
        this.openPendingConfigure(data);
      },
      error: (err: unknown) => {
        this.pageError.set(this.errorViewModel(err));
        this.pageState.set('error');
      },
    });
  }

  onConfigure(provider: DrawChannelProviderView): void {
    this.openConfigDialog(provider, null);
  }

  onConfigureSlot(provider: DrawChannelProviderView, slot: DrawChannelSlotConfigView): void {
    this.openConfigDialog(provider, slot);
  }

  private errorViewModel(err: unknown): ErrorViewModel {
    const problem = (err as { error?: ProblemDetail })?.error;
    if (problem) {
      const normalized = webAppErrorFromProblemDetail(problem, 'admin.setup.draw_channels', 'page');
      const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
      return toErrorViewModel(normalized, copy);
    }

    return {
      title: this.translate.instant('common.errors.fallback.title'),
      message: this.translate.instant('common.errors.fallback.message'),
      severity: 'error',
    };
  }

  private openPendingConfigure(providers: readonly DrawChannelProviderView[]): void {
    const pending = this.pendingConfigure;
    if (!pending) return;
    this.pendingConfigure = null;
    const provider = providers.find(item => item.providerCode === pending.providerCode);
    if (!provider) return;
    const slot = pending.slotKey
      ? provider.slots.find(item => item.slotKey === pending.slotKey) ?? null
      : null;
    this.openConfigDialog(provider, slot);
  }

  private openConfigDialog(provider: DrawChannelProviderView, slot: DrawChannelSlotConfigView | null): void {
    this.dialog.open(DrawChannelConfigDialog, {
      width: 'min(760px, calc(100vw - 2rem))',
      maxWidth: 'calc(100vw - 2rem)',
      data: { provider, slot },
    }).afterClosed().subscribe((updated: DrawChannelProviderView | undefined) => {
      if (!updated) return;
      this.allProviders.update(current => current.map(item =>
        item.providerCode === updated.providerCode ? updated : item,
      ));
      this.actionNotice.set(this.translate.instant('admin.drawChannels.notice.saved', {
        provider: updated.providerLabel,
      }));
    });
  }
}
