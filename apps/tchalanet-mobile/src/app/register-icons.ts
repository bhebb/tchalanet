// register-icons.ts
import { addIcons } from 'ionicons';
import {
add, alertCircleOutline,
camera, chevronBack, chevronForward,   ellipse,
  gridOutline, heart,   home, homeOutline, homeSharp,
  logInOutline, logOutOutline,   menuOutline, notificationsOutline,
people, person, personOutline,
searchOutline,   settingsOutline, square,
time, triangle} from 'ionicons/icons';

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
