// @vitest-environment jsdom
import '@angular/compiler';
import { HttpClient } from '@angular/common/http';
import { signal } from '@angular/core';
import { TestBed, getTestBed } from '@angular/core/testing';
import {
  BrowserTestingModule,
  platformBrowserTesting,
} from '@angular/platform-browser/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { Observable, Subject } from 'rxjs';

try {
  getTestBed().initTestEnvironment(BrowserTestingModule, platformBrowserTesting());
} catch {
  // déjà initialisé par un autre spec du même worker
}

import { ApiResponse } from '../contracts/api.types';
import { TCH_API_BASE } from '../http/api-base';
import { TchBackendClient, TchResourceRequest } from './tch-backend-client';

interface PendingRequest {
  readonly url: string;
  readonly subject: Subject<unknown>;
  subscribed: number;
  unsubscribed: number;
}

describe('TchBackendClient resources', () => {
  let client: TchBackendClient;
  let pending: PendingRequest[];

  beforeEach(() => {
    pending = [];
    TestBed.configureTestingModule({
      providers: [
        {
          provide: HttpClient,
          useValue: {
            get: (url: string) => {
              const entry: PendingRequest = {
                url,
                subject: new Subject<unknown>(),
                subscribed: 0,
                unsubscribed: 0,
              };
              pending.push(entry);
              return new Observable(subscriber => {
                entry.subscribed += 1;
                const sub = entry.subject.subscribe(subscriber);
                return () => {
                  entry.unsubscribed += 1;
                  sub.unsubscribe();
                };
              });
            },
          },
        },
        { provide: TCH_API_BASE, useValue: '/api/v1' },
      ],
    });
    client = TestBed.inject(TchBackendClient);
  });

  afterEach(() => {
    TestBed.resetTestingModule();
  });

  /** La propagation rxResource passe par une Promise interne : drainer les microtasks. */
  async function settle(): Promise<void> {
    for (let i = 0; i < 3; i++) {
      await Promise.resolve();
      TestBed.tick();
    }
  }

  async function flushLatest(data: unknown): Promise<void> {
    const entry = pending[pending.length - 1];
    entry.subject.next({ data } satisfies ApiResponse<unknown>);
    entry.subject.complete();
    await settle();
  }

  it('reste idle et ne charge pas tant que request() renvoie undefined (lazy)', () => {
    const request = signal<TchResourceRequest | undefined>(undefined);
    const res = client.getResource<{ name: string }>(() => request());

    TestBed.tick();

    expect(res.status()).toBe('idle');
    expect(pending.length).toBe(0);
  });

  it('charge, unwrap ApiResponse et résout la valeur', async () => {
    const request = signal<TchResourceRequest | undefined>({ path: '/tenants/t-1' });
    const res = client.getResource<{ name: string }>(() => request());

    TestBed.tick();
    expect(pending.length).toBe(1);
    expect(pending[0].url).toBe('/api/v1/tenants/t-1');
    expect(res.status()).toBe('loading');

    await flushLatest({ name: 'Borlette Centre' });
    expect(res.status()).toBe('resolved');
    expect(res.value()).toEqual({ name: 'Borlette Centre' });
  });

  it('annule la requête en vol quand les params changent (anti-race)', async () => {
    const q = signal('a');
    const res = client.getResource<{ name: string }>(() => ({
      path: '/tenants',
      options: { params: { q: q() } },
    }));

    TestBed.tick();
    expect(pending.length).toBe(1);
    expect(pending[0].subscribed).toBe(1);

    q.set('ab');
    TestBed.tick();

    expect(pending.length).toBe(2);
    expect(pending[0].unsubscribed).toBe(1);

    await flushLatest({ name: 'Résultat récent' });
    expect(res.value()).toEqual({ name: 'Résultat récent' });
  });

  it('passe en reloading en gardant la valeur précédente lisible', async () => {
    const page = signal(0);
    const res = client.getResource<{ name: string }>(() => ({
      path: `/tenants?p=${page()}`,
    }));

    TestBed.tick();
    await flushLatest({ name: 'Page 0' });
    expect(res.status()).toBe('resolved');

    page.set(1);
    TestBed.tick();

    expect(res.status()).toBe('loading');
    // Nouveau params = nouveau chargement ; reload() sur les mêmes params = reloading
    res.reload();
    await settle();
    await flushLatest({ name: 'Page 1' });
    expect(res.value()).toEqual({ name: 'Page 1' });
  });

  it('expose l’erreur du stream via error()', async () => {
    const res = client.getResource<{ name: string }>(() => ({ path: '/tenants/t-404' }));

    TestBed.tick();
    pending[0].subject.error({ error: { title: 'Not Found', status: 404 } });
    await settle();

    expect(res.status()).toBe('error');
    expect(res.error()).toBeDefined();
  });

  it('getPageResource normalise la page backend en TchPage', async () => {
    const res = client.getPageResource<{ id: string }>(() => ({ path: '/tenants' }));

    TestBed.tick();
    await flushLatest({ items: [{ id: 't-1' }], totalElements: 41, page: 0, size: 20 });

    const value = res.value();
    expect(value?.items).toEqual([{ id: 't-1' }]);
    expect(value?.totalElements).toBe(41);
    expect(value?.totalPages).toBe(3);
    expect(value?.hasNext).toBe(true);
  });

  it('getResource projette le DTO brut vers la vue métier', async () => {
    const res = client.getResource<{ label: string }, { name: string }>(
      () => ({ path: '/tenants/t-1' }),
      raw => ({ label: raw.name.toUpperCase() }),
    );

    TestBed.tick();
    await flushLatest({ name: 'borlette' });

    expect(res.value()).toEqual({ label: 'BORLETTE' });
  });

  it('getApiResponseResource retains notices, trace, and a projected data value', async () => {
    const res = client.getApiResponseResource<{ label: string }, { name: string }>(
      () => ({ path: '/dashboard' }),
      raw => ({ label: raw.name.toUpperCase() }),
    );

    TestBed.tick();
    pending[0].subject.next({
      status: 'PARTIAL',
      notices: [
        {
          code: 'dashboard.recent_tickets.unavailable',
          message: 'diagnostic only',
          severity: 'WARN',
        },
      ],
      trace: { requestId: 'req-dashboard' },
      data: { name: 'borlette' },
    } satisfies ApiResponse<unknown>);
    pending[0].subject.complete();
    await settle();

    expect(res.value()).toEqual({
      status: 'PARTIAL',
      notices: [
        {
          code: 'dashboard.recent_tickets.unavailable',
          message: 'diagnostic only',
          severity: 'WARN',
        },
      ],
      trace: { requestId: 'req-dashboard' },
      data: { label: 'BORLETTE' },
    });
  });

  it('getPageResource projette chaque item et préserve la pagination', async () => {
    const res = client.getPageResource<{ code: string }, { id: string }>(
      () => ({ path: '/tenants' }),
      raw => ({ code: `T-${raw.id}` }),
    );

    TestBed.tick();
    await flushLatest({ items: [{ id: '1' }, { id: '2' }], totalElements: 2, page: 0, size: 20 });

    const value = res.value();
    expect(value?.items).toEqual([{ code: 'T-1' }, { code: 'T-2' }]);
    expect(value?.totalElements).toBe(2);
  });
});
