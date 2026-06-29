import { Component, signal, ChangeDetectionStrategy } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { FeatureFlags } from './feature-flags';
import { FeatureFlagDirective } from './feature-flag.directive';

@Component({
  imports: [FeatureFlagDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <p *tchFeature="'web.demo'; else off">on</p>
    <ng-template #off><p>off</p></ng-template>
  `,
})
class HostComponent {}

describe('FeatureFlagDirective', () => {
  const enabledFlags = signal<Record<string, boolean>>({});
  const features = {
    isEnabled: (key: string, fallback = false) => enabledFlags()[key] ?? fallback,
  };

  function setup() {
    TestBed.configureTestingModule({
      imports: [HostComponent],
      providers: [{ provide: FeatureFlags, useValue: features }],
    });
    return TestBed.createComponent(HostComponent);
  }

  it('renders the gated content when the flag is enabled', () => {
    enabledFlags.set({ 'web.demo': true });
    const fixture = setup();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('on');
    expect(fixture.nativeElement.textContent).not.toContain('off');
  });

  it('renders the else template when the flag is disabled', () => {
    enabledFlags.set({ 'web.demo': false });
    const fixture = setup();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('off');
    expect(fixture.nativeElement.textContent).not.toContain('on');
  });

  it('reacts when the flag flips after settings resolve', () => {
    enabledFlags.set({ 'web.demo': false });
    const fixture = setup();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('off');

    enabledFlags.set({ 'web.demo': true });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('on');
  });
});
