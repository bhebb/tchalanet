import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';
import { defineCustomElements } from '@ionic/pwa-elements/loader';
import { registerIcons } from './app/register-icons';

defineCustomElements(window);

registerIcons();

bootstrapApplication(App, appConfig).catch((err) => console.error(err));
