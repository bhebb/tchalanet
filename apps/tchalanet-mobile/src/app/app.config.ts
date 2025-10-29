// app.config.ts (ou main.ts si tu utilises provide* standalone)
import { provideIonicAngular } from '@ionic/angular/standalone';

import { ApplicationConfig } from '@angular/core';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideRouter } from '@angular/router';

import { appRoutes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [provideRouter(appRoutes), provideIonicAngular(), provideAnimationsAsync()],
};
