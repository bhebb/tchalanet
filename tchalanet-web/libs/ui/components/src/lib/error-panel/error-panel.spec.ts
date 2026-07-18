// @vitest-environment jsdom
import '@angular/compiler';
import { Component, signal } from '@angular/core';
import { TestBed, getTestBed } from '@angular/core/testing';
import {
  BrowserTestingModule,
  platformBrowserTesting,
} from '@angular/platform-browser/testing';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { TchErrorPanel } from './error-panel';

try {
  getTestBed().initTestEnvironment(BrowserTestingModule, platformBrowserTesting());
} catch {
  // The browser platform is initialized once per Vitest worker.
}

describe(TchErrorPanel.name, () => {
  afterEach(() => TestBed.resetTestingModule());

  it('renders a support reference and delegates recovery actions to its owner', () => {
    TestBed.configureTestingModule({ imports: [ErrorPanelHost] });
    const fixture = TestBed.createComponent(ErrorPanelHost);
    const host = fixture.componentInstance;
    host.title.set('Une erreur est survenue');
    host.message.set('Reessayez ou contactez le support.');
    host.showRetry.set(true);
    host.retryLabel.set('Reessayer');
    host.showBack.set(true);
    host.backLabel.set('Retour');
    host.supportReference.set('req_123 / trace_456');
    host.supportReferenceLabel.set('Reference support');
    host.copySupportLabel.set('Copier');
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('req_123 / trace_456');
    const buttons = fixture.nativeElement.querySelectorAll('button');
    expect(buttons).toHaveLength(3);

    buttons[0].click();
    buttons[1].click();
    buttons[2].click();

    expect(host.copySupport).toHaveBeenCalledOnce();
    expect(host.retry).toHaveBeenCalledOnce();
    expect(host.back).toHaveBeenCalledOnce();
  });

  it('disables retry while a retry is pending', () => {
    TestBed.configureTestingModule({ imports: [ErrorPanelHost] });
    const fixture = TestBed.createComponent(ErrorPanelHost);
    const host = fixture.componentInstance;
    host.showRetry.set(true);
    host.retryLabel.set('Reessayer');
    host.retryPending.set(true);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('button')?.disabled).toBe(true);
  });
});

@Component({
  standalone: true,
  imports: [TchErrorPanel],
  template: `
    <tch-error-panel
      [title]="title()"
      [message]="message()"
      [showRetry]="showRetry()"
      [retryLabel]="retryLabel()"
      [retryPending]="retryPending()"
      [showBack]="showBack()"
      [backLabel]="backLabel()"
      [supportReference]="supportReference()"
      [supportReferenceLabel]="supportReferenceLabel()"
      [copySupportLabel]="copySupportLabel()"
      (retry)="retry()"
      (back)="back()"
      (copySupport)="copySupport()"
    />
  `,
})
class ErrorPanelHost {
  readonly title = signal('');
  readonly message = signal('');
  readonly showRetry = signal(false);
  readonly retryLabel = signal('');
  readonly retryPending = signal(false);
  readonly showBack = signal(false);
  readonly backLabel = signal('');
  readonly supportReference = signal('');
  readonly supportReferenceLabel = signal('');
  readonly copySupportLabel = signal('');
  readonly retry = vi.fn();
  readonly back = vi.fn();
  readonly copySupport = vi.fn();
}
