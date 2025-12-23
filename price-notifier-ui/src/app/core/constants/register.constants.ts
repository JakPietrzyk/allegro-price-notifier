export const REGISTER_CONSTANTS = {
  UI: {
    TITLE: 'Załóż konto',
    FIRST_NAME_LABEL: 'Imię',
    LAST_NAME_LABEL: 'Nazwisko',
    EMAIL_LABEL: 'Email *',
    PASSWORD_LABEL: 'Hasło *',
    SUBMIT_BTN: 'Zarejestruj się',
    SUBMIT_BTN_LOADING: 'Rejestracja...',
    HAS_ACCOUNT: 'Masz już konto?',
    LOGIN_LINK: 'Zaloguj się'
  },
  MESSAGES: {
    ERRORS: {
      GENERIC: 'Rejestracja nieudana. Możliwe, że email jest już zajęty.',
      INVALID_FORM: 'Proszę poprawnie wypełnić wymagane pola.'
    }
  }
} as const;
