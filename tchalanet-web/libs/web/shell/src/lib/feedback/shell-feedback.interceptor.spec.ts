import { HttpClient, HttpContext, provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { TranslateService } from '@ngx-translate/core';
import { TCH_FEEDBACK_CONTEXT } from '@tch/api';
import { describe, expect, it } from 'vitest';

import { shellFeedbackInterceptor } from './shell-feedback.interceptor';
import { ShellFeedbackStore } from './shell-feedback.store';

describe('shellFeedbackInterceptor', () => {
  it('routes an explicitly shell-owned failure once with safe translated copy', () => {
    const { http, httpMock, store } = setup();

    http.get('/api/system', {
      context: feedbackContext({ owner: 'shell', mode: 'inherit' }),
    }).subscribe({ error: () => undefined });
    httpMock.expectOne('/api/system').flush(
      { title: 'Provider exception should stay diagnostic', detail: 'secret', status: 503, code: 'service.unavailable' },
      { status: 503, statusText: 'Service Unavailable', headers: { 'X-Request-Id': 'req_123' } },
    );

    expect(store.items()).toMatchObject([{
      severity: 'error',
      title: 'Service indisponible',
      message: 'Reessayez plus tard.',
      requestId: 'req_123',
    }]);
    expect(store.items()[0].message).not.toContain('secret');
    httpMock.verify();
  });

  it('does not route locally-owned or silent failures to the shell', () => {
    const { http, httpMock, store } = setup();

    for (const context of [
      { owner: 'page' as const, mode: 'local' as const },
      { owner: 'shell' as const, mode: 'silent' as const },
      { mode: 'inherit' as const },
    ]) {
      http.get('/api/local', { context: feedbackContext(context) }).subscribe({ error: () => undefined });
      httpMock.expectOne('/api/local').flush({}, { status: 500, statusText: 'Failure' });
    }

    expect(store.items()).toEqual([]);
    httpMock.verify();
  });
});

function setup(): { readonly http: HttpClient; readonly httpMock: HttpTestingController; readonly store: ShellFeedbackStore } {
  TestBed.resetTestingModule();
  TestBed.configureTestingModule({
    providers: [
      provideHttpClient(withInterceptors([shellFeedbackInterceptor])),
      provideHttpClientTesting(),
      ShellFeedbackStore,
      {
        provide: TranslateService,
        useValue: {
          instant: (key: string) => ({
            'common.errors.categories.service_unavailable.title': 'Service indisponible',
            'common.errors.categories.service_unavailable.message': 'Reessayez plus tard.',
          })[key] ?? key,
        },
      },
    ],
  });

  return {
    http: TestBed.inject(HttpClient),
    httpMock: TestBed.inject(HttpTestingController),
    store: TestBed.inject(ShellFeedbackStore),
  };
}

function feedbackContext(value: { readonly owner?: 'shell' | 'page'; readonly mode: 'inherit' | 'local' | 'silent' }): HttpContext {
  return new HttpContext().set(TCH_FEEDBACK_CONTEXT, value);
}
