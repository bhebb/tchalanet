import { Component, signal } from '@angular/core';
import { IonContent, IonHeader, IonItem, IonList, IonTitle, IonToolbar } from '@ionic/angular/standalone';

@Component({
  standalone: true,
  selector: 'tch-tickets-page',
  imports: [
    IonHeader,
    IonToolbar,
    IonTitle,
    IonContent,
    IonList,
    IonItem
  ],
  template: `
    <ion-header>
      <ion-toolbar>
        <ion-title>Tickets</ion-title>
      </ion-toolbar>
    </ion-header>
    <ion-content>
      <ion-list>
        @for (t of tickets(); track t.id) {
          <ion-item>{{ t.title }}</ion-item>
        }
      </ion-list>
    </ion-content>
  `
})
export class TicketsPage {
  tickets = signal([{ id: 1, title: 'Welcome ticket' }, { id: 2, title: 'Welcome ticket2' }]);
}
