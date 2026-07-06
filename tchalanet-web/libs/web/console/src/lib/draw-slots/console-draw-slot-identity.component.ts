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
    const providerCode = firstText(view.providerShortName, view.providerCode);
    const period = periodLabel(view.slotKey, view.channelShortName, view.slotLabel, view.providerTimeLabel);
    if (providerCode && period) return `${providerCode}-${period}`;
    return firstText(providerCode, view.channelShortName, view.slotLabel, view.slotKey) ?? '—';
  });

  readonly subtitle = computed(() => {
    const view = this.identity();
    const title = this.title();
    const parts = displayLabels(title, view.providerName, view.slotLabel);
    return parts.length > 0 ? parts.join(' · ') : null;
  });

  readonly logoText = computed(() => {
    const view = this.identity();
    return firstText(view.logoText, view.providerShortName, view.providerCode, view.slotKey, this.title())?.slice(0, 4).toUpperCase() ?? '—';
  });

  readonly localLabel = computed(() => {
    const view = this.identity();
    const showLocalDate = hasDifferentDate(view.localDateLabel, view.providerDateLabel);
    const dateTime = [showLocalDate ? view.localDateLabel : null, view.localTimeLabel].filter(Boolean).join(' ');
    if (!dateTime) return null;
    return view.localTimezoneLabel ? `${dateTime} · ${view.localTimezoneLabel}` : dateTime;
  });

  readonly providerNameLabel = computed(() => {
    if (this.density() === 'compact') return null;
    return null;
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

function hasDifferentDate(
  localDateLabel: string | null | undefined,
  providerDateLabel: string | null | undefined,
): boolean {
  const local = localDateLabel?.trim();
  const provider = providerDateLabel?.trim();
  return Boolean(local && provider && local !== provider);
}

function periodLabel(...values: readonly (string | null | undefined)[]): string | null {
  for (const value of values) {
    const normalized = value?.trim().toUpperCase();
    if (!normalized) continue;
    const time = timeLabel(normalized);
    if (time) return time;
    if (/(^|[_\-\s·])MID(DAY)?($|[_\-\s·])/.test(normalized)) return 'Midi';
    if (/(^|[_\-\s·])EVE(NING)?($|[_\-\s·])/.test(normalized)) return 'Soir';
    if (/(^|[_\-\s·])MORNING($|[_\-\s·])/.test(normalized)) return 'Matin';
    if (/(^|[_\-\s·])LATE($|[_\-\s·])/.test(normalized)) return 'Late';
  }
  return null;
}

function timeLabel(value: string): string | null {
  const explicit = /(^|[^0-9])([0-2][0-9]):([0-5][0-9])([^0-9]|$)/.exec(value);
  if (explicit) return `${explicit[2]}:${explicit[3]}`;

  const compact = /(^|[^0-9])([0-2][0-9])([0-5][0-9])([^0-9]|$)/.exec(value);
  if (!compact) return null;
  return `${compact[2]}:${compact[3]}`;
}
