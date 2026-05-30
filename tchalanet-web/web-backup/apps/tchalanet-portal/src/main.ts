import { bootstrapApplication } from '@angular/platform-browser';

import 'zone.js';

import { App } from './app/app';
import { appConfig } from './app/app.config';

import './styles.scss';

console.log('🔄 main.ts loading... Angular bootstrap starting');

bootstrapApplication(App, appConfig).catch((err) =>
  console.error('Bootstrap Error:', err),
);
