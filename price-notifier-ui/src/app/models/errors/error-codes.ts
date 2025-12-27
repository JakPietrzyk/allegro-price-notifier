export enum ErrorCode {
  AUTH_INVALID_CREDENTIALS = 'AUTH_INVALID_CREDENTIALS',
  AUTH_USER_ALREADY_EXISTS = 'AUTH_USER_ALREADY_EXISTS',
  PRODUCT_NOT_FOUND = 'PRODUCT_NOT_FOUND',
  PRODUCT_NOT_IN_STORE = 'PRODUCT_NOT_IN_STORE',
  VALIDATION_FAILED = 'VALIDATION_FAILED',
  INTERNAL_SERVER_ERROR = 'INTERNAL_SERVER_ERROR',
  USER_CREDENTIALS_INVALID = 'USER_CREDENTIALS_INVALID'
}

export const ERROR_MESSAGES: Record<ErrorCode, string> = {
  [ErrorCode.AUTH_INVALID_CREDENTIALS]: 'Podany e-mail lub hasło są nieprawidłowe.',
  [ErrorCode.AUTH_USER_ALREADY_EXISTS]: 'Użytkownik o takim adresie e-mail już istnieje.',
  [ErrorCode.PRODUCT_NOT_FOUND]: 'Nie znaleziono takiego produktu w naszej bazie.',
  [ErrorCode.PRODUCT_NOT_IN_STORE]: 'Produkt nie został odnaleziony w sklepie zewnętrznym (sprawdź link).',
  [ErrorCode.VALIDATION_FAILED]: 'Formularz zawiera błędy. Sprawdź poprawność danych.',
  [ErrorCode.INTERNAL_SERVER_ERROR]: 'Wystąpił błąd serwera. Spróbuj ponownie później.',
  [ErrorCode.USER_CREDENTIALS_INVALID]: 'Błąd autoryzacji, zaloguj się ponownie.'
};
