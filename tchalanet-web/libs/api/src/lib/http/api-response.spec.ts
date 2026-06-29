import '@angular/compiler';
import { describe, expect, it } from 'vitest';

import { unwrapApiResponse } from './api-response';

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
});
