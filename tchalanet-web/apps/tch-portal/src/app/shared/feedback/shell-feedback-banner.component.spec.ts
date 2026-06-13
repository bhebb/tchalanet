import { describe, expect, it } from 'vitest';

import { ShellFeedbackBannerComponent } from './shell-feedback-banner.component';
import { ShellFeedbackItem } from './shell-feedback.model';

function makeItem(overrides: Partial<ShellFeedbackItem> = {}): ShellFeedbackItem {
  return {
    id: 'test-1',
    severity: 'error',
    title: 'Erreur',
    message: 'Détail',
    dismissible: true,
    ...overrides,
  };
}

function makeComponent(verbosity: 'minimal' | 'standard' | 'verbose', item: ShellFeedbackItem): ShellFeedbackBannerComponent {
  const c = new ShellFeedbackBannerComponent();
  c.item = item;
  c.verbosity = verbosity;
  return c;
}

describe('ShellFeedbackBannerComponent', () => {
  describe('details section', () => {
    it('is collapsed by default', () => {
      const c = makeComponent('standard', makeItem({ requestId: 'tch_req_x', traceId: 'aabb', spanId: '1122' }));
      expect(c['expanded']()).toBe(false);
    });

    it('toggles open on toggleDetails()', () => {
      const c = makeComponent('standard', makeItem({ traceId: 'aabb' }));
      c['toggleDetails']();
      expect(c['expanded']()).toBe(true);
    });

    it('toggles closed on second call', () => {
      const c = makeComponent('standard', makeItem({ traceId: 'aabb' }));
      c['toggleDetails']();
      c['toggleDetails']();
      expect(c['expanded']()).toBe(false);
    });

    it('hasDiagnostic returns true when any trace field present', () => {
      expect(makeComponent('standard', makeItem({ requestId: 'x' }))['hasDiagnostic']()).toBe(true);
      expect(makeComponent('standard', makeItem({ traceId: 'x' }))['hasDiagnostic']()).toBe(true);
      expect(makeComponent('standard', makeItem({ spanId: 'x' }))['hasDiagnostic']()).toBe(true);
    });

    it('hasDiagnostic returns false when no trace fields', () => {
      expect(makeComponent('standard', makeItem())['hasDiagnostic']()).toBe(false);
    });
  });

  describe('showDetails computed', () => {
    const item = makeItem({ traceId: 'x' });

    it('is false for minimal verbosity', () => {
      expect(makeComponent('minimal', item)['showDetails']()).toBe(false);
    });

    it('is true for standard verbosity', () => {
      expect(makeComponent('standard', item)['showDetails']()).toBe(true);
    });

    it('is true for verbose verbosity', () => {
      expect(makeComponent('verbose', item)['showDetails']()).toBe(true);
    });
  });
});
