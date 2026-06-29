import { EnvironmentProviders, makeEnvironmentProviders } from '@angular/core';
import { initializeApp, provideFirebaseApp } from '@angular/fire/app';
import { connectAuthEmulator, getAuth, provideAuth } from '@angular/fire/auth';
import { FirebaseOptions } from 'firebase/app';

import { AUTH_CLIENT } from '../auth-client';
import { FirebaseAuthService } from './firebase-auth.service';

export interface FirebaseAuthClientConfig {
  readonly options: FirebaseOptions;
  readonly emulatorUrl: string | null;
}

export function provideFirebaseAuthClient(config: FirebaseAuthClientConfig): EnvironmentProviders {
  return makeEnvironmentProviders([
    provideFirebaseApp(() => initializeApp(config.options)),
    provideAuth(() => {
      const auth = getAuth();
      if (config.emulatorUrl) {
        connectAuthEmulator(auth, config.emulatorUrl, {
          disableWarnings: true,
        });
      }
      return auth;
    }),
    FirebaseAuthService,
    { provide: AUTH_CLIENT, useExisting: FirebaseAuthService },
  ]);
}
