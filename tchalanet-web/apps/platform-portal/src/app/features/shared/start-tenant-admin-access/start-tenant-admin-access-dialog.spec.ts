import { DOCUMENT } from '@angular/common';
import { PLATFORM_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { provideTranslateService } from '@ngx-translate/core';
import { PortalHandoffApi, SupportAccessStore } from '@tch/core/auth';
import { TchRuntimeConfigStore } from '@tch/shared-config';
import { of } from 'rxjs';

import { PlatformTenantAdminAccessApi } from '../../tenant-admins/data-access/platform-tenant-admin-access-api.service';
import { StartTenantAdminAccessDialog, StartTenantAdminAccessDialogData } from './start-tenant-admin-access-dialog';

describe(StartTenantAdminAccessDialog.name, () => {
  const session = {
    sessionId: 'support-session-1',
    tenantId: 'tenant-1',
    tenantCode: 'TCH',
    tenantName: 'Tchalanet',
    startedAt: '2026-07-12T10:00:00Z',
    expiresAt: '2026-07-12T11:00:00Z',
    actorRole: 'SUPER_ADMIN' as const,
    mode: 'SUPPORT_OVERRIDE' as const,
    sensitiveDataMasked: false,
  };

  const dialogRef = { close: vi.fn() };
  const accessApi = { startAdminAccess: vi.fn() };
  const store = { startSession: vi.fn() };
  const handoffs = { create: vi.fn() };
  const locationAssign = vi.fn();
  const documentMock = {
    defaultView: {
      location: {
        origin: 'https://tchalanet-web-stg.pages.dev',
        assign: locationAssign,
      },
    },
  };

  beforeEach(() => {
    dialogRef.close.mockReset();
    accessApi.startAdminAccess.mockReset();
    store.startSession.mockReset();
    handoffs.create.mockReset();
    locationAssign.mockReset();

    TestBed.configureTestingModule({
      providers: [
        provideTranslateService(),
        { provide: DOCUMENT, useValue: documentMock },
        { provide: PLATFORM_ID, useValue: 'browser' },
        { provide: MAT_DIALOG_DATA, useValue: data() },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: PlatformTenantAdminAccessApi, useValue: accessApi },
        { provide: SupportAccessStore, useValue: store },
        { provide: PortalHandoffApi, useValue: handoffs },
      ],
    });
  });

  it('opens the admin portal directly when platform and admin share the same origin', async () => {
    runtimeConfig('/admin');
    accessApi.startAdminAccess.mockReturnValue(of(session));
    const dialog = createDialog();

    submitValidForm(dialog);
    await settle();

    expect(accessApi.startAdminAccess).toHaveBeenCalledWith(
      'tenant-1',
      { reason: 'Support investigation', mode: 'SUPPORT_OVERRIDE' },
      { suppressShellFeedback: true },
    );
    expect(store.startSession).toHaveBeenCalledWith(session);
    expect(dialogRef.close).toHaveBeenCalledWith(session);
    expect(handoffs.create).not.toHaveBeenCalled();
    expect(locationAssign).toHaveBeenCalledWith('/admin/app/admin');
  });

  it('creates a support handoff when the admin portal is cross-origin', async () => {
    runtimeConfig('https://admin.stg.tchalanet.com');
    accessApi.startAdminAccess.mockReturnValue(of(session));
    handoffs.create.mockReturnValue(of({
      handoffId: 'handoff-1',
      code: 'secret',
      targetPortal: 'ADMIN',
      targetUrl: 'https://admin.stg.tchalanet.com',
      entryRoute: '/app/admin',
      expiresAt: '2026-07-12T10:05:00Z',
    }));
    const dialog = createDialog();

    submitValidForm(dialog);
    await settle();

    expect(handoffs.create).toHaveBeenCalledWith(
      {
        targetPortal: 'ADMIN',
        entryRoute: '/app/admin',
        supportAccessSessionId: 'support-session-1',
      },
      { suppressShellFeedback: true },
    );
    expect(locationAssign).toHaveBeenCalledWith(
      'https://admin.stg.tchalanet.com/login/handoff#code=handoff-1.secret',
    );
  });
});

function createDialog(): StartTenantAdminAccessDialog {
  return TestBed.runInInjectionContext(() => new StartTenantAdminAccessDialog());
}

function submitValidForm(dialog: StartTenantAdminAccessDialog): void {
  dialog.form.setValue({
    reason: 'Support investigation',
    confirmed: true,
  });
  dialog.submit();
}

function runtimeConfig(adminPortalUrl: string): void {
  TestBed.inject(TchRuntimeConfigStore).setConfig({
    appId: 'platform-portal',
    production: true,
    apiBaseUrl: 'https://api.stg.tchalanet.com/api/v1',
    authBaseUrl: 'https://api.stg.tchalanet.com/auth',
    assetsBaseUrl: '/assets',
    portalBaseUrls: {
      'admin-portal': adminPortalUrl,
      'platform-portal': '/platform',
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
}

function data(): StartTenantAdminAccessDialogData {
  return {
    tenantId: 'tenant-1',
    tenantName: 'Tchalanet',
    tenantCode: 'TCH',
    tenantStatus: 'ACTIVE',
  };
}

async function settle(): Promise<void> {
  await Promise.resolve();
  await Promise.resolve();
}
