import { Component, input } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';

import { ShellFeedbackOutletComponent } from '../../../shared/feedback/shell-feedback-outlet.component';
import { PrivateShellComponent } from './private-shell.component';
import { PrivateTopbarComponent } from './private-topbar.component';
import type { PrivateSpace } from './private-navigation.model';

@Component({ selector: 'tch-shell-feedback-outlet', standalone: true, template: '' })
class MockFeedbackOutlet {}

@Component({ selector: 'tch-private-topbar', standalone: true, template: '' })
class MockTopbar {
  readonly space = input.required<PrivateSpace>();
}

describe('PrivateShellComponent', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideTranslateService()],
    }).overrideComponent(PrivateShellComponent, {
      remove: { imports: [ShellFeedbackOutletComponent, PrivateTopbarComponent] },
      add: { imports: [MockFeedbackOutlet, MockTopbar] },
    });
  });

  it('renders the sidebar nav element', () => {
    const fixture = TestBed.createComponent(PrivateShellComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('tch-sidebar-nav')).toBeTruthy();
  });

  it('renders the topbar element', () => {
    const fixture = TestBed.createComponent(PrivateShellComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('tch-private-topbar')).toBeTruthy();
  });

  it('renders the router-outlet', () => {
    const fixture = TestBed.createComponent(PrivateShellComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('router-outlet')).toBeTruthy();
  });

  it('renders the shell feedback outlet', () => {
    const fixture = TestBed.createComponent(PrivateShellComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('tch-shell-feedback-outlet')).toBeTruthy();
  });
});
