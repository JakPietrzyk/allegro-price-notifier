import { environment } from '../../../environments/environment';

export const AUTH_CONSTANTS = {
  API: {
    LOGIN: `${environment.apiUrl}/auth/authenticate`,
    REGISTER: `${environment.apiUrl}/auth/register`
  },
  COOKIES: {
    TOKEN_NAME: 'jwt_token',
    EXPIRY_DAYS: 1,
    PATH: '/'
  }
} as const;
