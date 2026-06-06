import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';

import { PublicBottomNav } from './public-bottom-nav';
import { PublicFooter } from './public-footer';

describe('public shell presentation', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideTranslateService()] });
  });

  it('creates the reusable footer and bottom navigation', () => {
    expect(TestBed.createComponent(PublicFooter).componentInstance).toBeTruthy();
    expect(TestBed.createComponent(PublicBottomNav).componentInstance).toBeTruthy();
  });
});
