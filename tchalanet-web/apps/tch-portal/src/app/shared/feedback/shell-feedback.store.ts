import { Injectable, computed, signal } from '@angular/core';

import { AddShellFeedbackInput, ShellFeedbackItem } from './shell-feedback.model';

const MAX_ITEMS = 5;

@Injectable({ providedIn: 'root' })
export class ShellFeedbackStore {
  private readonly _items = signal<ShellFeedbackItem[]>([]);

  readonly items = this._items.asReadonly();
  readonly hasItems = computed(() => this._items().length > 0);

  add(input: AddShellFeedbackInput): void {
    const item: ShellFeedbackItem = {
      id: generateId(),
      severity: input.severity,
      title: input.title,
      message: input.message,
      source: input.source,
      requestId: input.requestId,
      traceId: input.traceId,
      spanId: input.spanId,
      status: input.status,
      copyText: input.copyText,
      dismissible: input.dismissible ?? true,
    };
    this._items.update(current => {
      const next = [item, ...current];
      return next.length > MAX_ITEMS ? next.slice(0, MAX_ITEMS) : next;
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
