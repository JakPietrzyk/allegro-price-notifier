export const LOGIN_CONSTANTS = {
  UI: {
    TITLE: 'Zaloguj się',
    EMAIL_LABEL: 'Email',
    EMAIL_PLACEHOLDER: 'np. admin@test.pl',
    PASSWORD_LABEL: 'Hasło',
    SUBMIT_BTN: 'Wejdź',
    SUBMIT_BTN_LOADING: 'Logowanie...',
    NO_ACCOUNT: 'Nie masz konta?',
    REGISTER_LINK: 'Zarejestruj się'
  },
  MESSAGES: {
    ERRORS: {
      INVALID_CREDENTIALS: 'Nieprawidłowy email lub hasło',
      GENERIC: 'Wystąpił błąd logowania.'
    }
  }
} as const;
