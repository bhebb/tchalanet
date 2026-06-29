import '@angular/compiler';

import { describe, expect, it } from 'vitest';

import { appendQuery, pageQuery, toHttpParams, toQueryString } from './query-params';

describe('query param helpers', () => {
  it('drops empty values and preserves repeated params', () => {
    const params = toHttpParams({
      q: 'draw',
      empty: '',
      skipped: undefined,
      status: ['OPEN', 'LOCKED'],
      page: 1,
      active: false,
    });

    expect(params.toString()).toBe('q=draw&status=OPEN&status=LOCKED&page=1&active=false');
  });

  it('appends query params with the right separator', () => {
    expect(appendQuery('/admin/draws', { page: 0, size: 20 })).toBe(
      '/admin/draws?page=0&size=20',
    );
    expect(appendQuery('/admin/draws?active=true', { page: 0 })).toBe(
      '/admin/draws?active=true&page=0',
    );
  });

  it('builds standard page query inputs', () => {
    expect(toQueryString(pageQuery({ page: 2, size: 50, sort: 'createdAt,desc' }))).toBe(
      'page=2&size=50&sort=createdAt,desc',
    );
  });
});
