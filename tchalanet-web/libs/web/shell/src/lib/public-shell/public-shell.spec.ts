import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';

import { PublicFooter } from './public-footer';
import { PublicShellLayoutComponent } from './public-shell-layout.component';

describe('public shell presentation', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideTranslateService()] });
  });

  it('creates the reusable footer and shell layout', () => {
    expect(TestBed.createComponent(PublicFooter).componentInstance).toBeTruthy();
    expect(TestBed.createComponent(PublicShellLayoutComponent).componentInstance).toBeTruthy();
  });
});
