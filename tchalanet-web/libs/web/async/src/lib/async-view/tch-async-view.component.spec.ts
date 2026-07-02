import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslateService } from '@ngx-translate/core';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { TchErrorViewModel } from '@tch/web/errors';

import { TchAsyncReadyDirective } from './tch-async-ready.directive';
import { TchAsyncViewComponent } from './tch-async-view.component';

@Component({
  standalone: true,
  imports: [TchAsyncViewComponent, TchAsyncReadyDirective],
  template: `
    <tch-async-view
      [resource]="source"
      [error]="errorVm()"
      loadingLabel="Chargement…"
      (retry)="retries = retries + 1"
    >
      <div empty data-testid="empty">Aucune donnée</div>
      <ng-template tchAsyncReady let-items>
        <ul data-testid="ready">
          @for (item of $any(items); track item) {
            <li>{{ item }}</li>
          }
        </ul>
      </ng-template>
    </tch-async-view>
  `,
})
class HostComponent {
  readonly status = signal('idle');
  readonly value = signal<string[] | undefined>(undefined);
  readonly error = signal<unknown>(undefined);
  readonly errorVm = signal<TchErrorViewModel | null>(null);
  retries = 0;

  readonly source = {
    status: () => this.status(),
    value: () => this.value(),
    error: () => this.error(),
  };
}

describe('TchAsyncViewComponent', () => {
  let fixture: ComponentFixture<HostComponent>;
  let host: HostComponent;

  beforeEach(async () => {
    vi.useFakeTimers();
    TestBed.configureTestingModule({
      imports: [HostComponent],
      providers: [{ provide: TranslateService, useValue: { instant: (k: string) => k } }],
    });
    fixture = TestBed.createComponent(HostComponent);
    host = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  function text(): string {
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  function query(selector: string): HTMLElement | null {
    return (fixture.nativeElement as HTMLElement).querySelector(selector);
  }

  function resolve(items: string[]): void {
    host.value.set(items);
    host.status.set('resolved');
    fixture.detectChanges();
  }

  it('ne rend rien en idle', () => {
    expect(text().trim()).toBe('');
  });

  it('anti-flash : pas de spinner avant ~300 ms de chargement initial', () => {
    host.status.set('loading');
    fixture.detectChanges();
    expect(query('.loading')).toBeNull();

    vi.advanceTimersByTime(300);
    fixture.detectChanges();
    expect(query('.loading')).toBeTruthy();
  });

  it('rend le template ready avec la valeur en contexte', () => {
    resolve(['midi', 'soir']);
    const ready = query('[data-testid="ready"]');
    expect(ready).toBeTruthy();
    expect(ready?.textContent).toContain('midi');
    expect(ready?.textContent).toContain('soir');
  });

  it('rend le slot empty pour une valeur vide', () => {
    resolve([]);
    expect(query('[data-testid="empty"]')).toBeTruthy();
    expect(query('[data-testid="ready"]')).toBeNull();
  });

  it('erreur initiale : error-panel avec retry et id de corrélation', () => {
    host.errorVm.set({
      title: 'Erreur',
      message: 'Chargement impossible',
      severity: 'error',
      traceId: 'trace-99',
    });
    host.error.set(new Error('boom'));
    host.status.set('error');
    fixture.detectChanges();

    expect(text()).toContain('Chargement impossible');
    expect(text()).toContain('trace-99');
    expect(query('[data-testid="ready"]')).toBeNull();
  });

  it('reloading : les données restent affichées, pas de blanchiment', () => {
    resolve(['midi']);

    host.status.set('reloading');
    fixture.detectChanges();

    expect(query('[data-testid="ready"]')).toBeTruthy();
    expect(text()).toContain('midi');

    // la barre discrète apparaît après le délai anti-flash
    vi.advanceTimersByTime(300);
    fixture.detectChanges();
    expect(query('.tch-async-view__bar')).toBeTruthy();
  });

  it('changement de params (loading avec stale) : mêmes garanties que reloading', () => {
    resolve(['midi']);

    host.value.set(undefined);
    host.status.set('loading');
    fixture.detectChanges();

    expect(query('[data-testid="ready"]')).toBeTruthy();
    expect(text()).toContain('midi');
    expect(query('.loading')).toBeNull();
  });

  it('reload depuis un état vide : le slot empty reste visible avec la barre discrète', () => {
    resolve([]);
    expect(query('[data-testid="empty"]')).toBeTruthy();

    host.status.set('reloading');
    fixture.detectChanges();
    vi.advanceTimersByTime(300);
    fixture.detectChanges();

    expect(query('[data-testid="empty"]')).toBeTruthy();
    expect(query('.tch-async-view__bar')).toBeTruthy();
  });

  it('erreur au reload : données précédentes affichées + erreur de section', () => {
    resolve(['midi']);

    host.errorVm.set({ title: 'Erreur', message: 'Reload KO', severity: 'error' });
    host.error.set(new Error('boom'));
    host.value.set(undefined);
    host.status.set('error');
    fixture.detectChanges();

    expect(query('[data-testid="ready"]')).toBeTruthy();
    expect(text()).toContain('midi');
    expect(text()).toContain('Reload KO');
  });
});
