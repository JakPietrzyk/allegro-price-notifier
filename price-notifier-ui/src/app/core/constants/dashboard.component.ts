export const DASHBOARD_CONSTANTS = {
  UI: {
    header: {
      title: 'Price Notifier 🛒',
      logoutBtn: 'Wyloguj'
    },
    addByName: {
      title: 'Dodaj po nazwie',
      placeholder: 'np. PlayStation 5',
      button: 'Szukaj'
    },
    addByUrl: {
      title: 'Dodaj link Ceneo',
      placeholder: 'Link do Ceneo',
      button: 'Link'
    },
    status: {
      loading: '🔄 Pobieranie aktualnych cen...',
      empty: 'Brak produktów. Dodaj coś powyżej!',
      lastChecked: 'Ost. sprawdzenie:'
    },
    product: {
      deleteTitle: 'Usuń produkt',
      offerLink: 'Oferta Ceneo ↗'
    }
  },
  MESSAGES: {
    CONFIRM_DELETE: 'Czy na pewno chcesz usunąć ten produkt i jego historię?',
    ADD_SUCCESS: 'Produkt został dodany pomyślnie.',
    ERRORS: {
      ADD_FAILED: 'Błąd podczas dodawania produktu.',
      DELETE_FAILED: 'Nie udało się usunąć produktu.',
      LOAD_FAILED: 'Nie udało się pobrać listy produktów.',
      GENERIC: 'Wystąpił nieoczekiwany błąd.'
    }
  }
} as const;
