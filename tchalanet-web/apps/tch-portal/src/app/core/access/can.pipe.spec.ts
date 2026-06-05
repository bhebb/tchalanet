import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { AccessService } from './access.service';
import { CanPipe } from './can.pipe';

describe('CanPipe', () => {
  const allowed = signal(false);

  function setup() {
    TestBed.configureTestingModule({
      providers: [CanPipe, { provide: AccessService, useValue: { can: () => allowed() } }],
    });
    return TestBed.inject(CanPipe);
  }

  it('returns the access decision for the requirement', () => {
    allowed.set(true);
    expect(setup().transform({ feature: 'web.x' })).toBe(true);
  });

  it('returns false when access is denied', () => {
    allowed.set(false);
    expect(setup().transform({ feature: 'web.x', entitlement: 'payouts' })).toBe(false);
  });
});
