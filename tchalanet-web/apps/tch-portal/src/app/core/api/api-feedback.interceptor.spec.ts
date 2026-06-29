import '@angular/compiler';

import { HttpClient, HttpContext, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { TranslateService } from '@ngx-translate/core';
import { SUPPRESS_SHELL_FEEDBACK } from '@tch/api';
import { of } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';

import { ShellFeedbackStore } from '../feedback/shell-feedback.store';
import { apiFeedbackInterceptor } from './api-feedback.interceptor';

describe('apiFeedbackInterceptor', () => {
  function setup() {
    const store = new ShellFeedbackStore();
    vi.spyOn(store, 'add');

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([apiFeedbackInterceptor])),
        provideHttpClientTesting(),
        { provide: ShellFeedbackStore, useValue: store },
        { provide: TranslateService, useValue: { instant: (key: string) => key, get: (key: string) => of(key) } },
      ],
    });

    return {
      client: TestBed.inject(HttpClient),
      http: TestBed.inject(HttpTestingController),
      store,
    };
  }

  it('does not create shell feedback when the request is locally handled', () => {
    const { client, http, store } = setup();

    client
      .get('/api/v1/public/page', {
        context: new HttpContext().set(SUPPRESS_SHELL_FEEDBACK, true),
      })
      .subscribe({ error: () => undefined });

    http.expectOne('/api/v1/public/page').flush(
      { title: 'Raw backend detail', status: 500, code: 'internal.unexpected' },
      { status: 500, statusText: 'Server Error' },
    );

    expect(store.add).not.toHaveBeenCalled();
    http.verify();
  });

  it('creates shell feedback for unowned API failures', () => {
    const { client, http, store } = setup();

    client.get('/api/v1/private/action').subscribe({ error: () => undefined });

    http.expectOne('/api/v1/private/action').flush(
      {
        title: 'Raw backend detail',
        status: 503,
        code: 'service.partner.degraded',
        traceId: 'trace-1',
      },
      { status: 503, statusText: 'Service Unavailable' },
    );

    expect(store.add).toHaveBeenCalledOnce();
    expect(store.add).toHaveBeenCalledWith(
      expect.objectContaining({
        dedupeKey: expect.stringContaining('service.partner.degraded'),
        traceId: 'trace-1',
      }),
    );
    http.verify();
  });

  it('does not create shell feedback for locally targeted response notices', () => {
    const { client, http, store } = setup();

    client.get('/api/v1/dashboard').subscribe();

    http.expectOne('/api/v1/dashboard').flush({
      status: 'SUCCESS_WITH_WARNINGS',
      data: {},
      notices: [
        {
          code: 'dashboard.commissions.unavailable',
          message: 'Commissions are temporarily unavailable.',
          domain: 'dashboard',
          severity: 'WARN',
          meta: {
            surface: 'section',
            placement: 'top',
            target: 'dashboard.commissions',
          },
        },
      ],
      services: [],
    });

    expect(store.add).not.toHaveBeenCalled();
    http.verify();
  });
});
