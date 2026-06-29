import { TestBed } from '@angular/core/testing';
import { Auth } from '@angular/fire/auth';

import { FirebaseAuthService } from './firebase-auth.service';

describe('FirebaseAuthService', () => {
  it('waits for Firebase to restore auth state before reporting authentication', async () => {
    const auth = {
      authStateReady: vi.fn().mockResolvedValue(undefined),
      currentUser: { uid: 'firebase-user-1' },
      setPersistence: vi.fn().mockResolvedValue(undefined),
    };
    TestBed.configureTestingModule({
      providers: [FirebaseAuthService, { provide: Auth, useValue: auth }],
    });

    await expect(TestBed.inject(FirebaseAuthService).isAuthenticated()).resolves.toBe(true);
    expect(auth.authStateReady).toHaveBeenCalledTimes(2);
    expect(auth.setPersistence).toHaveBeenCalledOnce();
  });

  it('waits for Firebase restored auth state before returning an access token', async () => {
    const getIdToken = vi.fn().mockResolvedValue('access-token');
    const auth = {
      authStateReady: vi.fn().mockResolvedValue(undefined),
      currentUser: { getIdToken },
      setPersistence: vi.fn().mockResolvedValue(undefined),
    };
    TestBed.configureTestingModule({
      providers: [FirebaseAuthService, { provide: Auth, useValue: auth }],
    });

    await expect(TestBed.inject(FirebaseAuthService).getAccessToken(true)).resolves.toBe(
      'access-token',
    );
    expect(auth.authStateReady).toHaveBeenCalledTimes(2);
    expect(auth.setPersistence).toHaveBeenCalledOnce();
    expect(getIdToken).toHaveBeenCalledWith(true);
  });
});
