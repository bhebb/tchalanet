import { describe, expect, it } from 'vitest';

import { ShellFeedbackStore } from './shell-feedback.store';

describe('ShellFeedbackStore', () => {
  it('groups repeated feedback by dedupeKey and increments repeatCount', () => {
    const store = new ShellFeedbackStore();

    store.add({
      dedupeKey: 'service.down|503|dashboard|shell',
      severity: 'error',
      title: 'service.down',
      message: 'Unavailable',
      traceId: 'trace-1',
    });
    store.add({
      dedupeKey: 'service.down|503|dashboard|shell',
      severity: 'error',
      title: 'service.down',
      message: 'Unavailable',
      traceId: 'trace-2',
    });

    expect(store.items()).toHaveLength(1);
    expect(store.items()[0].repeatCount).toBe(2);
    expect(store.items()[0].traceId).toBe('trace-2');
  });

  it('keeps distinct dedupe keys as separate feedback items', () => {
    const store = new ShellFeedbackStore();

    store.add({ dedupeKey: 'a', severity: 'warn', title: 'a', message: 'A' });
    store.add({ dedupeKey: 'b', severity: 'warn', title: 'b', message: 'B' });

    expect(store.items()).toHaveLength(2);
  });

  it('caps visible feedback and exposes overflow count', () => {
    const store = new ShellFeedbackStore();

    for (let i = 0; i < 5; i += 1) {
      store.add({ dedupeKey: `k-${i}`, severity: 'warn', title: `t-${i}`, message: `m-${i}` });
    }

    expect(store.items()).toHaveLength(5);
    expect(store.visibleItems()).toHaveLength(3);
    expect(store.overflowCount()).toBe(2);
  });
});
