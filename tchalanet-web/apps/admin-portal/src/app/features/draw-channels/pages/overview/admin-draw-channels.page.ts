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
    if (row.slot.resultSlotActive === false) return 'admin.drawChannels.list.message.sourceInactive';
    return 'admin.drawChannels.list.message.active';
  }

  channelTitle(row: DrawChannelListRow): string {
    const slotLabel = row.slot.label.trim();
    return slotLabel || row.providerLabel;
  }

  providerLogoUrl(row: DrawChannelListRow): string | null {
    return consoleLotteryProviderLogoUrl(row.providerCode);
  }

  daysOfWeekLabel(daysOfWeek: string | null | undefined): string {
    const parts = this.expandDaysOfWeek(daysOfWeek);
    if (parts.length === 0 || parts.length === 7) {
      return this.translate.instant('admin.drawChannels.list.days.everyDay');
    }
    return parts
      .map(day => this.translate.instant(`admin.drawChannels.daysShort.${day}`))
      .join(', ');
  }

  private slotResultMode(slot: DrawChannelSlotConfigView): DrawChannelListRow['resultMode'] {
    if (slot.resultSlotActive === false) return 'UNCONFIGURED';
    if (slot.defaultSource === 'MANUAL') return 'MANUAL';
    if (slot.defaultSource) return 'AUTO';
    return 'UNCONFIGURED';
  }

  private channelStatus(slot: DrawChannelSlotConfigView): DrawChannelListRow['status'] {
    if (!slot.enabled) return 'inactive';
    if (!slot.drawTime || slot.resultSlotActive === false) return 'attention';
    return 'active';
  }

  private expandDaysOfWeek(daysOfWeek: string | null | undefined): readonly string[] {
    const value = daysOfWeek?.trim().toUpperCase();
    if (!value) return [];
    if (value === 'MON-SUN') return DAY_ORDER;

    const days = new Set<string>();
    for (const part of value.split(',')) {
      const trimmed = part.trim();
      if (!trimmed) continue;
      if (trimmed.includes('-')) {
        const [start, end] = trimmed.split('-').map(token => token.trim());
        for (const day of daysInRange(start, end)) days.add(day);
      } else {
        const day = dayToken(trimmed);
        if (day) days.add(day);
      }
    }
    return DAY_ORDER.filter(day => days.has(day));
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

const DAY_ORDER = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
] as const;

type DayCode = (typeof DAY_ORDER)[number];

const DAY_TOKEN_TO_CODE: Record<string, DayCode> = {
  MON: 'MONDAY',
  TUE: 'TUESDAY',
  WED: 'WEDNESDAY',
  THU: 'THURSDAY',
  FRI: 'FRIDAY',
  SAT: 'SATURDAY',
  SUN: 'SUNDAY',
};

function dayToken(token: string): DayCode | null {
  return DAY_TOKEN_TO_CODE[token.slice(0, 3)] ?? null;
}

function daysInRange(startToken: string | undefined, endToken: string | undefined): readonly DayCode[] {
  const start = dayToken(startToken ?? '');
  const end = dayToken(endToken ?? '');
  if (!start || !end) return [];
  const startIndex = DAY_ORDER.indexOf(start);
  const endIndex = DAY_ORDER.indexOf(end);
  const result: DayCode[] = [];
  for (let index = startIndex; ; index = (index + 1) % DAY_ORDER.length) {
    result.push(DAY_ORDER[index]);
    if (index === endIndex) break;
  }
  return result;
}

function compareDrawChannelRows(a: DrawChannelListRow, b: DrawChannelListRow): number {
  const byActive = Number(b.slot.enabled) - Number(a.slot.enabled);
  if (byActive !== 0) return byActive;
  const byTime = (a.slot.drawTime ?? '').localeCompare(b.slot.drawTime ?? '');
  if (byTime !== 0) return byTime;
  return (a.slot.label || a.providerLabel).localeCompare(b.slot.label || b.providerLabel);
}
