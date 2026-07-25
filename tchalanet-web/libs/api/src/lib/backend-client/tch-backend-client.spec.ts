import '@angular/compiler';
import { HttpClient, HttpContext, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { createEnvironmentInjector, EnvironmentInjector, runInInjectionContext } from '@angular/core';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { firstValueFrom, of, throwError } from 'rxjs';

import { TCH_API_BASE, TCH_API_BASE_RESOLVER } from '../http/api-base';
import { SUPPRESS_SHELL_FEEDBACK, TCH_FEEDBACK_CONTEXT } from '../http/api-feedback-context';
import { TchBackendClient } from './tch-backend-client';

describe('TchBackendClient', () => {
  let injector: EnvironmentInjector;
  let client: TchBackendClient;
  let apiBase: string;
  let requests: { readonly url: string; readonly options: unknown }[];
  let response: unknown;

  beforeEach(() => {
    apiBase = '/api/v1';
    requests = [];
    injector = createEnvironmentInjector([
      {
        provide: HttpClient,
        useValue: {
          get: (url: string, options: unknown) => {
            requests.push({ url, options });
            return of(response);
          },
          post: (url: string, body: unknown, options: unknown) => {
            requests.push({ url, options: { ...(options as object), body } });
            return response instanceof HttpErrorResponse ? throwError(() => response) : of(response);
          },
        },
      },
      { provide: TCH_API_BASE, useValue: '/api/v1' },
      { provide: TCH_API_BASE_RESOLVER, useValue: () => apiBase },
    ]);

    client = runInInjectionContext(injector, () => new TchBackendClient());
  });

  afterEach(() => {
    injector.destroy();
  });

  it('returns a typed page from a backend page response', () => {
    let received: unknown;

    response = {
      status: 'SUCCESS',
      notices: [],
      data: {
        content: ['one', 'two'],
        total: 4,
        number: 1,
        size: 2,
      },
    };

    client.getPage<string>('/items').subscribe(page => {
      received = page;
    });

    expect(requests[0]?.url).toBe('/api/v1/items');
    expect(received).toEqual({
      items: ['one', 'two'],
      totalElements: 4,
      totalPages: 2,
      page: 1,
      size: 2,
      last: true,
      hasNext: false,
      hasPrevious: true,
    });
  });

  it('uses requested page params when the backend omits page metadata', () => {
    let received: unknown;

    response = {
      status: 'SUCCESS',
      notices: [],
      data: {
        items: ['twenty-one'],
        totalElements: 31,
      },
    };

    client.getPage<string>('/items', { params: { page: '2', size: '10' } }).subscribe(page => {
      received = page;
    });

    expect(requests[0]?.url).toBe('/api/v1/items');
    expect(received).toEqual({
      items: ['twenty-one'],
      totalElements: 31,
      totalPages: 4,
      page: 2,
      size: 10,
      last: false,
      hasNext: true,
      hasPrevious: true,
    });
  });

  it('retains notices and trace metadata with a normalized paged response', () => {
    let received: unknown;

    response = {
      status: 'PARTIAL',
      notices: [{ code: 'dashboard.recent_tickets.unavailable', message: 'diagnostic', severity: 'WARN' }],
      trace: { requestId: 'req-1', traceId: 'trace-1' },
      data: { items: ['one'], totalElements: 3 },
    };

    client.getPageApiResponse<string>('/items', { params: { page: '1', size: '2' } }).subscribe(value => {
      received = value;
    });

    expect(received).toEqual({
      status: 'PARTIAL',
      notices: [{ code: 'dashboard.recent_tickets.unavailable', message: 'diagnostic', severity: 'WARN' }],
      trace: { requestId: 'req-1', traceId: 'trace-1' },
      data: {
        items: ['one'],
        totalElements: 3,
        totalPages: 2,
        page: 1,
        size: 2,
        last: true,
        hasNext: false,
        hasPrevious: true,
      },
    });
  });

  it('resolves the API base URL at request time', () => {
    response = {
      status: 'SUCCESS',
      notices: [],
      data: { ok: true },
    };
    apiBase = 'https://api.stg.tchalanet.com/api/v1';

    client.get('/runtime/private').subscribe();

    expect(requests[0]?.url).toBe('https://api.stg.tchalanet.com/api/v1/runtime/private');
  });

  it('passes explicit feedback ownership through request context', () => {
    response = { status: 'SUCCESS', notices: [], data: { ok: true } };

    client.get('/dashboard', {
      feedback: { owner: 'section', mode: 'local', target: 'dashboard.commissions' },
    }).subscribe();

    const context = (requests[0]?.options as { context?: HttpContext }).context;
    expect(context?.get(TCH_FEEDBACK_CONTEXT)).toEqual({
      owner: 'section',
      mode: 'local',
      target: 'dashboard.commissions',
    });
    expect(context?.get(SUPPRESS_SHELL_FEEDBACK)).toBe(false);
  });

  it('maps the legacy suppression flag to local feedback ownership during migration', () => {
    response = { status: 'SUCCESS', notices: [], data: { ok: true } };

    client.get('/dashboard', { suppressShellFeedback: true }).subscribe();

    const context = (requests[0]?.options as { context?: HttpContext }).context;
    expect(context?.get(TCH_FEEDBACK_CONTEXT)).toEqual({ mode: 'local' });
    expect(context?.get(SUPPRESS_SHELL_FEEDBACK)).toBe(true);
  });

  it('decodes ProblemDetail JSON returned from a blob request failure', async () => {
    const problem = {
      title: 'Ticket print rejected',
      status: 400,
      code: 'ticket.reprint.reason_required',
      category: 'VALIDATION',
      detail: 'Diagnostic-only prose',
    };
    response = new HttpErrorResponse({
      status: 400,
      statusText: 'Bad Request',
      url: '/api/v1/tenant/cashier/tickets/ticket-1/print',
      headers: new HttpHeaders({ 'X-Request-Id': 'tch_req_print_1' }),
      error: new Blob([JSON.stringify(problem)], { type: 'application/problem+json' }),
    });

    await expect(firstValueFrom(client.postBlob('/tenant/cashier/tickets/ticket-1/print', {})))
      .rejects.toMatchObject({
        error: problem,
        status: 400,
      });
  });
});
