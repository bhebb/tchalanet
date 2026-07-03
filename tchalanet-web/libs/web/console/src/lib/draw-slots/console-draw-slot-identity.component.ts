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
    const parts = [
      firstText(view.channelCode, view.slotKey),
      view.slotLabel && view.slotLabel !== this.title() ? view.slotLabel : null,
    ].filter(Boolean);
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
    if (!view.providerTimeLabel) return null;
    return view.providerTimezoneLabel
      ? `${view.providerTimeLabel} · ${view.providerTimezoneLabel}`
      : view.providerTimeLabel;
  });
}

function firstText(...values: readonly (string | null | undefined)[]): string | null {
  for (const value of values) {
    const text = value?.trim();
    if (text) return text;
  }
  return null;
}
