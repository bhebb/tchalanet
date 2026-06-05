import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { AccessService } from './access.service';
import { CanDirective } from './can.directive';

@Component({
  imports: [CanDirective],
  template: `
    <p *tchCan="{ feature: 'web.x', entitlement: 'payouts' }; else off">on</p>
    <ng-template #off><p>off</p></ng-template>
  `,
})
class HostComponent {}

describe('CanDirective', () => {
  const allowed = signal(false);
  const access = { can: () => allowed() };

  function setup() {
    TestBed.configureTestingModule({
      imports: [HostComponent],
      providers: [{ provide: AccessService, useValue: access }],
    });
    return TestBed.createComponent(HostComponent);
  }

  it('renders content when access is granted', () => {
    allowed.set(true);
    const fixture = setup();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('on');
    expect(fixture.nativeElement.textContent).not.toContain('off');
  });

  it('renders the else template when access is denied', () => {
    allowed.set(false);
    const fixture = setup();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('off');
    expect(fixture.nativeElement.textContent).not.toContain('on');
  });

  it('reacts when access changes after settings resolve', () => {
    allowed.set(false);
    const fixture = setup();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('off');

    allowed.set(true);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('on');
  });
});
