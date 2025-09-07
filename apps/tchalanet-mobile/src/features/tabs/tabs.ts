import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonTabs, IonTabBar, IonTabButton, IonIcon, IonLabel } from '@ionic/angular/standalone';
import { RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'tch-tabs',
  standalone: true,
  imports: [
    CommonModule,
    IonTabs,
    IonTabBar,
    IonTabButton,
    IonIcon,
    IonLabel
  ],
  template: `
    <ion-tabs>
      <ion-tab-bar slot="bottom">
        <ion-tab-button tab="tab1" href="/tabs/tab1">
          <ion-icon name="triangle"></ion-icon>
          <ion-label>Tab 1</ion-label>
        </ion-tab-button>
        <ion-tab-button tab="tab2" href="/tabs/tab2">
          <ion-icon name="ellipse"></ion-icon>
          <ion-label>Tab 2</ion-label>
        </ion-tab-button>
        <ion-tab-button tab="tab3" href="/tabs/tab3">
          <ion-icon name="square"></ion-icon>
          <ion-label>Tab 3</ion-label>
        </ion-tab-button>
      </ion-tab-bar>
    </ion-tabs>
  `
})
export class Tabs {

}
