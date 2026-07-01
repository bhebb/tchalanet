import '@angular/compiler';
import { HttpClient } from '@angular/common/http';
import { createEnvironmentInjector, EnvironmentInjector, runInInjectionContext } from '@angular/core';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { of } from 'rxjs';

import { TCH_API_BASE } from '../http/api-base';
import { TchBackendClient } from './tch-backend-client';

describe('TchBackendClient', () => {
  let injector: EnvironmentInjector;
  let client: TchBackendClient;
  let requests: { readonly url: string; readonly options: unknown }[];
  let response: unknown;

  beforeEach(() => {
    requests = [];
    injector = createEnvironmentInjector([
      {
        provide: HttpClient,
        useValue: {
          get: (url: string, options: unknown) => {
            requests.push({ url, options });
            return of(response);
          },
        },
      },
      { provide: TCH_API_BASE, useValue: '/api/v1' },
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
});
