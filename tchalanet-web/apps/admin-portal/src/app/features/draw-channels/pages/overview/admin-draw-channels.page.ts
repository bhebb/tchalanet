import { SlicePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { mapHttpErrorToProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { TchErrorPanel, TchSectionError } from '@tch/ui/components';
import {
  AdminEmptyStateComponent,
  AdminPageShellComponent,
  AdminRefreshButtonComponent,
} from '@tch/ui/console';
import { AdminStatusPillComponent, AdminStatusTone } from '@tch/ui/console';
import { consoleLotteryProviderLogoUrl } from '@tch/web/console';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';

import { AdminDrawChannelsApiService } from '../../data-access/admin-draw-channels-api.service';
import {
  DrawChannelProviderView,
  DrawResultAcquisitionView,
  DrawChannelSlotConfigView,
} from '../../data-access/admin-draw-channels.models';
import { DrawChannelsSummaryComponent } from '../../components/draw-channels-summary/draw-channels-summary.component';
import { DrawChannelConfigDialog } from '../../components/draw-channel-config-dialog/draw-channel-config.dialog';

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

interface DrawChannelListRow {
  readonly providerCode: string;
  readonly providerLabel: string;
  readonly slot: DrawChannelSlotConfigView;
  readonly resultMode: 'AUTO' | 'MANUAL' | 'UNCONFIGURED';
  readonly status: 'active' | 'attention' | 'inactive';
  readonly gameCount: number | null;
}

@Component({
  selector: 'tch-admin-draw-channels-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    SlicePipe,
    MatButtonModule,
    TranslatePipe,
    AdminPageShellComponent,
    AdminRefreshButtonComponent,
    AdminStatusPillComponent,
    AdminEmptyStateComponent,
    TchErrorPanel,
    TchSectionError,
    DrawChannelsSummaryComponent,
  ],
  templateUrl: './admin-draw-channels.page.html',
  styleUrls: ['./admin-draw-channels.page.scss'],
})
export class AdminDrawChannelsPage implements OnInit {
  private readonly api = inject(AdminDrawChannelsApiService);
  private readonly dialog = inject(MatDialog);
  private readonly route = inject(ActivatedRoute);
  private readonly translate = inject(TranslateService);
  private handledDeepLinkKey: string | null = null;

  readonly pageState = signal<PageState>('loading');
  readonly pageError = signal<ErrorViewModel | null>(null);
  readonly actionNotice = signal<string | null>(null);
  readonly allProviders = signal<DrawChannelProviderView[]>([]);
  readonly savingChannelId = signal<string | null>(null);
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

  readonly channelRows = computed<DrawChannelListRow[]>(() =>
    this.allProviders().flatMap(provider =>
      provider.slots.map(slot => {
        const gameCount = slot.saleReadyGameCount ?? slot.offeredGameCount ?? null;
        return {
          providerCode: provider.providerCode,
          providerLabel: provider.providerLabel,
          slot,
          resultMode: this.resultMode(provider.resultAcquisition.mode),
          status: this.channelStatus(slot, gameCount),
          gameCount,
        };
      }),
    ),
  );

  readonly filteredRows = computed(() => {
    const filter = this.activeFilter();
    const query = this.searchQuery().trim().toLowerCase();

    return this.channelRows()
      .filter(row => {
        if (filter === 'active') return row.status === 'active';
        if (filter === 'todo') return row.status === 'attention';
        if (filter === 'inactive') return row.status === 'inactive';
        if (filter === 'error') return row.slot.resultSlotActive === false;
        return true;
      })
      .filter(
        row =>
          !query ||
          row.providerLabel.toLowerCase().includes(query) ||
          row.providerCode.toLowerCase().includes(query) ||
          row.slot.label.toLowerCase().includes(query) ||
          row.slot.slotKey.toLowerCase().includes(query),
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
        this.handleDeepLinkedChannel(data);
      },
      error: (err: unknown) => {
        this.pageError.set(this.errorViewModel(err));
        this.pageState.set('error');
      },
    });
  }

  toggleChannelEnabled(slot: DrawChannelSlotConfigView, enabled: boolean): void {
    if (!slot.channelId || this.savingChannelId()) return;
    this.savingChannelId.set(slot.channelId);
    this.actionNotice.set(null);
    this.api.setChannelActive(slot.channelId, enabled, { suppressShellFeedback: true }).subscribe({
      next: () => {
        this.savingChannelId.set(null);
        this.load();
      },
      error: (err: unknown) => {
        this.savingChannelId.set(null);
        this.pageError.set(this.errorViewModel(err));
        this.pageState.set('error');
      },
    });
  }

  openChannelConfig(slot: DrawChannelSlotConfigView): void {
    if (!slot.channelId) return;
    this.dialog
      .open(DrawChannelConfigDialog, {
        data: {
          channelId: slot.channelId,
          label: slot.label,
        },
        maxWidth: '640px',
        width: 'min(640px, 96vw)',
      })
      .afterClosed()
      .subscribe(saved => {
        if (saved === true) this.load();
      });
  }

  rowStatusLabelKey(status: DrawChannelListRow['status']): string {
    return `admin.drawChannels.list.status.${status}`;
  }

  rowStatusTone(status: DrawChannelListRow['status']): AdminStatusTone {
    if (status === 'active') return 'success';
    if (status === 'attention') return 'warning';
    return 'neutral';
  }

  resultModeLabelKey(mode: DrawChannelListRow['resultMode']): string {
    return `admin.drawChannels.list.resultMode.${mode}`;
  }

  rowMessageLabelKey(row: DrawChannelListRow): string {
    if (!row.slot.enabled) return 'admin.drawChannels.list.message.inactive';
    if (!row.slot.drawTime) return 'admin.drawChannels.list.message.missingSchedule';
    if (row.gameCount === 0) return 'admin.drawChannels.list.message.noGames';
    if (row.slot.resultSlotActive === false) return 'admin.drawChannels.list.message.sourceInactive';
    return 'admin.drawChannels.list.message.active';
  }

  providerLogoUrl(row: DrawChannelListRow): string | null {
    return consoleLotteryProviderLogoUrl(row.providerCode);
  }

  channelTitle(row: DrawChannelListRow): string {
    const slotLabel = row.slot.label.trim();
    return slotLabel || row.providerLabel;
  }

  private resultMode(mode: DrawResultAcquisitionView['mode']): DrawChannelListRow['resultMode'] {
    if (mode === 'AUTO') return 'AUTO';
    if (mode === 'MANUAL') return 'MANUAL';
    return 'UNCONFIGURED';
  }

  private channelStatus(
    slot: DrawChannelSlotConfigView,
    gameCount: number | null,
  ): DrawChannelListRow['status'] {
    if (!slot.enabled) return 'inactive';
    if (!slot.drawTime || slot.resultSlotActive === false || gameCount === 0) return 'attention';
    return 'active';
  }

  private handleDeepLinkedChannel(providers: readonly DrawChannelProviderView[]): void {
    const providerParam = this.route.snapshot.queryParamMap.get('provider')?.trim().toLowerCase();
    const slotParam = this.route.snapshot.queryParamMap.get('slot')?.trim().toLowerCase();
    if (!providerParam && !slotParam) return;

    const deepLinkKey = `${providerParam ?? ''}:${slotParam ?? ''}`;
    if (this.handledDeepLinkKey === deepLinkKey) return;

    const provider = providers.find(p =>
      !providerParam ||
      p.providerCode.toLowerCase() === providerParam ||
      p.providerLabel.toLowerCase().includes(providerParam)
    );
    const slot = provider?.slots.find(s =>
      !slotParam ||
      s.slotKey.toLowerCase() === slotParam ||
      s.channelId?.toLowerCase() === slotParam ||
      s.label.toLowerCase().includes(slotParam)
    );

    if (providerParam) this.searchQuery.set(provider?.providerLabel ?? providerParam);
    if (!slot) return;

    this.handledDeepLinkKey = deepLinkKey;
    queueMicrotask(() => this.openChannelConfig(slot));
  }

  private errorViewModel(err: unknown): ErrorViewModel {
    return adminDrawChannelsErrorView(err, key => this.translate.instant(key));
  }
}
