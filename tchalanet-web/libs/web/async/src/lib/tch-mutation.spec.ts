import { TestBed } from '@angular/core/testing';
import { TranslateService } from '@ngx-translate/core';
import { Subject } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';

import { TchMutation, TchMutationContext, tchMutation } from './tch-mutation';

interface Call {
  readonly input: string;
  readonly ctx: TchMutationContext;
  readonly result: Subject<string>;
}

function setup(options?: {
  onSuccess?: (result: string, input: string) => void;
  onError?: (err: unknown, input: string) => boolean | void;
  idempotency?: { keyFactory?: () => string };
}): { mutation: TchMutation<string, string>; calls: Call[] } {
  TestBed.configureTestingModule({
    providers: [{ provide: TranslateService, useValue: { instant: (k: string) => k } }],
  });

  const calls: Call[] = [];
  const mutation = TestBed.runInInjectionContext(() =>
    tchMutation<string, string>({
      run: (input, ctx) => {
        const result = new Subject<string>();
        calls.push({ input, ctx, result });
        return result.asObservable();
      },
      source: 'test.mutation',
      onSuccess: options?.onSuccess,
      onError: options?.onError,
      idempotency: options?.idempotency,
    }),
  );
  return { mutation, calls };
}

describe('tchMutation', () => {
  it('suit le pending global pendant l’exécution', () => {
    const { mutation, calls } = setup();
    expect(mutation.pending()).toBe(false);

    mutation.execute('a');
    expect(mutation.pending()).toBe(true);

    calls[0].result.next('ok');
    calls[0].result.complete();
    expect(mutation.pending()).toBe(false);
  });

  it('bloque le double-submit : execute() est no-op si déjà pending', () => {
    const { mutation, calls } = setup();
    mutation.execute('a');
    mutation.execute('a');
    mutation.execute('b');

    expect(calls.length).toBe(1);
  });

  it('gère le pending par clé de ligne indépendamment', () => {
    const { mutation, calls } = setup();
    mutation.execute('a', { key: 'row-1' });

    expect(mutation.pending('row-1')).toBe(true);
    expect(mutation.pending('row-2')).toBe(false);
    expect(mutation.pending()).toBe(true);

    mutation.execute('b', { key: 'row-2' });
    expect(calls.length).toBe(2);

    mutation.execute('c', { key: 'row-1' }); // double-clic ligne 1 : no-op
    expect(calls.length).toBe(2);
  });

  it('émet un feedback succès et appelle onSuccess', () => {
    const onSuccess = vi.fn();
    const { mutation, calls } = setup({ onSuccess });

    mutation.execute('a');
    calls[0].result.next('résultat');
    calls[0].result.complete();

    expect(mutation.feedback()).toEqual({ kind: 'success', vm: null });
    expect(onSuccess).toHaveBeenCalledWith('résultat', 'a');
  });

  it('émet un feedback erreur avec ErrorViewModel mappé', () => {
    const { mutation, calls } = setup();

    mutation.execute('a');
    calls[0].result.error({ error: { title: 'Conflit', status: 409, detail: 'Déjà traité' } });

    const feedback = mutation.feedback();
    expect(feedback?.kind).toBe('error');
    expect(feedback?.vm).toBeTruthy();
    expect(mutation.pending()).toBe(false);
  });

  it('onError=true supprime le feedback générique (erreur gérée par la page)', () => {
    const { mutation, calls } = setup({ onError: () => true });
    mutation.execute('a');
    calls[0].result.error({ error: { title: 'Validation', status: 400 } });

    expect(mutation.feedback()).toBeNull();
    expect(mutation.pending()).toBe(false);
  });

  it('onError sans true laisse le feedback erreur standard', () => {
    const seen: unknown[] = [];
    const { mutation, calls } = setup({ onError: err => void seen.push(err) });
    mutation.execute('a');
    calls[0].result.error({ error: { title: 'KO', status: 500 } });

    expect(seen.length).toBe(1);
    expect(mutation.feedback()?.kind).toBe('error');
  });

  it('clearFeedback efface le feedback', () => {
    const { mutation, calls } = setup();
    mutation.execute('a');
    calls[0].result.next('ok');
    calls[0].result.complete();

    mutation.clearFeedback();
    expect(mutation.feedback()).toBeNull();
  });

  it('ne fournit pas de clé d’idempotence sans configuration', () => {
    const { mutation, calls } = setup();
    mutation.execute('a');
    expect(calls[0].ctx.idempotencyKey).toBeUndefined();
  });

  it('fournit une clé d’idempotence via keyFactory', () => {
    const { mutation, calls } = setup({ idempotency: { keyFactory: () => 'key-42' } });
    mutation.execute('a');
    expect(calls[0].ctx.idempotencyKey).toBe('key-42');
  });

  it('génère une clé UUID par défaut quand idempotency est activé', () => {
    const { mutation, calls } = setup({ idempotency: {} });
    mutation.execute('a');
    expect(calls[0].ctx.idempotencyKey).toMatch(/^[0-9a-f-]{36}$/);
  });
});
