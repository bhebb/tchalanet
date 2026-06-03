import { Component, inject } from '@angular/core';
import { RouterModule } from '@angular/router';

import { AppRuntimeStore } from './core/runtime';

@Component({
  imports: [RouterModule],
  selector: 'tch-root',
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  private readonly runtime = inject(AppRuntimeStore);

  protected title = 'tch-portal';

  constructor() {
    this.runtime.initPublicRuntime();
  }
}
