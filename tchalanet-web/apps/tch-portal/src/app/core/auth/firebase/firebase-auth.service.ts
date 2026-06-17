import { inject, Injectable } from '@angular/core';
import {
  Auth,
  browserLocalPersistence,
  browserSessionPersistence,
  setPersistence,
  signInWithEmailAndPassword,
  signOut,
  user,
} from '@angular/fire/auth';
import { firstValueFrom } from 'rxjs';

import { AuthClient, AuthLoginRequest } from '../auth-client';

@Injectable({ providedIn: 'root' })
export class FirebaseAuthService implements AuthClient {
  private readonly auth = inject(Auth);

  readonly user$ = user(this.auth);

  async isAuthenticated(): Promise<boolean> {
    return (await firstValueFrom(this.user$)) !== null;
  }

  async login(request: AuthLoginRequest): Promise<void> {
    await setPersistence(
      this.auth,
      request.remember ? browserLocalPersistence : browserSessionPersistence,
    );
    await signInWithEmailAndPassword(this.auth, request.username, request.password);
  }

  async logout(): Promise<void> {
    await signOut(this.auth);
  }

  async getAccessToken(forceRefresh = false): Promise<string | null> {
    const currentUser = await firstValueFrom(this.user$);
    return currentUser ? currentUser.getIdToken(forceRefresh) : null;
  }

  async getTokenExpiresAt(): Promise<string | undefined> {
    const currentUser = await firstValueFrom(this.user$);
    if (!currentUser) {
      return undefined;
    }

    return (await currentUser.getIdTokenResult()).expirationTime;
  }
}
