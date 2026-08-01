import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminRefreshButtonComponent } from './admin-refresh-button.component';

@Component({
  standalone: true,
  imports: [AdminRefreshButtonComponent],
  template: `
    <tch-admin-refresh-button
      label="Refresh"
      [disabled]="disabled"
      (refreshRequested)="onRefresh()"
    />
  `,
})
class HostComponent {
  disabled = false;
  refreshes = 0;

  onRefresh(): void {
    this.refreshes += 1;
  }
}

describe('AdminRefreshButtonComponent', () => {
  let fixture: ComponentFixture<HostComponent>;
  let host: HostComponent;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HostComponent] });
    fixture = TestBed.createComponent(HostComponent);
    host = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('emits refresh requests and can be disabled while loading', () => {
    const button = fixture.nativeElement.querySelector('button') as HTMLButtonElement;

    button.click();
    expect(host.refreshes).toBe(1);

    host.disabled = true;
    fixture.detectChanges();
    expect(button.disabled).toBe(true);
  });
});
