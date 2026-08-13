import { SlicePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { AdminStatusPillComponent, type AdminStatusTone } from '@tch/ui/console';
import { consoleLotteryProviderLogoUrl } from '@tch/web/console';

import { type DrawChannelSlotConfigView } from '../../data-access/admin-draw-channels.models';

export type DrawChannelListResultMode = 'AUTO' | 'MANUAL' | 'UNCONFIGURED';
export type DrawChannelListStatus = 'active' | 'attention' | 'inactive';

export interface DrawChannelListItemView {
  readonly providerCode: string;
  readonly providerLabel: string;
  readonly slot: DrawChannelSlotConfigView;
  readonly resultMode: DrawChannelListResultMode;
  readonly status: DrawChannelListStatus;
}

@Component({
  selector: 'tch-draw-channel-list-item',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [SlicePipe, MatButtonModule, TranslatePipe, AdminStatusPillComponent],
  templateUrl: './draw-channel-list-item.component.html',
  styleUrls: ['./draw-channel-list-item.component.scss'],
})
export class DrawChannelListItemComponent {
  private readonly translate = injectTranslate();

  readonly row = input.required<DrawChannelListItemView>();
  readonly saving = input(false);

  readonly configure = output<DrawChannelSlotConfigView>();
  readonly details = output<DrawChannelSlotConfigView>();
  readonly toggleEnabled = output<boolean>();

  readonly providerLogoUrl = computed(() => consoleLotteryProviderLogoUrl(this.row().providerCode));

  statusLabelKey(status: DrawChannelListStatus): string {
    return `admin.drawChannels.list.status.${status}`;
  }

  statusTone(status: DrawChannelListStatus): AdminStatusTone {
    if (status === 'active') return 'success';
    if (status === 'attention') return 'warning';
    return 'neutral';
  }

  resultModeLabelKey(mode: DrawChannelListResultMode): string {
    return `admin.drawChannels.list.resultMode.${mode}`;
  }

  messageLabelKey(row: DrawChannelListItemView): string {
    if (!row.slot.enabled) return 'admin.drawChannels.list.message.inactive';
    if (!row.slot.drawTime) return 'admin.drawChannels.list.message.missingSchedule';
    if (row.slot.resultSlotActive === false)
      return 'admin.drawChannels.list.message.sourceInactive';
    return 'admin.drawChannels.list.message.active';
  }

  channelTitle(row: DrawChannelListItemView): string {
    return row.slot.label.trim() || row.providerLabel;
  }

  daysOfWeekLabel(daysOfWeek: string | null | undefined): string {
    const parts = expandDaysOfWeek(daysOfWeek);
    if (parts.length === 0 || parts.length === 7) {
      return this.translate('admin.drawChannels.list.days.everyDay');
    }
    return parts.map(day => this.translate(`admin.drawChannels.daysShort.${day}`)).join(', ');
  }
}

function injectTranslate(): (key: string) => string {
  const translate = inject(TranslateService);
  return key => translate.instant(key);
}

function expandDaysOfWeek(daysOfWeek: string | null | undefined): readonly string[] {
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

function daysInRange(
  startToken: string | undefined,
  endToken: string | undefined,
): readonly DayCode[] {
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
