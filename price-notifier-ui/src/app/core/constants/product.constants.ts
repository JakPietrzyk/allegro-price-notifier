import { environment } from '../../../environments/environment';

const BASE_URL = `${environment.apiUrl}/products`;

export const PRODUCT_CONSTANTS = {
  API: {
    BASE: BASE_URL,
    SEARCH: `${BASE_URL}/search`,
    URL_ADD: `${BASE_URL}/url`,
    BY_ID: (id: string | number) => `${BASE_URL}/${id}`
  }
} as const;
