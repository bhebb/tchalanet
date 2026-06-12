import '@angular/compiler';
import { HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { describe, expect, it } from 'vitest';

import { mapHttpErrorToProblemDetail } from './http-error.mapper';

function makeErrorResponse(opts: {
  status?: number;
  body?: unknown;
  headers?: Record<string, string>;
}): HttpErrorResponse {
  return new HttpErrorResponse({
    status: opts.status ?? 400,
    error: opts.body ?? null,
    headers: new HttpHeaders(opts.headers ?? {}),
    url: 'https://api.test/endpoint',
  });
}

describe('mapHttpErrorToProblemDetail', () => {
  it('extracts requestId from X-Request-Id response header', () => {
    const err = makeErrorResponse({
      status: 500,
      body: { title: 'Server error', status: 500 },
      headers: { 'X-Request-Id': 'tch_req_abc123' },
    });
    const pd = mapHttpErrorToProblemDetail(err);
    expect(pd.requestId).toBe('tch_req_abc123');
  });

  it('extracts traceId and spanId from response headers', () => {
    const err = makeErrorResponse({
      status: 500,
      body: { title: 'err', status: 500 },
      headers: {
        'X-Request-Id': 'tch_req_abc',
        'X-Trace-Id': 'trace-abc',
        'X-Span-Id': 'span-xyz',
      },
    });
    const pd = mapHttpErrorToProblemDetail(err);
    expect(pd.traceId).toBe('trace-abc');
    expect(pd.spanId).toBe('span-xyz');
  });

  it('prefers body requestId over header when both present', () => {
    const err = makeErrorResponse({
      status: 400,
      body: { title: 'Bad request', status: 400, requestId: 'tch_req_from-body' },
      headers: { 'X-Request-Id': 'tch_req_from-header' },
    });
    const pd = mapHttpErrorToProblemDetail(err);
    expect(pd.requestId).toBe('tch_req_from-body');
  });

  it('falls back to header requestId when body has none', () => {
    const err = makeErrorResponse({
      status: 400,
      body: { title: 'Bad request', status: 400 },
      headers: { 'X-Request-Id': 'tch_req_fallback' },
    });
    const pd = mapHttpErrorToProblemDetail(err);
    expect(pd.requestId).toBe('tch_req_fallback');
  });

  it('returns null-safe result when no headers present', () => {
    const err = makeErrorResponse({ status: 500, body: { title: 'oops', status: 500 } });
    const pd = mapHttpErrorToProblemDetail(err);
    expect(pd.requestId).toBeUndefined();
    expect(pd.traceId).toBeUndefined();
    expect(pd.spanId).toBeUndefined();
  });

  it('handles non-ProblemDetail error body without crash', () => {
    const err = makeErrorResponse({
      status: 503,
      body: 'Gateway timeout',
      headers: { 'X-Request-Id': 'tch_req_xyz' },
    });
    const pd = mapHttpErrorToProblemDetail(err);
    expect(pd.status).toBe(503);
    expect(pd.correlationId).toBe('tch_req_xyz');
    expect(pd.requestId).toBe('tch_req_xyz');
  });

  it('passes through a plain ProblemDetail object unchanged', () => {
    const plain = { title: 'Forbidden', status: 403, requestId: 'tch_req_x' };
    const pd = mapHttpErrorToProblemDetail(plain);
    expect(pd.title).toBe('Forbidden');
    expect(pd.requestId).toBe('tch_req_x');
  });
});
