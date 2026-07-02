import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';

import { TchPaginationComponent } from './tch-pagination.component';

@Component({
  standalone: true,
  imports: [TchPaginationComponent],
  template: `
    <tch-pagination
      [page]="page()"
      [size]="size()"
      [totalElements]="total()"
      (pageChange)="lastPage = $event"
      (sizeChange)="lastSize = $event"
    />
  `,
})
class HostComponent {
  readonly page = signal(0);
  readonly size = signal(20);
  readonly total = signal(41);
  lastPage: number | null = null;
  lastSize: number | null = null;
}

describe('TchPaginationComponent', () => {
  let fixture: ComponentFixture<HostComponent>;
  let host: HostComponent;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HostComponent] });
    fixture = TestBed.createComponent(HostComponent);
    host = fixture.componentInstance;
    fixture.detectChanges();
  });

  function el(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function buttons(): HTMLButtonElement[] {
    return Array.from(el().querySelectorAll('button'));
  }

  it('affiche la plage « N–M sur Total »', () => {
    expect(el().textContent).toContain('1–20 sur 41');
  });

  it('adapte la plage à la page courante', () => {
    host.page.set(2);
    fixture.detectChanges();
    expect(el().textContent).toContain('41–41 sur 41');
  });

  it('désactive précédent en première page et suivant en dernière', () => {
    const [prev, next] = buttons();
    expect(prev.disabled).toBe(true);
    expect(next.disabled).toBe(false);

    host.page.set(2);
    fixture.detectChanges();
    expect(prev.disabled).toBe(false);
    expect(next.disabled).toBe(true);
  });

  it('émet pageChange sur précédent/suivant', () => {
    const [prev, next] = buttons();
    next.click();
    expect(host.lastPage).toBe(1);

    host.page.set(1);
    fixture.detectChanges();
    prev.click();
    expect(host.lastPage).toBe(0);
  });

  it('émet sizeChange depuis le sélecteur de taille', () => {
    const select = el().querySelector('select') as HTMLSelectElement;
    select.value = '50';
    select.dispatchEvent(new Event('change'));
    expect(host.lastSize).toBe(50);
  });

  it('affiche 0 sur 0 sans données', () => {
    host.total.set(0);
    fixture.detectChanges();
    expect(el().textContent).toContain('0 sur 0');
  });
});
