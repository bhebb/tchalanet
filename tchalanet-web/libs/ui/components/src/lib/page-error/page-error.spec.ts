// @vitest-environment jsdom
import '@angular/compiler';
import { Component, signal } from '@angular/core';
import { TestBed, getTestBed } from '@angular/core/testing';
import { BrowserTestingModule, platformBrowserTesting } from '@angular/platform-browser/testing';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { TchPageError } from './page-error';

try {
  getTestBed().initTestEnvironment(BrowserTestingModule, platformBrowserTesting());
} catch {
  // The browser platform is initialized once per Vitest worker.
}

describe(TchPageError.name, () => {
  afterEach(() => TestBed.resetTestingModule());

  it('focuses the recovery surface, shows a support reference, and blocks duplicate retries', async () => {
    TestBed.configureTestingModule({ imports: [PageErrorHost] });
    const fixture = TestBed.createComponent(PageErrorHost);
    const host = fixture.componentInstance;
    host.retryLabel.set('Reessayer');
    host.showRetry.set(true);
    host.retryPending.set(true);
    host.supportReference.set('req_123 / trace_456');
    host.supportReferenceLabel.set('Reference support');
    host.copySupportLabel.set('Copier');
    fixture.detectChanges();
    await Promise.resolve();

    expect(fixture.nativeElement.textContent).toContain('req_123 / trace_456');
    expect(document.activeElement).toBe(fixture.nativeElement.querySelector('main'));
    const buttons = fixture.nativeElement.querySelectorAll('button');
    expect(buttons).toHaveLength(2);
    expect(buttons[1].disabled).toBe(true);

    buttons[0].click();
    buttons[1].click();
    expect(host.copySupport).toHaveBeenCalledOnce();
    expect(host.retry).not.toHaveBeenCalled();
  });
});

@Component({
  standalone: true,
  imports: [TchPageError],
  template: `
    <tch-page-error
      title="Une erreur est survenue"
      [showRetry]="showRetry()"
      [retryPending]="retryPending()"
      [retryLabel]="retryLabel()"
      [supportReference]="supportReference()"
      [supportReferenceLabel]="supportReferenceLabel()"
      [copySupportLabel]="copySupportLabel()"
      (retry)="retry()"
      (copySupport)="copySupport()"
    />
  `,
})
class PageErrorHost {
  readonly showRetry = signal(false);
  readonly retryPending = signal(false);
  readonly retryLabel = signal('');
  readonly supportReference = signal('');
  readonly supportReferenceLabel = signal('');
  readonly copySupportLabel = signal('');
  readonly retry = vi.fn();
  readonly copySupport = vi.fn();
}
