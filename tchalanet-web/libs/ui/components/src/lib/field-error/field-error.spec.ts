// @vitest-environment jsdom
import '@angular/compiler';
import { Component, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { getTestBed, TestBed } from '@angular/core/testing';
import { BrowserTestingModule, platformBrowserTesting } from '@angular/platform-browser/testing';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { afterEach, describe, expect, it } from 'vitest';

import { TchFieldError } from './field-error';

try {
  getTestBed().initTestEnvironment(BrowserTestingModule, platformBrowserTesting());
} catch {
  // The browser platform is initialized once per Vitest worker.
}

describe(TchFieldError.name, () => {
  afterEach(() => TestBed.resetTestingModule());

  it('translates local reactive-form validation keys instead of embedding French copy', () => {
    TestBed.configureTestingModule({ imports: [FieldErrorHost, TranslateModule.forRoot()] });
    const translate = TestBed.inject(TranslateService);
    translate.setTranslation('ht', {
      common: { validation: { required: 'Chan sa a obligatwa.' } },
    });
    translate.use('ht');

    const fixture = TestBed.createComponent(FieldErrorHost);
    fixture.componentInstance.control.markAsTouched();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Chan sa a obligatwa.');
    expect(fixture.nativeElement.textContent).not.toContain('Ce champ est obligatoire.');
  });

  it('shows every presentation-owned server message for a field', () => {
    TestBed.configureTestingModule({ imports: [FieldErrorHost, TranslateModule.forRoot()] });
    const fixture = TestBed.createComponent(FieldErrorHost);
    fixture.componentInstance.control.setErrors({
      server: [
        { message: 'errors.codes.validation.required.message' },
        { message: 'errors.codes.validation.invalid_format.message' },
      ],
    });
    fixture.componentInstance.control.markAsTouched();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelectorAll('.tch-field-error')).toHaveLength(2);
  });
});

@Component({
  standalone: true,
  imports: [ReactiveFormsModule, TchFieldError],
  template: '<tch-field-error [control]="control" [message]="message()" />',
})
class FieldErrorHost {
  readonly control = new FormControl('', { nonNullable: true, validators: [Validators.required] });
  readonly message = signal('');
}
