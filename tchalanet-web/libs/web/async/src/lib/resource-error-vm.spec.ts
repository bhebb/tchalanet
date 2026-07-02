import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { TranslateService } from '@ngx-translate/core';
import { describe, expect, it } from 'vitest';

import { resourceErrorVm, unwrapResourceError } from './resource-error-vm';

function setup(status: string, error: unknown) {
  TestBed.configureTestingModule({
    providers: [{ provide: TranslateService, useValue: { instant: (k: string) => k } }],
  });
  const statusSig = signal(status);
  const errorSig = signal(error);
  const vm = TestBed.runInInjectionContext(() =>
    resourceErrorVm({ status: statusSig, error: errorSig }, 'test.source'),
  );
  return { vm, statusSig, errorSig };
}

describe('resourceErrorVm', () => {
  it('renvoie null hors état error', () => {
    const { vm } = setup('resolved', undefined);
    expect(vm()).toBeNull();
  });

  it('mappe un ProblemDetail (via HttpErrorResponse) en ErrorViewModel', () => {
    const { vm } = setup(
      'error',
      new HttpErrorResponse({
        status: 404,
        error: { title: 'Not Found', status: 404, traceId: 'trace-1' },
      }),
    );
    const result = vm();
    expect(result).toBeTruthy();
    expect(result?.traceId).toBe('trace-1');
    expect(result?.status).toBe(404);
  });

  it('déballe Error.cause posé par le resource', () => {
    const wrapped = new Error('resource wrapper');
    wrapped.cause = new HttpErrorResponse({
      status: 409,
      error: { title: 'Conflit', status: 409 },
    });
    const { vm } = setup('error', wrapped);
    expect(vm()?.status).toBe(409);
  });

  it('mappe une erreur inconnue en fallback affichable', () => {
    const { vm } = setup('error', new Error('boom'));
    const result = vm();
    expect(result?.title).toBeTruthy();
    expect(result?.message).toBeTruthy();
  });

  it('redevient null quand le resource sort de l’état error', () => {
    const { vm, statusSig } = setup('error', { error: { title: 'X', status: 500 } });
    expect(vm()).toBeTruthy();
    statusSig.set('loading');
    expect(vm()).toBeNull();
  });
});

describe('unwrapResourceError', () => {
  it('renvoie cause quand présent', () => {
    const err = new Error('w');
    err.cause = 'origine';
    expect(unwrapResourceError(err)).toBe('origine');
  });

  it('renvoie l’erreur telle quelle sinon', () => {
    const raw = { status: 500 };
    expect(unwrapResourceError(raw)).toBe(raw);
  });
});
