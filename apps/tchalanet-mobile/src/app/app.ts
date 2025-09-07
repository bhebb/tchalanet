import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { IonApp, IonRouterOutlet } from '@ionic/angular/standalone';

@Component({
  standalone: true,
  imports: [
    RouterModule,
    IonApp,
    IonRouterOutlet
  ],
  selector: 'tch-root',
  template: `
    <ion-app>
      <ion-router-outlet></ion-router-outlet>
    </ion-app>
  `
})
export class App{}
