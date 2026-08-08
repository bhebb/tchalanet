import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';

import {
  ProviderMatrixView,
  SlotMatrixView,
} from '../../data-access/admin-draw-sales-matrix-api.service';

export interface DrawChannelFilterValue {
  readonly provider: string;
  readonly slotKey: string;
}

interface DrawChannelFilterOption extends DrawChannelFilterValue {
  readonly key: string;
  readonly label: string;
  readonly hint: string;
}

@Component({
  selector: 'tch-draw-channel-filter-select',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatFormFieldModule, MatSelectModule],
  templateUrl: './draw-channel-filter-select.component.html',
  styleUrls: ['./draw-channel-filter-select.component.scss'],
})
export class DrawChannelFilterSelectComponent {
  readonly providers = input.required<readonly ProviderMatrixView[]>();
  readonly provider = input('');
  readonly slotKey = input('');
  readonly label = input('Tirage');
  readonly allLabel = input('Tous les tirages');

  readonly selectionChange = output<DrawChannelFilterValue>();

  protected readonly options = computed<readonly DrawChannelFilterOption[]>(() =>
    this.providers()
      .flatMap(provider =>
        provider.slots
          .filter(slot => slot.channel?.active || slot.slotReady)
          .map(slot => toOption(provider.providerCode, slot)),
      )
      .sort((a, b) => a.label.localeCompare(b.label)),
  );

  protected readonly selectedKey = computed(() => {
    const provider = this.provider().trim().toUpperCase();
    const slotKey = this.slotKey().trim().toUpperCase();
    return provider && slotKey ? optionKey(provider, slotKey) : '';
  });

  protected onValueChange(key: string): void {
    const option = this.options().find(item => item.key === key);
    this.selectionChange.emit({
      provider: option?.provider ?? '',
      slotKey: option?.slotKey ?? '',
    });
  }
}

function toOption(provider: string, slot: SlotMatrixView): DrawChannelFilterOption {
  const providerCode = provider.trim().toUpperCase();
  const slotKey = slot.slotKey.trim().toUpperCase();
  const channelCode = slot.channel?.channelCode?.trim().toUpperCase() ?? '';
  const slotLabel = slotDisplayLabel(providerCode, channelCode, slotKey);

  return {
    provider: providerCode,
    slotKey,
    key: optionKey(providerCode, slotKey),
    label: `${providerCode}-${slotLabel}`,
    hint: channelCode || slotKey,
  };
}

function optionKey(provider: string, slotKey: string): string {
  return `${provider}::${slotKey}`;
}

function slotDisplayLabel(provider: string, channelCode: string, slotKey: string): string {
  const raw = channelCode || slotKey;
  const normalized = raw
    .replace(new RegExp(`^${provider}[_-]?`, 'i'), '')
    .replace(/^.*[_-]/, '')
    .toUpperCase();

  if (normalized === 'MID' || normalized === 'MIDI' || normalized === 'MIDDAY') return 'Midi';
  if (normalized === 'EVE' || normalized === 'EVENING' || normalized === 'SOIR') return 'Soir';
  if (normalized === 'MORN' || normalized === 'MORNING') return 'Matin';
  if (normalized === 'LATE') return 'Late';
  if (normalized === 'DAY') return 'Jour';
  if (normalized === 'NIGHT') return 'Nuit';
  if (/^\d{3,4}$/.test(normalized)) return `${normalized.slice(0, -2)}:${normalized.slice(-2)}`;

  return toTitleCase(normalized.replace(/[_-]+/g, ' '));
}

function toTitleCase(value: string): string {
  return value
    .toLowerCase()
    .replace(/\b[a-z]/g, letter => letter.toUpperCase())
    .trim();
}
