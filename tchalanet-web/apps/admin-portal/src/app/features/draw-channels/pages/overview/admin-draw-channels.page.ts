import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  signal,
} from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { TchConfirmDialog, type TchConfirmDialogData, TchNotice } from '@tch/ui/components';
import {
  AdminEmptyStateComponent,
  AdminPageShellComponent,
  AdminRefreshButtonComponent,
} from '@tch/ui/console';
import {
  TchAsyncReadyDirective,
  TchAsyncViewComponent,
  resourceErrorVm,
  tchMutation,
} from '@tch/web/async';

import { AdminDrawChannelsApiService } from '../../data-access/admin-draw-channels-api.service';
import {
  type DrawChannelProviderView,
  type DrawChannelSlotConfigView,
} from '../../data-access/admin-draw-channels.models';
import { DrawChannelsSummaryComponent } from '../../components/draw-channels-summary/draw-channels-summary.component';
import { DrawChannelConfigDialog } from '../../components/draw-channel-config-dialog/draw-channel-config.dialog';
import {
  DrawChannelListItemComponent,
  type DrawChannelListItemView,
  type DrawChannelListResultMode,
  type DrawChannelListStatus,
} from '../../components/draw-channel-list-item/draw-channel-list-item.component';

type ActiveFilter = 'all' | 'active' | 'todo' | 'inactive' | 'error';

interface ToggleChannelInput {
  readonly slot: DrawChannelSlotConfigView;
  readonly enabled: boolean;
}

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
    TchNotice,
    TchAsyncReadyDirective,
    TchAsyncViewComponent,
    DrawChannelsSummaryComponent,
    DrawChannelListItemComponent,
  ],
  templateUrl: './admin-draw-channels.page.html',
  styleUrls: ['./admin-draw-channels.page.scss'],
})
export class AdminDrawChannelsPage {
  private readonly api = inject(AdminDrawChannelsApiService);
  private readonly dialog = inject(MatDialog);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly translate = inject(TranslateService);
  private handledDeepLinkKey: string | null = null;
  private readonly queryParamMap = toSignal(this.route.queryParamMap, {
    initialValue: this.route.snapshot.queryParamMap,
  });

  readonly providersResource = this.api.getDrawChannelProvidersResource({
    suppressShellFeedback: true,
  });
  readonly fromSetup = computed(() => this.queryParamMap().get('from') === 'setup');
  readonly setupReturnFragment = 'setup-required-title';
  readonly providersError = resourceErrorVm(this.providersResource, 'admin.setup.draw_channels');
  readonly allProviders = computed(() => this.providersResource.value() ?? []);
  readonly activeFilter = signal<ActiveFilter>('all');
  readonly searchQuery = signal<string>('');
  readonly toggleChannel = tchMutation<ToggleChannelInput, unknown>({
    run: input =>
      this.api.setChannelActive(input.slot.channelId ?? '', input.enabled, {
        suppressShellFeedback: true,
      }),
    source: 'admin.setup.draw_channels.toggle',
    onSuccess: () => this.load(true),
  });

