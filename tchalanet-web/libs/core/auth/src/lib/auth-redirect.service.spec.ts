import { DOCUMENT } from '@angular/common';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { TchRuntimeConfigStore } from '@tch/shared-config';
import { of } from 'rxjs';

import { AuthRedirectService } from './auth-redirect.service';
import { PortalHandoffApi } from './handoff/portal-handoff-api.service';

describe('AuthRedirectService', () => {
  const router = {
    navigateByUrl: vi.fn(),
  };
  const handoffs = {
    create: vi.fn(),
  };
  const locationAssign = vi.fn();
  const documentMock = {
    defaultView: {
      location: {
        origin: 'http://localhost:4200',
        assign: locationAssign,
      },
    },
  };

  beforeEach(() => {
    router.navigateByUrl.mockReset();
    handoffs.create.mockReset();
    locationAssign.mockReset();
    TestBed.configureTestingModule({
      providers: [
        { provide: Router, useValue: router },
        { provide: PortalHandoffApi, useValue: handoffs },
        { provide: DOCUMENT, useValue: documentMock },
      ],
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

  it('uses a handoff when the public portal redirects to a cross-origin platform portal', async () => {
    const runtimeConfig = TestBed.inject(TchRuntimeConfigStore);
    runtimeConfig.setConfig({
      appId: 'public-portal',
      production: false,
      apiBaseUrl: '/api/v1',
      authBaseUrl: '/auth',
      assetsBaseUrl: '/assets',
      portalBaseUrls: {
        'admin-portal': 'http://localhost:4302',
        'platform-portal': 'http://localhost:4202',
      },
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
    handoffs.create.mockReturnValue(of({
      handoffId: 'handoff-1',
      code: 'secret',
      targetPortal: 'PLATFORM',
      targetUrl: '/platform',
      entryRoute: '/app/platform',
      expiresAt: '2026-07-04T14:50:00Z',
    }));

    await TestBed.inject(AuthRedirectService).navigateAfterLogin({
      authenticated: true,
      roles: ['SUPER_ADMIN'],
    });

    expect(handoffs.create).toHaveBeenCalledWith(
      {
        targetPortal: 'PLATFORM',
        entryRoute: '/app/platform',
      },
      { suppressShellFeedback: true },
    );
    expect(locationAssign).toHaveBeenCalledWith(
      'http://localhost:4202/login/handoff#code=handoff-1.secret',
    );
  });
});
