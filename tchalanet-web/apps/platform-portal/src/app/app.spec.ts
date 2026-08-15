import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideState, provideStore } from '@ngrx/store';
import { provideTranslateService } from '@ngx-translate/core';
import { AUTH_CLIENT, AuthClient } from '@tch/core/auth';
import { i18nFeature } from '@tch/core/i18n';
import { themeStoreProvider } from '@tch/ui/theme';
import { App } from './app';

const authClient: AuthClient = {
  isAuthenticated: async () => false,
  login: async () => undefined,
  logout: async () => undefined,
  getAccessToken: async () => null,
  getTokenExpiresAt: async () => undefined,
};

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]),
        provideTranslateService(),
        provideStore({}),
        provideState(i18nFeature),
        themeStoreProvider,
        { provide: AUTH_CLIENT, useValue: authClient },
      ],
    }).compileComponents();
  });

  it('should render the routed app host', async () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('router-outlet')).not.toBeNull();
  });
});
