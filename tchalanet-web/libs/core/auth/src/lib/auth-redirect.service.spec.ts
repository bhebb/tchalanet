import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { TchRuntimeConfigStore } from '@tch/shared-config';

import { AuthRedirectService } from './auth-redirect.service';

describe('AuthRedirectService', () => {
  const router = {
    navigateByUrl: vi.fn(),
  };

  beforeEach(() => {
    router.navigateByUrl.mockReset();
    TestBed.configureTestingModule({
      providers: [{ provide: Router, useValue: router }],
    });
  });

  it('keeps platform account activation inside the platform portal', async () => {
    const runtimeConfig = TestBed.inject(TchRuntimeConfigStore);
    runtimeConfig.setConfig({
      appId: 'platform-portal',
      production: false,
      apiBaseUrl: '/api/v1',
      authBaseUrl: '/auth',
      assetsBaseUrl: '/assets',
      enableSandbox: false,
      firebaseAuthEmulatorUrl: null,
      firebase: {
        apiKey: '',
        authDomain: '',
        projectId: '',
        storageBucket: '',
        messagingSenderId: '',
        appId: '',
      },
    });

    await TestBed.inject(AuthRedirectService).navigateAfterLogin({
      authenticated: true,
      roles: ['SUPER_ADMIN'],
      entryRoute: '/app/account/activation',
    });

    expect(router.navigateByUrl).toHaveBeenCalledWith('/app/account/activation');
  });
});
