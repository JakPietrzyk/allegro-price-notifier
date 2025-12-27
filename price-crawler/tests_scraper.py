import unittest
from unittest.mock import patch, MagicMock
from bs4 import BeautifulSoup

# Importujemy aplikację. Zakładam, że Twój plik nazywa się main.py
from main import app, get_soup, find_best_product_link, extract_cheapest_offer
# Importujemy Enum, żeby porównywać wartości (opcjonalnie, można sprawdzać stringi)
from scraper_error_code import ScraperErrorCode


class TestScraperUtils(unittest.TestCase):
    def setUp(self):
        self.html_search_results = """
        <html>
            <body>
                <div class="cat-prod-row">
                    <strong class="cat-prod-row__name">
                        <a href="/product-123.htm" class="js_seoUrl">Test Product</a>
                    </strong>
                </div>
            </body>
        </html>
        """

        self.html_product_page = """
        <html>
            <body>
                <h1 class="product-top__product-info__name">Mock Product</h1>
                <div class="product-offer__container">
                    <span class="price">
                        <span class="value">2000</span>
                    </span>
                </div>
                <div class="product-offer__container">
                    <span class="price">
                        <span class="value">1500</span>
                        <span class="penny">,50</span>
                    </span>
                </div>
            </body>
        </html>
        """

    @patch('main.requests.get')
    def test_get_soup_returns_soup_on_200(self, mock_get):
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.content = b"<html></html>"
        mock_get.return_value = mock_response

        result = get_soup("http://example.com")
        self.assertIsInstance(result, BeautifulSoup)

    @patch('main.requests.get')
    def test_get_soup_returns_none_on_404(self, mock_get):
        mock_response = MagicMock()
        mock_response.status_code = 404
        mock_get.return_value = mock_response

        result = get_soup("http://example.com")
        self.assertIsNone(result)

    @patch('main.requests.get')
    def test_get_soup_returns_none_on_exception(self, mock_get):
        mock_get.side_effect = Exception("Connection error")
        result = get_soup("http://example.com")
        self.assertIsNone(result)

    @patch('main.get_soup')
    def test_find_best_product_link_returns_correct_url(self, mock_get_soup):
        mock_get_soup.return_value = BeautifulSoup(self.html_search_results, 'html.parser')

        result = find_best_product_link("Test Product")
        self.assertEqual(result, "https://www.ceneo.pl/product-123.htm")

    @patch('main.get_soup')
    def test_find_best_product_link_returns_search_url_on_direct_match(self, mock_get_soup):
        direct_html = '<html><body><table class="product-offers"></table></body></html>'
        mock_get_soup.return_value = BeautifulSoup(direct_html, 'html.parser')

        result = find_best_product_link("Direct Match Item")
        self.assertIn("https://www.ceneo.pl/;szukaj-", result)

    @patch('main.get_soup')
    def test_find_best_product_link_returns_none_if_not_found(self, mock_get_soup):
        mock_get_soup.return_value = BeautifulSoup("<html></html>", 'html.parser')
        result = find_best_product_link("Unknown")
        self.assertIsNone(result)

    @patch('main.get_soup')
    def test_extract_cheapest_offer_returns_min_price(self, mock_get_soup):
        mock_get_soup.return_value = BeautifulSoup(self.html_product_page, 'html.parser')

        title, price = extract_cheapest_offer("http://example.com/p1")
        self.assertEqual(title, "Mock Product")
        self.assertEqual(price, 1500.50)

    @patch('main.get_soup')
    def test_extract_cheapest_offer_returns_zero_if_no_offers(self, mock_get_soup):
        mock_get_soup.return_value = BeautifulSoup("<html><h1>Title</h1></html>", 'html.parser')

        title, price = extract_cheapest_offer("http://example.com/p1")
        self.assertEqual(price, 0.0)


class TestFlaskEndpoints(unittest.TestCase):
    def setUp(self):
        self.app = app.test_client()
        self.app.testing = True

    @patch('main.extract_cheapest_offer')
    @patch('main.find_best_product_link')
    def test_find_price_success(self, mock_find, mock_extract):
        mock_find.return_value = "https://ceneo.pl/1"
        mock_extract.return_value = ("Test Item", 100.0)

        response = self.app.post('/find_price', json={"productName": "Item"})
        data = response.get_json()

        self.assertEqual(response.status_code, 200)
        self.assertEqual(data['price'], 100.0)
        self.assertEqual(data['found_product_name'], "Test Item")

    def test_find_price_missing_payload(self):
        response = self.app.post('/find_price', json={})
        data = response.get_json()

        self.assertEqual(response.status_code, 400)
        self.assertEqual(data['errorCode'], ScraperErrorCode.MISSING_PARAM.value)
        self.assertIn("Missing parameter", data['message'])

    @patch('main.find_best_product_link')
    def test_find_price_product_not_found(self, mock_find):
        mock_find.return_value = None

        response = self.app.post('/find_price', json={"productName": "Ghost Item"})
        data = response.get_json()

        self.assertEqual(response.status_code, 404)
        self.assertEqual(data['errorCode'], ScraperErrorCode.PRODUCT_NOT_FOUND.value)

    @patch('main.extract_cheapest_offer')
    def test_scrape_direct_url_success(self, mock_extract):
        mock_extract.return_value = ("Direct Item", 50.0)
        payload = {"url": "https://www.ceneo.pl/555"}

        response = self.app.post('/scrape_direct_url', json=payload)
        data = response.get_json()

        self.assertEqual(response.status_code, 200)
        self.assertEqual(data['price'], 50.0)

    def test_scrape_direct_url_invalid_domain(self):
        payload = {"url": "https://www.google.com"}

        response = self.app.post('/scrape_direct_url', json=payload)
        data = response.get_json()

        self.assertEqual(response.status_code, 400)
        self.assertEqual(data['errorCode'], ScraperErrorCode.INVALID_DOMAIN.value)

    @patch('main.extract_cheapest_offer')
    def test_scrape_direct_url_price_parsing_error(self, mock_extract):
        mock_extract.return_value = ("Item", 0.0)
        payload = {"url": "https://www.ceneo.pl/555"}

        response = self.app.post('/scrape_direct_url', json=payload)
        data = response.get_json()

        self.assertEqual(response.status_code, 422)  # Zmienione z 404 na 422
        self.assertEqual(data['errorCode'], ScraperErrorCode.PRICE_PARSING_ERROR.value)
        self.assertEqual(data['message'], "Invalid price")

    def test_scrape_direct_url_missing_param(self):
        response = self.app.post('/scrape_direct_url', json={})
        data = response.get_json()

        self.assertEqual(response.status_code, 400)
        self.assertEqual(data['errorCode'], ScraperErrorCode.MISSING_PARAM.value)


if __name__ == '__main__':
    unittest.main()