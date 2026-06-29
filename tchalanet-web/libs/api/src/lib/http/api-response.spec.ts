import '@angular/compiler';
import { describe, expect, it } from 'vitest';

import { normalizeTchPage, unwrapApiResponse } from './api-response';

describe('api-response helpers', () => {
  it('unwraps ApiResponse data', () => {
    expect(
      unwrapApiResponse({
        status: 'SUCCESS',
        data: { ok: true },
        notices: [],
      }),
    ).toEqual({ ok: true });
  });

  it('normalizes backend pages using items and totalElements', () => {
    expect(
      normalizeTchPage({
        items: ['a', 'b'],
        totalElements: 6,
        totalPages: 3,
        page: 1,
        size: 2,
      }),
    ).toEqual({
      items: ['a', 'b'],
      totalElements: 6,
      totalPages: 3,
      page: 1,
      size: 2,
      last: false,
      hasNext: true,
      hasPrevious: true,
    });
  });

  it('normalizes Spring-style pages using content, total, and number', () => {
    expect(
      normalizeTchPage(
        {
          content: ['a'],
          total: 1,
          number: 0,
          size: 20,
        },
        2,
        50,
      ),
    ).toEqual({
      items: ['a'],
      totalElements: 1,
      totalPages: 1,
      page: 0,
      size: 20,
      last: true,
      hasNext: false,
      hasPrevious: false,
    });
  });
});
