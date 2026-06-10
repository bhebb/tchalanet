import { HttpContextToken } from '@angular/common/http';

export const SUPPRESS_SHELL_FEEDBACK = new HttpContextToken<boolean>(() => false);
