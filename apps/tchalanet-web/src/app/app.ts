import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';

@Component({
  imports: [RouterModule],
  selector: 'tch-root',
  template: `<router-outlet></router-outlet>`,
  styles: ``,
})
export class App {
  protected title = 'tchalanet-web';
}
