import '@angular/compiler';
import { HttpRequest, HttpResponse, HttpHandlerFn } from '@angular/common/http';
import { describe, expect, it } from 'vitest';
import { of } from 'rxjs';

import { correlationRequestInterceptor } from './correlation.interceptor';

function runInterceptor(request: HttpRequest<unknown>) {
  let captured: HttpRequest<unknown> | undefined;
  const next: HttpHandlerFn = (req) => {
    captured = req as HttpRequest<unknown>;
    return of(new HttpResponse({ status: 200 }));
  };
  correlationRequestInterceptor(request, next);
  return captured!;
}

describe('correlationRequestInterceptor', () => {
  it('adds X-Request-Id with tch_req_ prefix when absent', () => {
    const req = new HttpRequest('GET', '/api/test');
    const forwarded = runInterceptor(req);

    const header = forwarded.headers.get('X-Request-Id');
    expect(header).toBeTruthy();
    expect(header).toMatch(/^tch_req_/);
  });

  it('generates unique IDs for each request', () => {
    const req = new HttpRequest('GET', '/api/test');
    const a = runInterceptor(req).headers.get('X-Request-Id');
    const b = runInterceptor(req).headers.get('X-Request-Id');
    expect(a).not.toBe(b);
  });

  it('does not overwrite an existing X-Request-Id', () => {
    const existing = 'tch_req_already-set-0123456789';
    const req = new HttpRequest('GET', '/api/test', { headers: new (require('@angular/common/http').HttpHeaders)({ 'X-Request-Id': existing }) });
    const forwarded = runInterceptor(req);
    expect(forwarded.headers.get('X-Request-Id')).toBe(existing);
  });
});
