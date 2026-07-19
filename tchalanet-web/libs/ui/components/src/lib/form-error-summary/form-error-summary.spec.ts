// @vitest-environment jsdom
import '@angular/compiler';
import { Component, signal } from '@angular/core';
import { getTestBed, TestBed } from '@angular/core/testing';
import { BrowserTestingModule, platformBrowserTesting } from '@angular/platform-browser/testing';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { afterEach, describe, expect, it } from 'vitest';

import { TchFormErrorSummary } from './form-error-summary';

try {
  getTestBed().initTestEnvironment(BrowserTestingModule, platformBrowserTesting());
} catch {
  // The browser platform is initialized once per Vitest worker.
}

describe(TchFormErrorSummary.name, () => {
  afterEach(() => TestBed.resetTestingModule());

  it('renders all unconsumed messages and moves focus to the summary', async () => {
    TestBed.configureTestingModule({ imports: [FormErrorSummaryHost, TranslateModule.forRoot()] });
    const translate = TestBed.inject(TranslateService);
    translate.setTranslation('ht', {
      common: { errors: { categories: { validation: { title: 'Enfomasyon pou korije' } } } },
    });
    translate.use('ht');

    const fixture = TestBed.createComponent(FormErrorSummaryHost);
    fixture.componentInstance.messages.set(['Foma pa valid.', 'Chan sa a obligatwa.']);
    fixture.detectChanges();
    await Promise.resolve();

    const summary = fixture.nativeElement.querySelector('.tch-form-error-summary') as HTMLElement;
    expect(summary.textContent).toContain('Enfomasyon pou korije');
    expect(summary.querySelectorAll('li')).toHaveLength(2);
    expect(document.activeElement).toBe(summary);
  });
});

@Component({
  standalone: true,
  imports: [TchFormErrorSummary],
  template: '<tch-form-error-summary [messages]="messages()" />',
})
class FormErrorSummaryHost {
  readonly messages = signal<readonly string[]>([]);
}
