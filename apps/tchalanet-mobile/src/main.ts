import { defineCustomElements } from '@ionic/pwa-elements/loader';

import { bootstrapApplication } from '@angular/platform-browser';

import { App } from './app/app';
import { appConfig } from './app/app.config';
import { registerIcons } from './app/register-icons';

defineCustomElements(window);

registerIcons();

bootstrapApplication(App, appConfig).catch((err) => console.error(err));
