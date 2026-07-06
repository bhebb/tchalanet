import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { ConsoleDrawSlotIdentity } from './console-draw-slot-identity.models';

@Component({
  selector: 'tch-console-draw-slot-identity',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './console-draw-slot-identity.component.html',
  styleUrls: ['./console-draw-slot-identity.component.scss'],
})
export class ConsoleDrawSlotIdentityComponent {
  readonly identity = input.required<ConsoleDrawSlotIdentity>();
  readonly density = input<'comfortable' | 'compact'>('comfortable');

  readonly title = computed(() => {
    const view = this.identity();
    return firstText(view.channelShortName, view.channelName, view.slotLabel, view.slotKey, view.providerName, view.providerCode) ?? '—';
  });

  readonly subtitle = computed(() => {
    const view = this.identity();
    const title = this.title();
    const parts = displayLabels(title, view.channelName, view.slotLabel, view.providerName);
    return parts.length > 0 ? parts.join(' · ') : null;
  });

  readonly logoText = computed(() => {
    const view = this.identity();
    return firstText(view.logoText, view.providerShortName, view.providerCode, view.slotKey, this.title())?.slice(0, 4).toUpperCase() ?? '—';
  });

  readonly localLabel = computed(() => {
    const view = this.identity();
    const dateTime = [view.localDateLabel, view.localTimeLabel].filter(Boolean).join(' ');
    if (!dateTime) return null;
    return view.localTimezoneLabel ? `${dateTime} · ${view.localTimezoneLabel}` : dateTime;
  });

  readonly providerNameLabel = computed(() => {
    if (this.density() === 'compact') return null;
    const view = this.identity();
    const provider = firstText(view.providerName, view.providerCode);
    return provider && provider !== this.title() ? provider : null;
  });

  readonly providerLabel = computed(() => {
    const view = this.identity();
    const dateTime = [view.providerDateLabel, view.providerTimeLabel].filter(Boolean).join(' ');
    if (!dateTime) return null;
    return view.providerTimezoneLabel
      ? `${dateTime} · ${view.providerTimezoneLabel}`
      : dateTime;
  });
}

function firstText(...values: readonly (string | null | undefined)[]): string | null {
  for (const value of values) {
    const text = value?.trim();
    if (text) return text;
  }
  return null;
}

function displayLabels(currentTitle: string, ...values: readonly (string | null | undefined)[]): string[] {
  const labels: string[] = [];
  for (const value of values) {
    const text = value?.trim();
    if (!text || text === currentTitle || isTechnicalCode(text)) continue;
    const alreadyCovered = labels.some(label => label === text || label.includes(text) || text.includes(label));
    if (!alreadyCovered) labels.push(text);
  }
  return labels;
}

function isTechnicalCode(value: string): boolean {
  return /^[A-Z]{2,}(?:_[A-Z0-9]+)+$/.test(value) || /^HT_[A-Z0-9_]+$/.test(value);
}
