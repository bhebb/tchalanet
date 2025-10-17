// app.config.ts (ou main.ts si tu utilises provide* standalone)
import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { appRoutes } from './app.routes';
import { provideIonicAngular } from '@ionic/angular/standalone';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';

export const appConfig: ApplicationConfig = {
  providers: [provideRouter(appRoutes), provideIonicAngular(), provideAnimationsAsync()],
};
