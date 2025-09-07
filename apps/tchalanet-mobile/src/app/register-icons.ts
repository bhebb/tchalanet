// register-icons.ts
import { addIcons } from 'ionicons';
import {
  home, homeOutline, homeSharp,
  settingsOutline, personOutline,
  gridOutline, notificationsOutline,
  menuOutline, searchOutline, alertCircleOutline,
  logInOutline, logOutOutline, chevronBack, chevronForward, people, time, heart, add, camera, person, triangle, square,
  ellipse
} from 'ionicons/icons';

export function registerIcons() {
  addIcons({
    home, homeOutline, homeSharp,
    settingsOutline, personOutline,
    gridOutline, notificationsOutline,
    menuOutline, searchOutline, alertCircleOutline,
    logInOutline, logOutOutline, chevronBack, chevronForward,
    triangle, square, ellipse, camera, add, heart, time, people, person
  });
}
