import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { delayedVisibility } from './delayed-visibility';

describe('delayedVisibility (anti-flash)', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    TestBed.configureTestingModule({});
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  function create(active: () => boolean) {
    const visible = TestBed.runInInjectionContext(() => delayedVisibility(active));
    TestBed.tick(); // premier run de l'effect
    return visible;
  }

  it('n’affiche rien pour une réponse plus rapide que le délai (pas de flash)', () => {
    const active = signal(true);
    const visible = create(active);

    vi.advanceTimersByTime(200);
    active.set(false);
    TestBed.tick();
    vi.advanceTimersByTime(1000);

    expect(visible()).toBe(false);
  });

  it('affiche après le délai de 300 ms', () => {
    const active = signal(true);
    const visible = create(active);

    expect(visible()).toBe(false);
    vi.advanceTimersByTime(300);
    expect(visible()).toBe(true);
  });

  it('reste visible au moins 500 ms une fois montré (anti-flicker)', () => {
    const active = signal(true);
    const visible = create(active);

    vi.advanceTimersByTime(300);
    expect(visible()).toBe(true);

    active.set(false);
    TestBed.tick();
    vi.advanceTimersByTime(400); // 400 < 500 ms minimum
    expect(visible()).toBe(true);

    vi.advanceTimersByTime(200);
    expect(visible()).toBe(false);
  });

  it('se cache immédiatement si la durée minimale est déjà écoulée', () => {
    const active = signal(true);
    const visible = create(active);

    vi.advanceTimersByTime(300);
    vi.advanceTimersByTime(600); // visible depuis 600 ms > 500 ms

    active.set(false);
    TestBed.tick();
    expect(visible()).toBe(false);
  });
});