  readonly channelRows = computed<DrawChannelListItemView[]>(() =>
    this.allProviders()
      .flatMap(provider =>
        provider.slots.map(slot => {
          return {
            providerCode: provider.providerCode,
            providerLabel: provider.providerLabel,
            slot,
            resultMode: this.slotResultMode(slot),
            status: this.channelStatus(slot),
          };
        }),
      )
      .sort(compareDrawChannelRows),
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

  constructor() {
    effect(() => {
      const providers = this.providersResource.value();
      if (providers) this.handleDeepLinkedChannel(providers);
    });
  }

  load(preserveActionFeedback = false): void {
    if (!preserveActionFeedback) this.toggleChannel.clearFeedback();
    this.providersResource.reload();
  }

  toggleChannelEnabled(slot: DrawChannelSlotConfigView, enabled: boolean): void {
    if (!slot.channelId) return;
    if (!enabled) {
      this.confirmDisableChannel(slot);
      return;
    }
    if (this.activationBlocked(slot)) {
      this.openChannelDialog(slot);
      return;
    }
    this.toggleChannel.execute({ slot, enabled }, { key: slot.channelId });
  }

  openChannelConfig(slot: DrawChannelSlotConfigView): void {
    this.openChannelDialog(slot);
  }

  openChannelDetails(slot: DrawChannelSlotConfigView): void {
    if (!slot.channelId) return;
    void this.router.navigate(['/app/admin/draw-channels', slot.channelId], {
      queryParams: this.setupFlowQueryParams(),
    });
  }

  openChannelLimits(slot: DrawChannelSlotConfigView): void {
    if (!slot.channelId) return;
    void this.router.navigate(['/app/admin/draw-channels', slot.channelId], {
      queryParams: this.setupFlowQueryParams(),
      fragment: 'limits',
    });
  }

  channelSaving(slot: DrawChannelSlotConfigView): boolean {
    return !!slot.channelId && this.toggleChannel.pending(slot.channelId);
  }

  private openChannelDialog(slot: DrawChannelSlotConfigView): void {
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

  private confirmDisableChannel(slot: DrawChannelSlotConfigView): void {
    const channelId = slot.channelId;
    if (!channelId) return;
    this.dialog
      .open<TchConfirmDialog, TchConfirmDialogData, { confirmed: boolean }>(TchConfirmDialog, {
        data: {
          title: this.translate.instant('admin.drawChannels.confirm.disableTitle', {
            name: slot.label,
          }),
          message: this.translate.instant('admin.drawChannels.confirm.disableMessage'),
          confirmLabel: this.translate.instant('admin.drawChannels.confirm.disableAction'),
          cancelLabel: this.translate.instant('common.cancel'),
          destructive: true,
          icon: 'block',
        },
      })
      .afterClosed()
      .subscribe(result => {
        if (result?.confirmed) {
          this.toggleChannel.execute({ slot, enabled: false }, { key: channelId });
        }
      });
  }

  private slotResultMode(slot: DrawChannelSlotConfigView): DrawChannelListResultMode {
    if (slot.resultSlotActive === false) return 'UNCONFIGURED';
    if (slot.defaultSource === 'MANUAL') return 'MANUAL';
    if (slot.defaultSource) return 'AUTO';
    return 'UNCONFIGURED';
  }

  private channelStatus(slot: DrawChannelSlotConfigView): DrawChannelListStatus {
    if (!slot.enabled) return 'inactive';
    if (!slot.drawTime || slot.resultSlotActive === false) return 'attention';
    return 'active';
  }

  private activationBlocked(slot: DrawChannelSlotConfigView): boolean {
    return !slot.drawTime || slot.resultSlotActive === false;
  }

  private setupFlowQueryParams(): Record<string, string> | undefined {
    return this.fromSetup() ? { from: 'setup' } : undefined;
  }

  private handleDeepLinkedChannel(providers: readonly DrawChannelProviderView[]): void {
    const providerParam = this.route.snapshot.queryParamMap.get('provider')?.trim().toLowerCase();
    const slotParam = this.route.snapshot.queryParamMap.get('slot')?.trim().toLowerCase();
    if (!providerParam && !slotParam) return;

    const deepLinkKey = `${providerParam ?? ''}:${slotParam ?? ''}`;
    if (this.handledDeepLinkKey === deepLinkKey) return;

    const provider = providers.find(
      p =>
        !providerParam ||
        p.providerCode.toLowerCase() === providerParam ||
        p.providerLabel.toLowerCase().includes(providerParam),
    );
    const slot = provider?.slots.find(
      s =>
        !slotParam ||
        s.slotKey.toLowerCase() === slotParam ||
        s.channelId?.toLowerCase() === slotParam ||
        s.label.toLowerCase().includes(slotParam),
    );

    if (providerParam) this.searchQuery.set(provider?.providerLabel ?? providerParam);
    if (!slot) return;

    this.handledDeepLinkKey = deepLinkKey;
    queueMicrotask(() => this.openChannelConfig(slot));
  }
}

function compareDrawChannelRows(a: DrawChannelListItemView, b: DrawChannelListItemView): number {
  const byActive = Number(b.slot.enabled) - Number(a.slot.enabled);
  if (byActive !== 0) return byActive;
  const byTime = (a.slot.drawTime ?? '').localeCompare(b.slot.drawTime ?? '');
  if (byTime !== 0) return byTime;
  return (a.slot.label || a.providerLabel).localeCompare(b.slot.label || b.providerLabel);
}
