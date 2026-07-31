// @vitest-environment jsdom
import '@angular/compiler';
import { Component, signal } from '@angular/core';
import { getTestBed, TestBed } from '@angular/core/testing';
import { BrowserTestingModule, platformBrowserTesting } from '@angular/platform-browser/testing';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { TchFeedbackFocusDirective } from './feedback-focus.directive';

try {
  getTestBed().initTestEnvironment(BrowserTestingModule, platformBrowserTesting());
} catch {
  // The browser platform is initialized once per Vitest worker.
}

describe(TchFeedbackFocusDirective.name, () => {
  afterEach(() => {
    TestBed.resetTestingModule();
    Reflect.deleteProperty(HTMLElement.prototype, 'scrollIntoView');
    vi.restoreAllMocks();
  });

  it('scrolls to and focuses a feedback surface after it is rendered', async () => {
    const scrollIntoView = vi.fn();
    Object.defineProperty(HTMLElement.prototype, 'scrollIntoView', {
      configurable: true,
      value: scrollIntoView,
    });
    TestBed.configureTestingModule({ imports: [FeedbackFocusHost] });

    const fixture = TestBed.createComponent(FeedbackFocusHost);
    fixture.componentInstance.active.set(true);
    fixture.detectChanges();
    await fixture.whenStable();

    const surface = fixture.nativeElement.querySelector('[data-feedback]') as HTMLElement;
    expect(scrollIntoView).toHaveBeenCalledWith({ block: 'center', behavior: 'smooth' });
    expect(document.activeElement).toBe(surface);
  });

  it('does not move focus when the owner marks the surface as non-blocking', async () => {
    const scrollIntoView = vi.fn();
    Object.defineProperty(HTMLElement.prototype, 'scrollIntoView', {
      configurable: true,
      value: scrollIntoView,
    });
    TestBed.configureTestingModule({ imports: [FeedbackFocusHost] });

    const fixture = TestBed.createComponent(FeedbackFocusHost);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(scrollIntoView).not.toHaveBeenCalled();
    expect(document.activeElement).not.toBe(fixture.nativeElement.querySelector('[data-feedback]'));
  });
});

@Component({
  standalone: true,
  imports: [TchFeedbackFocusDirective],
  template: `
    <div
      data-feedback
      role="status"
      tabindex="-1"
      [tchFeedbackFocus]="active()"
      [tchFeedbackFocusKey]="active()"
    >
      Feedback
    </div>
  `,
})
class FeedbackFocusHost {
  readonly active = signal(false);
}
