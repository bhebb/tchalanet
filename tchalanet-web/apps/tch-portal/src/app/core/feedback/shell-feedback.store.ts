import { Injectable, computed, signal } from '@angular/core';

import { AddShellFeedbackInput, ShellFeedbackItem } from './shell-feedback.model';

const MAX_VISIBLE_ITEMS = 3;
const MAX_STORED_ITEMS = 20;

@Injectable({ providedIn: 'root' })
export class ShellFeedbackStore {
  private readonly _items = signal<ShellFeedbackItem[]>([]);

  readonly items = this._items.asReadonly();
  readonly visibleItems = computed(() => this._items().slice(0, MAX_VISIBLE_ITEMS));
  readonly overflowCount = computed(() => Math.max(0, this._items().length - MAX_VISIBLE_ITEMS));
  readonly hasItems = computed(() => this._items().length > 0);

  add(input: AddShellFeedbackInput): void {
    const item: ShellFeedbackItem = {
      id: generateId(),
      dedupeKey: input.dedupeKey ?? defaultDedupeKey(input),
      severity: input.severity,
      title: input.title,
      message: input.message,
      source: input.source,
      requestId: input.requestId,
      traceId: input.traceId,
      spanId: input.spanId,
      errorId: input.errorId,
      status: input.status,
      copyText: input.copyText,
      dismissible: input.dismissible ?? true,
      repeatCount: 1,
    };
    this._items.update(current => {
      const existing = current.findIndex(candidate => candidate.dedupeKey === item.dedupeKey);
      if (existing >= 0) {
        const next = [...current];
        const previous = next[existing];
        next[existing] = {
          ...previous,
          id: item.id,
          requestId: item.requestId ?? previous.requestId,
          traceId: item.traceId ?? previous.traceId,
          spanId: item.spanId ?? previous.spanId,
          errorId: item.errorId ?? previous.errorId,
          copyText: item.copyText ?? previous.copyText,
          repeatCount: previous.repeatCount + 1,
        };
        return next;
      }

      const next = [item, ...current];
      return next.length > MAX_STORED_ITEMS ? next.slice(0, MAX_STORED_ITEMS) : next;
    });
  }

  dismiss(id: string): void {
    this._items.update(current => current.filter(item => item.id !== id));
  }

  clear(): void {
    this._items.set([]);
  }
}

function generateId(): string {
  return `fb-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`;
}

function defaultDedupeKey(input: AddShellFeedbackInput): string {
  return [
    input.title,
    input.status ?? 'na',
    input.source ?? 'unknown',
    input.severity,
  ].join('|');
}
